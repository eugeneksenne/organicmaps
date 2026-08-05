create table public.live_broadcasts (
  id uuid primary key default gen_random_uuid(),
  moment_id uuid unique references public.moments(id) on delete cascade,
  host_id uuid not null references public.profiles(id),
  livekit_room text not null unique,
  state text not null default 'scheduled' check (state in ('scheduled', 'live', 'ended', 'replay_processing', 'replay_ready')),
  started_at timestamptz, ended_at timestamptz, viewer_count integer not null default 0 check (viewer_count >= 0),
  replay_path text, created_at timestamptz not null default now()
);
create table public.push_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  platform text not null check (platform in ('android', 'ios')),
  token text not null unique,
  app_version text, disabled_at timestamptz, created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table public.feed_events (
  id bigint generated always as identity primary key,
  actor_id uuid not null references public.profiles(id),
  moment_id uuid not null references public.moments(id) on delete cascade,
  event_type text not null check (event_type in ('view', 'watch_complete', 'like', 'ripple', 'save', 'share', 'comment', 'report')),
  occurred_at timestamptz not null default now(), metadata jsonb not null default '{}'::jsonb
);
create index feed_events_moment_time_idx on public.feed_events(moment_id, occurred_at desc);
alter table public.live_broadcasts enable row level security;
alter table public.push_devices enable row level security;
alter table public.feed_events enable row level security;
create policy "live broadcasts readable" on public.live_broadcasts for select to authenticated using (true);
create policy "host manages broadcast" on public.live_broadcasts for all to authenticated using (host_id = auth.uid()) with check (host_id = auth.uid());
create policy "owner manages device" on public.push_devices for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "users write their feed events" on public.feed_events for insert to authenticated with check (actor_id = auth.uid());
