-- Feed page RPC, follow, comments, and server-side Ripple momentum.
-- Clients never write feed_items scores or momentum bands.

drop policy if exists "approved public moments readable" on public.moments;
create policy "approved moments readable" on public.moments for select to authenticated
  using (
    deleted_at is null
    and moderation_state = 'approved'
    and (
      visibility = 'public'
      or creator_id = auth.uid()
      or (
        visibility = 'followers'
        and exists (
          select 1 from public.user_follows f
          where f.follower_id = auth.uid() and f.followee_id = creator_id
        )
      )
    )
  );

create table if not exists public.moment_momentum (
  moment_id uuid primary key references public.moments(id) on delete cascade,
  ripple_count integer not null default 0,
  ripple_hour integer not null default 0,
  like_count integer not null default 0,
  save_count integer not null default 0,
  comment_count integer not null default 0,
  band text not null default 'quiet' check (band in ('quiet', 'active', 'heating', 'hot', 'viral')),
  updated_at timestamptz not null default now()
);

alter table public.moment_momentum enable row level security;
drop policy if exists "momentum readable" on public.moment_momentum;
create policy "momentum readable" on public.moment_momentum for select to authenticated using (true);

create or replace function public.ripple_trust_weight()
returns numeric
language sql
stable
security definer
set search_path = public
as $$
  select case
    when exists(select 1 from public.profiles p where p.id = auth.uid() and p.is_verified) then 1.0
    when exists(select 1 from public.profiles p where p.id = auth.uid() and p.created_at < now() - interval '7 days') then 0.8
    else 0.4
  end;
$$;

create or replace function public.refresh_moment_momentum(target uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  ripples integer;
  hour_count integer;
  likes integer;
  saves integer;
  comments integer;
  next_band text;
begin
  select count(*) filter (where reaction = 'ripple'),
         count(*) filter (where reaction = 'like'),
         count(*) filter (where reaction = 'save')
    into ripples, likes, saves
    from public.moment_reactions where moment_id = target;
  select count(*) into hour_count
    from public.moment_reactions
   where moment_id = target and reaction = 'ripple' and created_at > now() - interval '1 hour';
  select count(*) into comments
    from public.moment_comments where moment_id = target and deleted_at is null;
  next_band := case
    when hour_count >= 50 or ripples >= 500 then 'viral'
    when hour_count >= 15 or ripples >= 100 then 'hot'
    when hour_count >= 5 or ripples >= 25 then 'heating'
    when ripples >= 3 then 'active'
    else 'quiet'
  end;
  insert into public.moment_momentum (moment_id, ripple_count, ripple_hour, like_count, save_count, comment_count, band, updated_at)
  values (target, ripples, hour_count, likes, saves, comments, next_band, now())
  on conflict (moment_id) do update
    set ripple_count = excluded.ripple_count,
        ripple_hour = excluded.ripple_hour,
        like_count = excluded.like_count,
        save_count = excluded.save_count,
        comment_count = excluded.comment_count,
        band = excluded.band,
        updated_at = now();
end;
$$;

create or replace function public.toggle_moment_reaction(target uuid, p_reaction text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  removed boolean := false;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if p_reaction not in ('like', 'ripple', 'save') then
    raise exception 'invalid_reaction';
  end if;
  if not exists (select 1 from public.moments where id = target and deleted_at is null) then
    raise exception 'unknown_moment';
  end if;
  if (select count(*) from public.moment_reactions
      where user_id = auth.uid() and created_at > now() - interval '1 minute') >= 40 then
    raise exception 'rate_limited';
  end if;
  if exists (
    select 1 from public.moment_reactions
    where moment_id = target and user_id = auth.uid() and reaction = p_reaction
  ) then
    delete from public.moment_reactions
     where moment_id = target and user_id = auth.uid() and reaction = p_reaction;
    removed := true;
  else
    insert into public.moment_reactions (moment_id, user_id, reaction)
    values (target, auth.uid(), p_reaction);
    insert into public.feed_events (actor_id, moment_id, event_type, metadata)
    values (auth.uid(), target, p_reaction, jsonb_build_object('trust', public.ripple_trust_weight()));
  end if;
  perform public.refresh_moment_momentum(target);
  return jsonb_build_object(
    'removed', removed,
    'trust', public.ripple_trust_weight(),
    'momentum', (select to_jsonb(m) from public.moment_momentum m where m.moment_id = target)
  );
end;
$$;

create or replace function public.follow_profile(other_user uuid, following boolean default true)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null or other_user = auth.uid() then
    raise exception 'invalid_peer';
  end if;
  if following then
    insert into public.user_follows (follower_id, followee_id) values (auth.uid(), other_user)
    on conflict do nothing;
  else
    delete from public.user_follows where follower_id = auth.uid() and followee_id = other_user;
  end if;
end;
$$;

create or replace function public.add_moment_comment(target uuid, p_body text)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare created uuid;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if char_length(btrim(coalesce(p_body, ''))) < 1 then
    raise exception 'empty_comment';
  end if;
  insert into public.moment_comments (moment_id, author_id, body)
  values (target, auth.uid(), left(btrim(p_body), 2200))
  returning id into created;
  insert into public.feed_events (actor_id, moment_id, event_type)
  values (auth.uid(), target, 'comment');
  perform public.refresh_moment_momentum(target);
  return created;
end;
$$;

create or replace function public.moment_comments(target uuid)
returns table (id uuid, author_name text, body text, created_at timestamptz)
language sql
stable
security definer
set search_path = public
as $$
  select c.id, p.display_name, c.body, c.created_at
  from public.moment_comments c
  join public.profiles p on p.id = c.author_id
  where c.moment_id = target and c.deleted_at is null
  order by c.created_at asc
  limit 40;
$$;

drop function if exists public.feed_page(text, integer);
create function public.feed_page(p_kind text default 'for_you', page_size integer default 12)
returns table (
  id uuid,
  creator_id uuid,
  creator_name text,
  creator_username text,
  creator_verified boolean,
  kind public.moment_kind,
  caption text,
  suburb text,
  venue_name text,
  published_at timestamptz,
  is_live boolean,
  is_replay boolean,
  live_viewers integer,
  like_count integer,
  ripple_count integer,
  comment_count integer,
  save_count integer,
  liked boolean,
  rippled boolean,
  saved boolean,
  following boolean,
  mine boolean,
  momentum_band text,
  friend_ripples integer,
  invitation_state text,
  invitation_until timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  with ranked as (
    select fi.moment_id, fi.score
    from public.feed_items fi
    where fi.user_id = auth.uid()
      and fi.feed_kind = p_kind
      and (fi.expires_at is null or fi.expires_at > now())
  ),
  fallback as (
    select m.id as moment_id, extract(epoch from m.published_at) as score
    from public.moments m
    where auth.uid() is not null
      and m.deleted_at is null
      and m.moderation_state = 'approved'
      and not exists (select 1 from ranked)
      and (
        p_kind <> 'following'
        or exists (select 1 from public.user_follows f where f.follower_id = auth.uid() and f.followee_id = m.creator_id)
      )
      and (
        p_kind <> 'live'
        or exists (select 1 from public.live_broadcasts lb where lb.moment_id = m.id and lb.state = 'live')
      )
  ),
  picked as (
    select moment_id, score from ranked
    union all
    select moment_id, score from fallback
  )
  select m.id,
         m.creator_id,
         p.display_name,
         p.username,
         coalesce(p.is_verified, false),
         m.kind,
         coalesce(m.caption, ''),
         coalesce(m.venue_id, ''),
         coalesce(inv.venue_id, coalesce(m.venue_id, '')),
         m.published_at,
         exists(select 1 from public.live_broadcasts lb where lb.moment_id = m.id and lb.state = 'live'),
         m.kind = 'replay' or exists(select 1 from public.live_broadcasts lb where lb.moment_id = m.id and lb.state = 'replay_ready'),
         coalesce((select lb.viewer_count from public.live_broadcasts lb where lb.moment_id = m.id limit 1), 0),
         coalesce(mo.like_count, 0),
         coalesce(mo.ripple_count, 0),
         coalesce(mo.comment_count, 0),
         coalesce(mo.save_count, 0),
         exists(select 1 from public.moment_reactions r where r.moment_id = m.id and r.user_id = auth.uid() and r.reaction = 'like'),
         exists(select 1 from public.moment_reactions r where r.moment_id = m.id and r.user_id = auth.uid() and r.reaction = 'ripple'),
         exists(select 1 from public.moment_reactions r where r.moment_id = m.id and r.user_id = auth.uid() and r.reaction = 'save'),
         exists(select 1 from public.user_follows f where f.follower_id = auth.uid() and f.followee_id = m.creator_id),
         m.creator_id = auth.uid(),
         coalesce(mo.band, 'quiet'),
         (
           select count(*)::int from public.moment_reactions r
           join public.user_follows f on f.followee_id = r.user_id
           where r.moment_id = m.id and r.reaction = 'ripple' and f.follower_id = auth.uid()
         ),
         inv.state::text,
         inv.available_until
  from picked pk
  join public.moments m on m.id = pk.moment_id
  join public.profiles p on p.id = m.creator_id
  left join public.moment_momentum mo on mo.moment_id = m.id
  left join public.moment_invitations inv on inv.moment_id = m.id
  where m.deleted_at is null
    and not exists (
      select 1 from public.user_blocks b
      where (b.blocker_id = auth.uid() and b.blocked_id = m.creator_id)
         or (b.blocker_id = m.creator_id and b.blocked_id = auth.uid())
    )
  order by pk.score desc, m.published_at desc
  limit greatest(1, least(coalesce(page_size, 12), 30));
$$;

grant execute on function public.toggle_moment_reaction(uuid, text) to authenticated;
grant execute on function public.follow_profile(uuid, boolean) to authenticated;
grant execute on function public.add_moment_comment(uuid, text) to authenticated;
grant execute on function public.moment_comments(uuid) to authenticated;
grant execute on function public.feed_page(text, integer) to authenticated;
