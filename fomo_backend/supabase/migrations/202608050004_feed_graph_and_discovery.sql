create table public.user_follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  followee_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key(follower_id, followee_id), check(follower_id <> followee_id)
);
create table public.user_blocks (
  blocker_id uuid not null references public.profiles(id) on delete cascade,
  blocked_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key(blocker_id, blocked_id), check(blocker_id <> blocked_id)
);
-- Precision is intentionally limited by product policy; exact location must never be used without consent.
create table public.user_discovery_locations (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  latitude numeric not null check(latitude between -90 and 90),
  longitude numeric not null check(longitude between -180 and 180),
  consented boolean not null default false,
  updated_at timestamptz not null default now(), expires_at timestamptz not null
);
create table public.venue_snapshots (
  venue_id text primary key,
  latitude numeric not null check(latitude between -90 and 90),
  longitude numeric not null check(longitude between -180 and 180),
  safety_state text not null check(safety_state in ('approved', 'review', 'blocked')),
  updated_at timestamptz not null default now()
);
alter table public.user_follows enable row level security;
alter table public.user_blocks enable row level security;
alter table public.user_discovery_locations enable row level security;
alter table public.venue_snapshots enable row level security;
create policy "follows readable" on public.user_follows for select to authenticated using (true);
create policy "user manages follows" on public.user_follows for all to authenticated using (follower_id = auth.uid()) with check (follower_id = auth.uid());
create policy "user manages blocks" on public.user_blocks for all to authenticated using (blocker_id = auth.uid()) with check (blocker_id = auth.uid());
create policy "user manages discovery location" on public.user_discovery_locations for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "approved venue snapshots readable" on public.venue_snapshots for select to authenticated using (safety_state = 'approved');
