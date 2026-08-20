-- Ripple Engine: user tap → moment → venue → event → city → creator.
-- Clients never write scores or bands. Ripple never stores live location,
-- Who's Here, or Moment Invitation settings.

create or replace function public.ripple_band(
  p_twenty integer,
  p_hour integer,
  p_tonight integer,
  p_total integer,
  p_score numeric
) returns text
language sql
immutable
as $$
  select case
    when p_twenty >= 40 or p_hour >= 50 or p_tonight >= 500 or p_total >= 500 or p_score >= 400 then 'viral'
    when p_twenty >= 12 or p_hour >= 15 or p_tonight >= 100 or p_total >= 100 or p_score >= 80 then 'hot'
    when p_twenty >= 4 or p_hour >= 5 or p_tonight >= 25 or p_total >= 25 or p_score >= 20 then 'heating'
    when p_total >= 3 or p_tonight >= 3 or p_score >= 3 then 'active'
    else 'quiet'
  end;
$$;

create or replace function public.ripple_trust_for(p_user uuid, p_creator uuid)
returns numeric
language sql
stable
security definer
set search_path = public
as $$
  select case
    when p_user is null then 0
    when p_user = p_creator then 0.2
    when exists (select 1 from public.profiles p where p.id = p_user and p.is_verified) then 1.0
    when exists (select 1 from public.profiles p where p.id = p_user and p.created_at < now() - interval '7 days') then 0.8
    else 0.4
  end;
$$;

create or replace function public.ripple_trust_weight()
returns numeric
language sql
stable
security definer
set search_path = public
as $$
  select public.ripple_trust_for(auth.uid(), null);
$$;

alter table public.moment_momentum
  add column if not exists trust_score numeric not null default 0,
  add column if not exists velocity numeric not null default 0,
  add column if not exists decayed_score numeric not null default 0,
  add column if not exists tonight_count integer not null default 0,
  add column if not exists last_ripple_at timestamptz;

create table if not exists public.venue_momentum (
  venue_id text primary key,
  ripple_count integer not null default 0,
  tonight_count integer not null default 0,
  hour_count integer not null default 0,
  twenty_count integer not null default 0,
  trust_score numeric not null default 0,
  velocity numeric not null default 0,
  decayed_score numeric not null default 0,
  band text not null default 'quiet' check (band in ('quiet', 'active', 'heating', 'hot', 'viral')),
  last_ripple_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.event_momentum (
  event_id text primary key,
  ripple_count integer not null default 0,
  tonight_count integer not null default 0,
  hour_count integer not null default 0,
  twenty_count integer not null default 0,
  trust_score numeric not null default 0,
  velocity numeric not null default 0,
  decayed_score numeric not null default 0,
  band text not null default 'quiet' check (band in ('quiet', 'active', 'heating', 'hot', 'viral')),
  last_ripple_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.city_momentum (
  city_id text primary key,
  ripple_count integer not null default 0,
  tonight_count integer not null default 0,
  hour_count integer not null default 0,
  twenty_count integer not null default 0,
  trust_score numeric not null default 0,
  velocity numeric not null default 0,
  decayed_score numeric not null default 0,
  band text not null default 'quiet' check (band in ('quiet', 'active', 'heating', 'hot', 'viral')),
  last_ripple_at timestamptz,
  updated_at timestamptz not null default now()
);

create table if not exists public.creator_momentum (
  creator_id uuid primary key references public.profiles(id) on delete cascade,
  ripple_count integer not null default 0,
  tonight_count integer not null default 0,
  hour_count integer not null default 0,
  twenty_count integer not null default 0,
  trust_score numeric not null default 0,
  velocity numeric not null default 0,
  decayed_score numeric not null default 0,
  band text not null default 'quiet' check (band in ('quiet', 'active', 'heating', 'hot', 'viral')),
  last_ripple_at timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.venue_momentum enable row level security;
alter table public.event_momentum enable row level security;
alter table public.city_momentum enable row level security;
alter table public.creator_momentum enable row level security;

drop policy if exists "venue momentum readable" on public.venue_momentum;
create policy "venue momentum readable" on public.venue_momentum for select to anon, authenticated using (true);
drop policy if exists "event momentum readable" on public.event_momentum;
create policy "event momentum readable" on public.event_momentum for select to anon, authenticated using (true);
drop policy if exists "city momentum readable" on public.city_momentum;
create policy "city momentum readable" on public.city_momentum for select to anon, authenticated using (true);
drop policy if exists "creator momentum readable" on public.creator_momentum;
create policy "creator momentum readable" on public.creator_momentum for select to authenticated using (true);

revoke all on public.moment_momentum from anon, authenticated;
revoke all on public.venue_momentum from anon, authenticated;
revoke all on public.event_momentum from anon, authenticated;
revoke all on public.city_momentum from anon, authenticated;
revoke all on public.creator_momentum from anon, authenticated;

grant select (moment_id, ripple_count, ripple_hour, like_count, save_count, comment_count, band, tonight_count, updated_at)
  on public.moment_momentum to authenticated;
grant select (venue_id, ripple_count, tonight_count, hour_count, twenty_count, band, updated_at)
  on public.venue_momentum to anon, authenticated;
grant select (event_id, ripple_count, tonight_count, hour_count, twenty_count, band, updated_at)
  on public.event_momentum to anon, authenticated;
grant select (city_id, ripple_count, tonight_count, hour_count, twenty_count, band, updated_at)
  on public.city_momentum to anon, authenticated;
grant select (creator_id, ripple_count, tonight_count, hour_count, twenty_count, band, updated_at)
  on public.creator_momentum to authenticated;

create index if not exists moment_reactions_ripple_time_idx
  on public.moment_reactions (moment_id, created_at desc) where reaction = 'ripple';
create index if not exists moment_reactions_user_time_idx
  on public.moment_reactions (user_id, created_at desc);

create or replace function public.ensure_moment_momentum()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.moment_momentum (moment_id) values (new.id) on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists moments_ensure_momentum on public.moments;
create trigger moments_ensure_momentum
  after insert on public.moments
  for each row execute procedure public.ensure_moment_momentum();

create or replace function public.refresh_moment_momentum(target uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  ripples integer;
  hour_count integer;
  twenty_count integer;
  tonight integer;
  likes integer;
  saves integer;
  comments integer;
  viewers integer := 0;
  trust numeric := 0;
  velocity numeric := 0;
  last_at timestamptz;
  age_hours numeric := 0;
  decayed numeric := 0;
  creator uuid;
  next_band text;
begin
  select creator_id into creator from public.moments where id = target;
  if creator is null then
    return;
  end if;
  select
    count(*) filter (where reaction = 'ripple'),
    count(*) filter (where reaction = 'like'),
    count(*) filter (where reaction = 'save'),
    count(*) filter (where reaction = 'ripple' and created_at > now() - interval '1 hour'),
    count(*) filter (where reaction = 'ripple' and created_at > now() - interval '20 minutes'),
    count(*) filter (where reaction = 'ripple' and (timezone('Africa/Johannesburg', created_at))::date = (timezone('Africa/Johannesburg', now()))::date),
    max(created_at) filter (where reaction = 'ripple'),
    coalesce(sum(public.ripple_trust_for(user_id, creator)) filter (where reaction = 'ripple'), 0)
    into ripples, likes, saves, hour_count, twenty_count, tonight, last_at, trust
    from public.moment_reactions where moment_id = target;
  select count(*) into comments
    from public.moment_comments where moment_id = target and deleted_at is null;
  select coalesce(viewer_count, 0) into viewers
    from public.live_broadcasts where moment_id = target and state = 'live';
  velocity := (coalesce(twenty_count, 0) * 4) + (coalesce(hour_count, 0) * 2) + (coalesce(tonight, 0) * 0.25) + (coalesce(viewers, 0) * 0.02);
  if last_at is not null then
    age_hours := greatest(0, extract(epoch from (now() - last_at)) / 3600.0);
  elsif exists (select 1 from public.moments m where m.id = target) then
    select greatest(0, extract(epoch from (now() - published_at)) / 3600.0) into age_hours
      from public.moments where id = target;
  end if;
  decayed := (coalesce(trust, 0) + velocity) * exp(-age_hours / 12.0);
  next_band := public.ripple_band(coalesce(twenty_count, 0), coalesce(hour_count, 0), coalesce(tonight, 0), coalesce(ripples, 0), decayed);
  insert into public.moment_momentum (
    moment_id, ripple_count, ripple_hour, like_count, save_count, comment_count,
    band, trust_score, velocity, decayed_score, tonight_count, last_ripple_at, updated_at
  ) values (
    target, coalesce(ripples, 0), coalesce(hour_count, 0), coalesce(likes, 0), coalesce(saves, 0), coalesce(comments, 0),
    next_band, trust, velocity, decayed, coalesce(tonight, 0), last_at, now()
  )
  on conflict (moment_id) do update
    set ripple_count = excluded.ripple_count,
        ripple_hour = excluded.ripple_hour,
        like_count = excluded.like_count,
        save_count = excluded.save_count,
        comment_count = excluded.comment_count,
        band = excluded.band,
        trust_score = excluded.trust_score,
        velocity = excluded.velocity,
        decayed_score = excluded.decayed_score,
        tonight_count = excluded.tonight_count,
        last_ripple_at = excluded.last_ripple_at,
        updated_at = now();
end;
$$;

create or replace function public.refresh_subject_momentum(
  p_kind text,
  p_id text
) returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  ripples integer := 0;
  hour_count integer := 0;
  twenty_count integer := 0;
  tonight integer := 0;
  trust numeric := 0;
  velocity numeric := 0;
  last_at timestamptz;
  age_hours numeric := 0;
  decayed numeric := 0;
  next_band text;
begin
  if p_id is null or length(btrim(p_id)) = 0 then
    return;
  end if;
  with src as (
    select r.user_id, r.created_at, m.creator_id
    from public.moment_reactions r
    join public.moments m on m.id = r.moment_id
    left join public.city_explore_venues v on v.id::text = m.venue_id or v.name = m.venue_id
    where r.reaction = 'ripple'
      and m.deleted_at is null
      and (
        (p_kind = 'venue' and m.venue_id = p_id)
        or (p_kind = 'event' and m.event_id = p_id)
        or (p_kind = 'city' and (v.city_id = p_id or exists (
          select 1 from public.profiles p where p.id = m.creator_id and p.city_id = p_id
        )))
        or (p_kind = 'creator' and m.creator_id::text = p_id)
      )
  )
  select count(*),
         count(*) filter (where created_at > now() - interval '1 hour'),
         count(*) filter (where created_at > now() - interval '20 minutes'),
         count(*) filter (where (timezone('Africa/Johannesburg', created_at))::date = (timezone('Africa/Johannesburg', now()))::date),
         coalesce(sum(public.ripple_trust_for(user_id, creator_id)), 0),
         max(created_at)
    into ripples, hour_count, twenty_count, tonight, trust, last_at
    from src;
  velocity := (coalesce(twenty_count, 0) * 4) + (coalesce(hour_count, 0) * 2) + (coalesce(tonight, 0) * 0.25);
  if last_at is not null then
    age_hours := greatest(0, extract(epoch from (now() - last_at)) / 3600.0);
  end if;
  decayed := (coalesce(trust, 0) + velocity) * exp(-age_hours / 12.0);
  next_band := public.ripple_band(coalesce(twenty_count, 0), coalesce(hour_count, 0), coalesce(tonight, 0), coalesce(ripples, 0), decayed);
  if p_kind = 'venue' then
    insert into public.venue_momentum (venue_id, ripple_count, tonight_count, hour_count, twenty_count, trust_score, velocity, decayed_score, band, last_ripple_at, updated_at)
    values (p_id, coalesce(ripples, 0), coalesce(tonight, 0), coalesce(hour_count, 0), coalesce(twenty_count, 0), trust, velocity, decayed, next_band, last_at, now())
    on conflict (venue_id) do update
      set ripple_count = excluded.ripple_count, tonight_count = excluded.tonight_count, hour_count = excluded.hour_count,
          twenty_count = excluded.twenty_count, trust_score = excluded.trust_score, velocity = excluded.velocity,
          decayed_score = excluded.decayed_score, band = excluded.band, last_ripple_at = excluded.last_ripple_at, updated_at = now();
  elsif p_kind = 'event' then
    insert into public.event_momentum (event_id, ripple_count, tonight_count, hour_count, twenty_count, trust_score, velocity, decayed_score, band, last_ripple_at, updated_at)
    values (p_id, coalesce(ripples, 0), coalesce(tonight, 0), coalesce(hour_count, 0), coalesce(twenty_count, 0), trust, velocity, decayed, next_band, last_at, now())
    on conflict (event_id) do update
      set ripple_count = excluded.ripple_count, tonight_count = excluded.tonight_count, hour_count = excluded.hour_count,
          twenty_count = excluded.twenty_count, trust_score = excluded.trust_score, velocity = excluded.velocity,
          decayed_score = excluded.decayed_score, band = excluded.band, last_ripple_at = excluded.last_ripple_at, updated_at = now();
  elsif p_kind = 'city' then
    insert into public.city_momentum (city_id, ripple_count, tonight_count, hour_count, twenty_count, trust_score, velocity, decayed_score, band, last_ripple_at, updated_at)
    values (p_id, coalesce(ripples, 0), coalesce(tonight, 0), coalesce(hour_count, 0), coalesce(twenty_count, 0), trust, velocity, decayed, next_band, last_at, now())
    on conflict (city_id) do update
      set ripple_count = excluded.ripple_count, tonight_count = excluded.tonight_count, hour_count = excluded.hour_count,
          twenty_count = excluded.twenty_count, trust_score = excluded.trust_score, velocity = excluded.velocity,
          decayed_score = excluded.decayed_score, band = excluded.band, last_ripple_at = excluded.last_ripple_at, updated_at = now();
  elsif p_kind = 'creator' then
    insert into public.creator_momentum (creator_id, ripple_count, tonight_count, hour_count, twenty_count, trust_score, velocity, decayed_score, band, last_ripple_at, updated_at)
    values (p_id::uuid, coalesce(ripples, 0), coalesce(tonight, 0), coalesce(hour_count, 0), coalesce(twenty_count, 0), trust, velocity, decayed, next_band, last_at, now())
    on conflict (creator_id) do update
      set ripple_count = excluded.ripple_count, tonight_count = excluded.tonight_count, hour_count = excluded.hour_count,
          twenty_count = excluded.twenty_count, trust_score = excluded.trust_score, velocity = excluded.velocity,
          decayed_score = excluded.decayed_score, band = excluded.band, last_ripple_at = excluded.last_ripple_at, updated_at = now();
  end if;
end;
$$;

create or replace function public.refresh_ripple_graph(target uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id text;
  e_id text;
  c_id text;
  creator uuid;
begin
  select m.venue_id, m.event_id, m.creator_id into v_id, e_id, creator
    from public.moments m where m.id = target;
  if creator is null then
    return;
  end if;
  select coalesce(
    (select v.city_id from public.city_explore_venues v where v.id::text = v_id or v.name = v_id limit 1),
    (select p.city_id from public.profiles p where p.id = creator)
  ) into c_id;
  if v_id is not null and length(btrim(v_id)) > 0 then
    perform public.refresh_subject_momentum('venue', v_id);
  end if;
  if e_id is not null and length(btrim(e_id)) > 0 then
    perform public.refresh_subject_momentum('event', e_id);
  end if;
  if c_id is not null and length(btrim(c_id)) > 0 then
    perform public.refresh_subject_momentum('city', c_id);
  end if;
  perform public.refresh_subject_momentum('creator', creator::text);
end;
$$;

create or replace function public.decay_ripple_momentum()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  rec record;
  n integer := 0;
begin
  for rec in
    select moment_id
      from public.moment_momentum
     where ripple_count > 0
       and (last_ripple_at is null or last_ripple_at < now() - interval '10 minutes')
     order by updated_at asc
     limit 200
  loop
    perform public.refresh_moment_momentum(rec.moment_id);
    perform public.refresh_ripple_graph(rec.moment_id);
    n := n + 1;
  end loop;
  return n;
end;
$$;

create or replace function public.moment_public_momentum(target uuid)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'moment_id', m.moment_id,
    'band', m.band,
    'ripple_count', m.ripple_count,
    'tonight_count', m.tonight_count,
    'ripple_hour', m.ripple_hour,
    'like_count', m.like_count,
    'save_count', m.save_count,
    'comment_count', m.comment_count,
    'updated_at', m.updated_at
  )
  from public.moment_momentum m
  where m.moment_id = target;
$$;

create or replace function public.set_moment_reaction(target uuid, p_reaction text, p_on boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  removed boolean := not p_on;
  existed boolean := false;
  trust numeric;
  creator uuid;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if p_reaction not in ('like', 'ripple', 'save') then
    raise exception 'invalid_reaction';
  end if;
  select creator_id into creator from public.moments
   where id = target and deleted_at is null;
  if creator is null then
    raise exception 'unknown_moment';
  end if;
  if creator <> auth.uid() then
    if not exists (
      select 1 from public.moments m
      where m.id = target
        and m.moderation_state = 'approved'
        and (
          m.visibility = 'public'
          or (
            m.visibility = 'followers'
            and exists (
              select 1 from public.user_follows f
              where f.follower_id = auth.uid() and f.followee_id = m.creator_id
            )
          )
        )
    ) then
      raise exception 'unknown_moment';
    end if;
  end if;
  if (select count(*) from public.moment_reactions
      where user_id = auth.uid() and created_at > now() - interval '1 minute') >= 40 then
    raise exception 'rate_limited';
  end if;
  existed := exists (
    select 1 from public.moment_reactions
    where moment_id = target and user_id = auth.uid() and reaction = p_reaction
  );
  if p_on and not existed and p_reaction = 'ripple' then
    trust := public.ripple_trust_for(auth.uid(), creator);
    if trust < 0.5 and (
      select count(*) from public.moment_reactions
      where user_id = auth.uid() and reaction = 'ripple' and created_at > now() - interval '1 hour'
    ) >= 10 then
      raise exception 'rate_limited';
    end if;
  end if;
  if p_on then
    insert into public.moment_reactions (moment_id, user_id, reaction)
    values (target, auth.uid(), p_reaction)
    on conflict do nothing;
    if not existed then
      insert into public.feed_events (actor_id, moment_id, event_type, metadata)
      values (auth.uid(), target, p_reaction, jsonb_build_object('trust', public.ripple_trust_for(auth.uid(), creator)));
    end if;
    removed := false;
  else
    delete from public.moment_reactions
     where moment_id = target and user_id = auth.uid() and reaction = p_reaction;
    removed := true;
  end if;
  perform public.refresh_moment_momentum(target);
  if p_reaction = 'ripple' then
    perform public.refresh_ripple_graph(target);
  end if;
  return jsonb_build_object(
    'removed', removed,
    'trust', public.ripple_trust_for(auth.uid(), creator),
    'momentum', public.moment_public_momentum(target)
  );
end;
$$;

create or replace function public.toggle_moment_reaction(target uuid, p_reaction text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  p_on boolean;
begin
  p_on := not exists (
    select 1 from public.moment_reactions
    where moment_id = target and user_id = auth.uid() and reaction = p_reaction
  );
  return public.set_moment_reaction(target, p_reaction, p_on);
end;
$$;

create or replace function public.ripple_snapshot(p_kind text, p_id text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  result jsonb;
begin
  if p_kind = 'moment' then
    if p_id is null or p_id !~* '^[0-9a-f-]{36}$' then
      return null;
    end if;
    if not exists (
      select 1 from public.moments m
      where m.id = p_id::uuid
        and m.deleted_at is null
        and m.moderation_state = 'approved'
        and (
          m.visibility = 'public'
          or m.creator_id = auth.uid()
          or (
            m.visibility = 'followers'
            and auth.uid() is not null
            and exists (
              select 1 from public.user_follows f
              where f.follower_id = auth.uid() and f.followee_id = m.creator_id
            )
          )
        )
    ) then
      return null;
    end if;
    select public.moment_public_momentum(p_id::uuid) into result;
    if result is not null then
      result := result || jsonb_build_object('kind', 'moment', 'id', p_id);
    end if;
    return result;
  elsif p_kind = 'venue' then
    select jsonb_build_object('kind', 'venue', 'id', v.venue_id, 'band', v.band,
                             'ripple_count', v.ripple_count, 'tonight_count', v.tonight_count, 'updated_at', v.updated_at)
      into result from public.venue_momentum v where v.venue_id = p_id;
  elsif p_kind = 'event' then
    select jsonb_build_object('kind', 'event', 'id', e.event_id, 'band', e.band,
                             'ripple_count', e.ripple_count, 'tonight_count', e.tonight_count, 'updated_at', e.updated_at)
      into result from public.event_momentum e where e.event_id = p_id;
  elsif p_kind = 'city' then
    select jsonb_build_object('kind', 'city', 'id', c.city_id, 'band', c.band,
                             'ripple_count', c.ripple_count, 'tonight_count', c.tonight_count, 'updated_at', c.updated_at)
      into result from public.city_momentum c where c.city_id = p_id;
  elsif p_kind = 'creator' then
    if auth.uid() is null then
      return null;
    end if;
    select jsonb_build_object('kind', 'creator', 'id', cr.creator_id, 'band', cr.band,
                             'ripple_count', cr.ripple_count, 'tonight_count', cr.tonight_count, 'updated_at', cr.updated_at)
      into result from public.creator_momentum cr where cr.creator_id::text = p_id;
  else
    return null;
  end if;
  return result;
end;
$$;

create or replace function public.ripple_trending(p_kind text default 'venue', p_limit integer default 12)
returns table (id text, band text, tonight_count integer, ripple_count integer)
language sql
stable
security definer
set search_path = public
as $$
  select * from (
    select v.venue_id, v.band, v.tonight_count, v.ripple_count
      from public.venue_momentum v
     where p_kind = 'venue' and v.band <> 'quiet'
    union all
    select e.event_id, e.band, e.tonight_count, e.ripple_count
      from public.event_momentum e
     where p_kind = 'event' and e.band <> 'quiet'
    union all
    select c.city_id, c.band, c.tonight_count, c.ripple_count
      from public.city_momentum c
     where p_kind = 'city' and c.band <> 'quiet'
  ) trending (id, band, tonight_count, ripple_count)
  order by case band when 'viral' then 5 when 'hot' then 4 when 'heating' then 3 when 'active' then 2 else 0 end desc,
           tonight_count desc, ripple_count desc
  limit greatest(1, least(coalesce(p_limit, 12), 30));
$$;

create or replace function public.creator_ripple_analytics()
returns table (
  moment_id uuid,
  caption text,
  band text,
  ripple_count integer,
  tonight_count integer,
  like_count integer,
  comment_count integer,
  published_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  select m.id, coalesce(m.caption, ''), coalesce(mo.band, 'quiet'),
         coalesce(mo.ripple_count, 0), coalesce(mo.tonight_count, 0),
         coalesce(mo.like_count, 0), coalesce(mo.comment_count, 0), m.published_at
  from public.moments m
  left join public.moment_momentum mo on mo.moment_id = m.id
  where auth.uid() is not null
    and m.creator_id = auth.uid()
    and m.deleted_at is null
  order by m.published_at desc
  limit 50;
$$;

create or replace function public.explore_city_venues(
  p_city_id text,
  p_world text default null,
  p_latitude numeric default -26.2041,
  p_longitude numeric default 28.0473,
  p_now time default localtime,
  p_local_date date default current_date,
  p_day_of_week smallint default extract(isodow from current_date)::smallint,
  p_limit integer default 30
)
returns table (
  id uuid, name text, suburb text, discovery_world text, venue_kind text, rating numeric,
  distance_km numeric, open_now boolean, closes_at time, is_24_hours boolean, experience_tags text[],
  image_url text, verified boolean, rhythm_message text, local_event_active boolean
)
language sql stable security invoker set search_path = public as $$
  with candidates as (
    select v.*,
      (v.is_24_hours or (v.opens_at is not null and v.closes_at is not null and
        case when v.closes_at > v.opens_at then p_now >= v.opens_at and p_now < v.closes_at else p_now >= v.opens_at or p_now < v.closes_at end)) as venue_open,
      exists (select 1 from public.city_discovery_events e where e.venue_id = v.id and e.active and now() between e.starts_at and e.ends_at) as event_live,
      exists (select 1 from public.city_public_holidays h where h.city_id = p_city_id and h.holiday_date = p_local_date) as holiday,
      coalesce((select case vm.band when 'viral' then 5 when 'hot' then 4 when 'heating' then 3 when 'active' then 2 else 0 end
                from public.venue_momentum vm where vm.venue_id = v.id::text or vm.venue_id = v.name
                order by case vm.band when 'viral' then 5 when 'hot' then 4 when 'heating' then 3 when 'active' then 2 else 0 end desc
                limit 1), 0) as heat
    from public.city_explore_venues v where v.city_id = p_city_id and v.safety_state = 'approved' and (p_world is null or v.discovery_world = p_world)
  )
  select c.id, c.name, c.suburb, c.discovery_world, c.venue_kind, c.rating,
    round((6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(c.latitude)) * cos(radians(c.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(c.latitude)))))::numeric, 1),
    c.venue_open, c.closes_at, c.is_24_hours, c.experience_tags, c.image_url, c.verified,
    case
      when c.discovery_world = 'nightlife' and p_now < '17:00' then 'Opening tonight · plan ahead'
      when c.discovery_world = 'nightlife' and p_now >= '21:00' then 'Open now · late entry available'
      when c.discovery_world = 'nightlife' then 'Open now · tonight''s energy'
      when c.discovery_world = 'food' and p_now < '11:00' then 'Open for breakfast'
      when c.discovery_world = 'food' and p_now >= '02:00' and p_now < '05:00' then 'Still open'
      when c.discovery_world = 'food' and p_now >= '17:00' then 'Dinner tonight'
      when c.is_24_hours then 'Always open'
      when c.venue_open then 'Open now'
      else 'Plan your visit'
    end, c.event_live
  from candidates c
  order by c.venue_open desc, c.event_live desc, c.holiday desc, c.heat desc,
    (6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(c.latitude)) * cos(radians(c.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(c.latitude))))) asc,
    c.quality_rank desc, c.rating desc nulls last, c.featured desc, c.updated_at desc
  limit greatest(1, least(p_limit, 50));
$$;

create or replace function public.smart_places_tonight(p_city_id text, p_mood text default null)
returns table (venue_id uuid, venue_name text, suburb text, rating numeric, reason text, guide_title text, open_now boolean)
language sql stable security invoker set search_path=public as $$
 select v.id, v.name, v.suburb, v.rating, r.reason, g.title,
  (v.is_24_hours or (v.opens_at is not null and v.closes_at is not null and case when v.closes_at > v.opens_at then localtime >= v.opens_at and localtime < v.closes_at else localtime >= v.opens_at or localtime < v.closes_at end))
 from public.smart_place_guides g
 join public.smart_place_recommendations r on r.guide_id=g.id
 join public.city_explore_venues v on v.id=r.venue_id
 where g.city_id=p_city_id and g.active and v.safety_state='approved' and (p_mood is null or g.mood=p_mood)
 order by r.rank asc,
   coalesce((select case vm.band when 'viral' then 5 when 'hot' then 4 when 'heating' then 3 when 'active' then 2 else 0 end
             from public.venue_momentum vm where vm.venue_id = v.id::text or vm.venue_id = v.name
             order by 1 desc limit 1), 0) desc;
$$;

create or replace function public.active_flash_drops(
  p_city_id text,
  p_category text default null,
  p_latitude numeric default -26.2041,
  p_longitude numeric default 28.0473,
  p_limit integer default 30
)
returns table (
  id uuid, source_name text, source_verified boolean, category text, title text, description text, offer_text text, schedule text,
  address text, distance_km numeric, starts_at timestamptz, ends_at timestamptz, created_at timestamptz,
  seconds_remaining bigint, interested_count bigint, status text
)
language sql stable security definer set search_path = public as $$
  with active_drops as (
    select d.*, (6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(d.latitude)) * cos(radians(d.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(d.latitude))))) as distance,
      (select count(*) from public.flash_drop_interests i where i.drop_id = d.id) as interests,
      coalesce((select cm.decayed_score from public.city_momentum cm where cm.city_id = d.city_id), 0) as city_heat
    from public.flash_drops d where d.city_id = p_city_id and d.active and d.starts_at <= now() and d.ends_at > now() and (p_category is null or d.category = p_category)
  )
  select id, source_name, source_verified, category, title, description, offer_text, to_char(starts_at at time zone 'Africa/Johannesburg', 'HH24:MI') || '–' || to_char(ends_at at time zone 'Africa/Johannesburg', 'HH24:MI'), address, round(distance::numeric, 1), starts_at, ends_at, created_at, greatest(0, extract(epoch from ends_at - now())::bigint), interests,
    case when created_at > now() - interval '20 minutes' then 'JUST DROPPED' when ends_at < now() + interval '15 minutes' then 'ENDS SOON' when starts_at > now() then 'TONIGHT ONLY' else 'TRENDING' end
  from active_drops
  order by (starts_at <= now()) desc, city_heat desc, distance asc, ends_at asc, created_at desc, interests desc
  limit greatest(1, least(p_limit, 50));
$$;

comment on table public.venue_momentum is 'Public venue heat. Never stores live location, Who''s Here, or invitation state.';
comment on table public.city_momentum is 'Public city heat aggregated from Ripples. Not attendance.';
comment on function public.ripple_snapshot(text, text) is 'Public band + counts only. Clients never calculate momentum.';

revoke execute on function public.ripple_band(integer, integer, integer, integer, numeric) from public, anon, authenticated;
revoke execute on function public.ripple_trust_for(uuid, uuid) from public, anon, authenticated;
revoke execute on function public.refresh_moment_momentum(uuid) from public, anon, authenticated;
revoke execute on function public.refresh_subject_momentum(text, text) from public, anon, authenticated;
revoke execute on function public.refresh_ripple_graph(uuid) from public, anon, authenticated;
revoke execute on function public.decay_ripple_momentum() from public, anon, authenticated;
revoke execute on function public.moment_public_momentum(uuid) from public, anon, authenticated;
revoke execute on function public.ensure_moment_momentum() from public, anon, authenticated;

grant execute on function public.set_moment_reaction(uuid, text, boolean) to authenticated;
grant execute on function public.toggle_moment_reaction(uuid, text) to authenticated;
grant execute on function public.ripple_snapshot(text, text) to anon, authenticated;
grant execute on function public.ripple_trending(text, integer) to anon, authenticated;
grant execute on function public.creator_ripple_analytics() to authenticated;
grant execute on function public.explore_city_venues(text, text, numeric, numeric, time, date, smallint, integer) to anon, authenticated;
grant execute on function public.smart_places_tonight(text, text) to anon, authenticated;
grant execute on function public.active_flash_drops(text, text, numeric, numeric, integer) to anon, authenticated;
grant execute on function public.decay_ripple_momentum() to service_role;
