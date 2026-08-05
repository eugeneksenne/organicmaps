-- Curated, verified venue discovery. These records are editorial venue facts, not social proof.
create table public.city_explore_venues (
  id uuid primary key default gen_random_uuid(),
  city_id text not null,
  name text not null,
  suburb text not null,
  discovery_world text not null check (discovery_world in ('nightlife', 'food', 'prep', 'wellness', 'travel', 'always_open')),
  venue_kind text not null,
  rating numeric(2,1) check (rating >= 0 and rating <= 5),
  latitude numeric not null check (latitude between -90 and 90),
  longitude numeric not null check (longitude between -180 and 180),
  opens_at time, closes_at time,
  is_24_hours boolean not null default false,
  experience_tags text[] not null default '{}',
  image_url text,
  verified boolean not null default false,
  quality_rank smallint not null default 0 check (quality_rank between 0 and 100),
  featured boolean not null default false,
  safety_state text not null default 'approved' check (safety_state in ('approved', 'review', 'blocked')),
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index city_explore_venues_discovery_idx on public.city_explore_venues(city_id, discovery_world, safety_state, updated_at desc);

create table public.user_saved_venues (
  user_id uuid not null references public.profiles(id) on delete cascade,
  venue_id uuid not null references public.city_explore_venues(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, venue_id)
);

alter table public.city_explore_venues enable row level security;
alter table public.user_saved_venues enable row level security;
create policy "approved explore venues readable" on public.city_explore_venues for select to anon, authenticated using (safety_state = 'approved');
create policy "users manage own saved venues" on public.user_saved_venues for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- The ordering privileges venues that are open, then nearby, editorially well-maintained venues.
-- Location is rounded by the client and never stored by this read-only discovery endpoint.
create or replace function public.explore_city_venues(
  p_city_id text,
  p_world text default null,
  p_latitude numeric default -26.2041,
  p_longitude numeric default 28.0473,
  p_now time default localtime,
  p_limit integer default 30
)
returns table (
  id uuid, name text, suburb text, discovery_world text, venue_kind text, rating numeric,
  distance_km numeric, open_now boolean, closes_at time, is_24_hours boolean, experience_tags text[],
  image_url text, verified boolean
)
language sql stable security invoker set search_path = public as $$
  select v.id, v.name, v.suburb, v.discovery_world, v.venue_kind, v.rating,
    round((6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(v.latitude)) * cos(radians(v.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(v.latitude)))))::numeric, 1) as distance_km,
    (v.is_24_hours or (v.opens_at is not null and v.closes_at is not null and
      case when v.closes_at > v.opens_at then p_now >= v.opens_at and p_now < v.closes_at else p_now >= v.opens_at or p_now < v.closes_at end)) as open_now,
    v.closes_at, v.is_24_hours, v.experience_tags, v.image_url, v.verified
  from public.city_explore_venues v
  where v.city_id = p_city_id and v.safety_state = 'approved' and (p_world is null or v.discovery_world = p_world)
  order by
    (v.is_24_hours or (v.opens_at is not null and v.closes_at is not null and case when v.closes_at > v.opens_at then p_now >= v.opens_at and p_now < v.closes_at else p_now >= v.opens_at or p_now < v.closes_at end)) desc,
    (6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(v.latitude)) * cos(radians(v.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(v.latitude))))) asc,
    v.quality_rank desc, v.rating desc nulls last, v.featured desc, v.updated_at desc
  limit greatest(1, least(p_limit, 50));
$$;
grant execute on function public.explore_city_venues(text, text, numeric, numeric, time, integer) to anon, authenticated;
