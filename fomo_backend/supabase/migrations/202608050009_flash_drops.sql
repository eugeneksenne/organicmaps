create table public.flash_drops (
  id uuid primary key default gen_random_uuid(),
  city_id text not null,
  source_name text not null,
  source_verified boolean not null default false,
  category text not null check (category in ('venue', 'event', 'brand')),
  title text not null,
  description text not null,
  offer_text text not null,
  address text not null,
  latitude numeric not null check (latitude between -90 and 90),
  longitude numeric not null check (longitude between -180 and 180),
  image_url text,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  created_at timestamptz not null default now(),
  active boolean not null default true,
  check (ends_at > starts_at)
);
create index flash_drops_active_rank_idx on public.flash_drops(city_id, category, starts_at, ends_at) where active;
create table public.flash_drop_interests (
  drop_id uuid not null references public.flash_drops(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (drop_id, user_id)
);
alter table public.flash_drops enable row level security;
alter table public.flash_drop_interests enable row level security;
create policy "currently active flash drops readable" on public.flash_drops for select to anon, authenticated using (active and ends_at > now());
create policy "users manage own flash drop interests" on public.flash_drop_interests for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Active only: expired Drops never leak into discovery, even if a client has stale cached data.
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
      (select count(*) from public.flash_drop_interests i where i.drop_id = d.id) as interests
    from public.flash_drops d where d.city_id = p_city_id and d.active and d.starts_at <= now() and d.ends_at > now() and (p_category is null or d.category = p_category)
  )
  select id, source_name, source_verified, category, title, description, offer_text, to_char(starts_at at time zone 'Africa/Johannesburg', 'HH24:MI') || '–' || to_char(ends_at at time zone 'Africa/Johannesburg', 'HH24:MI'), address, round(distance::numeric, 1), starts_at, ends_at, created_at, greatest(0, extract(epoch from ends_at - now())::bigint), interests,
    case when created_at > now() - interval '20 minutes' then 'JUST DROPPED' when ends_at < now() + interval '15 minutes' then 'ENDS SOON' when starts_at > now() then 'TONIGHT ONLY' else 'TRENDING' end
  from active_drops
  order by (starts_at <= now()) desc, distance asc, ends_at asc, created_at desc, interests desc
  limit greatest(1, least(p_limit, 50));
$$;
grant execute on function public.active_flash_drops(text, text, numeric, numeric, integer) to anon, authenticated;
