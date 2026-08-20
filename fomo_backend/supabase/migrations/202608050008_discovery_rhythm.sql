-- Context signals make Explore follow Johannesburg's rhythm without inventing attendance metrics.
alter table public.city_explore_venues drop constraint city_explore_venues_discovery_world_check;
alter table public.city_explore_venues add constraint city_explore_venues_discovery_world_check
  check (discovery_world in ('nightlife', 'food', 'prep', 'wellness', 'travel', 'always_open', 'rooftops', 'casinos'));

create table public.city_public_holidays (
  city_id text not null,
  holiday_date date not null,
  name text not null,
  primary key (city_id, holiday_date)
);
create table public.city_discovery_events (
  id uuid primary key default gen_random_uuid(),
  city_id text not null,
  discovery_world text not null,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  venue_id uuid references public.city_explore_venues(id) on delete cascade,
  active boolean not null default true,
  check (ends_at > starts_at)
);
create index city_discovery_events_active_idx on public.city_discovery_events(city_id, starts_at, ends_at) where active;
alter table public.city_public_holidays enable row level security;
alter table public.city_discovery_events enable row level security;
create policy "public holidays readable" on public.city_public_holidays for select to anon, authenticated using (true);
create policy "active discovery events readable" on public.city_discovery_events for select to anon, authenticated using (active);

drop function public.explore_city_venues(text, text, numeric, numeric, time, integer);
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
      exists (select 1 from public.city_public_holidays h where h.city_id = p_city_id and h.holiday_date = p_local_date) as holiday
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
  order by c.venue_open desc, c.event_live desc, c.holiday desc, -- special-day programming is relevant, never a popularity proxy
    (6371 * acos(least(1, greatest(-1, cos(radians(p_latitude)) * cos(radians(c.latitude)) * cos(radians(c.longitude) - radians(p_longitude)) + sin(radians(p_latitude)) * sin(radians(c.latitude))))) asc,
    c.quality_rank desc, c.rating desc nulls last, c.featured desc, c.updated_at desc
  limit greatest(1, least(p_limit, 50));
$$;
grant execute on function public.explore_city_venues(text, text, numeric, numeric, time, date, smallint, integer) to anon, authenticated;
