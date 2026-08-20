-- Stories: captions, reactions, mutes, publish/view/delete RPCs, richer inbox.

alter table public.stories
  add column if not exists caption text;

alter table public.stories
  add column if not exists kind text not null default 'text';

alter table public.stories
  drop constraint if exists stories_kind_check;
alter table public.stories
  add constraint stories_kind_check check (kind in ('text', 'photo', 'video', 'voice'));

create table if not exists public.story_reactions (
  story_id uuid not null references public.stories(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  emoji text not null check (char_length(emoji) between 1 and 16),
  created_at timestamptz not null default now(),
  primary key (story_id, user_id)
);

create table if not exists public.story_mutes (
  viewer_id uuid not null references public.profiles(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (viewer_id, author_id),
  check (viewer_id <> author_id)
);

alter table public.story_reactions enable row level security;
alter table public.story_mutes enable row level security;

drop policy if exists "viewers insert own story views" on public.story_views;
create policy "viewers insert own story views" on public.story_views for insert to authenticated
  with check (viewer_id = auth.uid());
drop policy if exists "viewers read story views" on public.story_views;
create policy "viewers read story views" on public.story_views for select to authenticated
  using (
    viewer_id = auth.uid()
    or exists (select 1 from public.stories s where s.id = story_id and s.author_id = auth.uid())
  );

drop policy if exists "members write story reactions" on public.story_reactions;
create policy "members write story reactions" on public.story_reactions for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "authors read story reactions" on public.story_reactions;
create policy "authors read story reactions" on public.story_reactions for select to authenticated
  using (
    user_id = auth.uid()
    or exists (select 1 from public.stories s where s.id = story_id and s.author_id = auth.uid())
  );

drop policy if exists "user manages story mutes" on public.story_mutes;
create policy "user manages story mutes" on public.story_mutes for all to authenticated
  using (viewer_id = auth.uid()) with check (viewer_id = auth.uid());

create or replace function public.publish_story(
  p_caption text,
  p_kind text default 'text',
  p_audience text default 'friends'
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  created uuid;
  clean text;
  allowed text;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  perform public.ensure_own_profile();
  clean := left(btrim(coalesce(p_caption, '')), 500);
  if char_length(clean) < 1 then
    raise exception 'empty_story';
  end if;
  if coalesce(p_kind, 'text') not in ('text', 'photo', 'video', 'voice') then
    raise exception 'invalid_kind';
  end if;
  allowed := case when coalesce(p_audience, 'friends') = 'private' then 'private' else 'friends' end;
  insert into public.stories (author_id, storage_path, encrypted_key, audience, caption, kind, expires_at)
  values (
    auth.uid(),
    'text:' || substr(md5(clean || auth.uid()::text), 1, 16),
    '\x'::bytea,
    allowed,
    clean,
    coalesce(p_kind, 'text'),
    now() + interval '24 hours'
  )
  returning id into created;
  return created;
end;
$$;

create or replace function public.mark_story_viewed(target uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if not exists (select 1 from public.stories where id = target and deleted_at is null and expires_at > now()) then
    raise exception 'unknown_story';
  end if;
  insert into public.story_views (story_id, viewer_id)
  values (target, auth.uid())
  on conflict do nothing;
end;
$$;

create or replace function public.react_to_story(target uuid, emoji text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if not exists (select 1 from public.stories where id = target and deleted_at is null) then
    raise exception 'unknown_story';
  end if;
  insert into public.story_reactions (story_id, user_id, emoji)
  values (target, auth.uid(), left(coalesce(emoji, '❤️'), 16))
  on conflict (story_id, user_id) do update set emoji = excluded.emoji, created_at = now();
end;
$$;

create or replace function public.delete_own_story(target uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.stories
     set deleted_at = now()
   where id = target and author_id = auth.uid() and deleted_at is null;
  if not found then
    raise exception 'cannot_delete';
  end if;
end;
$$;

create or replace function public.mute_story_author(target uuid, muted boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null or target = auth.uid() then
    raise exception 'invalid_peer';
  end if;
  if muted then
    insert into public.story_mutes (viewer_id, author_id) values (auth.uid(), target)
    on conflict do nothing;
  else
    delete from public.story_mutes where viewer_id = auth.uid() and author_id = target;
  end if;
end;
$$;

drop function if exists public.inbox_stories();
create function public.inbox_stories()
returns table (
  id uuid,
  author_id uuid,
  author_name text,
  author_username text,
  author_verified boolean,
  caption text,
  kind text,
  audience text,
  expires_at timestamptz,
  created_at timestamptz,
  viewed boolean,
  mine boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select s.id, s.author_id, p.display_name, p.username, p.is_verified,
         coalesce(s.caption, ''), s.kind, s.audience, s.expires_at, s.created_at,
         exists(select 1 from public.story_views v where v.story_id = s.id and v.viewer_id = auth.uid()),
         s.author_id = auth.uid()
  from public.stories s
  join public.profiles p on p.id = s.author_id
  where s.deleted_at is null and s.expires_at > now()
    and not exists (
      select 1 from public.story_mutes m
      where m.viewer_id = auth.uid() and m.author_id = s.author_id
    )
    and (
      s.author_id = auth.uid()
      or (s.audience = 'friends' and exists (
        select 1 from public.user_follows a
        join public.user_follows b on a.follower_id = b.followee_id and a.followee_id = b.follower_id
        where a.follower_id = auth.uid() and a.followee_id = s.author_id
      ))
    )
  order by (s.author_id = auth.uid()) desc,
           (exists(select 1 from public.story_views v where v.story_id = s.id and v.viewer_id = auth.uid())) asc,
           s.created_at desc
  limit 40;
$$;

grant execute on function public.publish_story(text, text, text) to authenticated;
grant execute on function public.mark_story_viewed(uuid) to authenticated;
grant execute on function public.react_to_story(uuid, text) to authenticated;
grant execute on function public.delete_own_story(uuid) to authenticated;
grant execute on function public.mute_story_author(uuid, boolean) to authenticated;
grant execute on function public.inbox_stories() to authenticated;
