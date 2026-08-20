-- Stories may only be friends (mutual follows) or only-me (private).
-- Followers and public audiences are removed.

create or replace function public.is_mutual_friend(other uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select other is not null
     and other <> auth.uid()
     and exists (
       select 1 from public.user_follows a
       join public.user_follows b
         on a.follower_id = b.followee_id and a.followee_id = b.follower_id
       where a.follower_id = auth.uid() and a.followee_id = other
     );
$$;

update public.stories
   set audience = 'friends'
 where audience is distinct from 'friends'
   and audience is distinct from 'private';

do $$
declare
  cname text;
begin
  for cname in
    select con.conname
    from pg_constraint con
    join pg_class rel on rel.oid = con.conrelid
    join pg_namespace nsp on nsp.oid = rel.relnamespace
    where nsp.nspname = 'public'
      and rel.relname = 'stories'
      and con.contype = 'c'
      and pg_get_constraintdef(con.oid) ilike '%audience%'
  loop
    execute format('alter table public.stories drop constraint if exists %I', cname);
  end loop;
end $$;

alter table public.stories alter column audience set default 'friends';
alter table public.stories
  add constraint stories_audience_friends_or_private
  check (audience in ('friends', 'private'));

drop policy if exists "followers read stories" on public.stories;
drop policy if exists "friends read stories" on public.stories;
create policy "friends read stories" on public.stories for select to authenticated
  using (
    deleted_at is null
    and expires_at > now()
    and (
      author_id = auth.uid()
      or (audience = 'friends' and public.is_mutual_friend(author_id))
    )
  );

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
      or (s.audience = 'friends' and public.is_mutual_friend(s.author_id))
    )
  order by (s.author_id = auth.uid()) desc,
           (exists(select 1 from public.story_views v where v.story_id = s.id and v.viewer_id = auth.uid())) asc,
           s.created_at desc
  limit 40;
$$;

grant execute on function public.is_mutual_friend(uuid) to authenticated;
grant execute on function public.publish_story(text, text, text) to authenticated;
grant execute on function public.inbox_stories() to authenticated;
