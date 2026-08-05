create table public.city_discover_snapshots (
  city_id text primary key,
  city_name text not null,
  headline text not null,
  recommendation text not null,
  weather_summary text not null,
  energy_percent smallint not null check (energy_percent between 0 and 100),
  live_venue_count integer not null default 0 check (live_venue_count >= 0),
  flash_drop_count integer not null default 0 check (flash_drop_count >= 0),
  updated_at timestamptz not null default now(),
  expires_at timestamptz not null
);
alter table public.city_discover_snapshots enable row level security;
create policy "current city snapshots readable" on public.city_discover_snapshots for select to anon, authenticated using (expires_at > now());
