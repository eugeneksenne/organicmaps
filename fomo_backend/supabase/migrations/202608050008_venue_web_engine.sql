-- Venue Web Experience Engine — session, download, analytics, and error tables.
-- These are backend-of-record rows; the Android/iOS clients keep a local cache but the
-- canonical session history, aggregated analytics and reported website errors live here.

create type public.venue_web_event_type as enum (
  'opened', 'closed', 'route_engine_launched', 'comms_engine_launched',
  'share_engine_launched', 'plan_overlay_opened', 'error_occurred', 'session_recovered',
  'download_captured'
);
create type public.venue_web_error_type as enum (
  'load_failure', 'redirect_loop', 'unsupported_scheme', 'timeout'
);
create type public.venue_download_kind as enum (
  'ticket', 'receipt', 'invoice', 'menu', 'booking_confirmation'
);

create table public.venue_web_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text not null,
  created_at timestamptz not null default now(),
  last_active_at timestamptz not null default now(),
  expires_at timestamptz not null default now() + make_interval(days => 30),
  load_time_ms integer,
  session_duration_ms integer
);
create index venue_web_sessions_user_idx on public.venue_web_sessions(user_id, last_active_at desc);

create table public.venue_downloads (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  venue_id text not null,
  kind public.venue_download_kind not null,
  title text not null,
  file_ref text not null, -- object storage path inside venue_downloads/<user>/<venue>/
  created_at timestamptz not null default now()
);
create index venue_downloads_user_created_idx on public.venue_downloads(user_id, created_at desc);

create table public.venue_web_events (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.venue_web_sessions(id) on delete cascade,
  event_type public.venue_web_event_type not null,
  occurred_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb
);
create index venue_web_events_session_idx on public.venue_web_events(session_id, occurred_at desc);

create table public.venue_web_errors (
  id uuid primary key default gen_random_uuid(),
  venue_id text not null,
  error_type public.venue_web_error_type not null,
  occurred_at timestamptz not null default now(),
  reported_by_user boolean not null default false,
  metadata jsonb not null default '{}'::jsonb
);
create index venue_web_errors_venue_idx on public.venue_web_errors(venue_id, occurred_at desc);

-- Object-storage bucket for downloads (tickets, receipts, menus, booking confirmations).
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('venue_downloads', 'venue_downloads', false, 20971520,
        array['application/pdf', 'image/jpeg', 'image/png', 'text/plain'])
on conflict (id) do nothing;

alter table public.venue_web_sessions enable row level security;
alter table public.venue_downloads enable row level security;
alter table public.venue_web_events enable row level security;
alter table public.venue_web_errors enable row level security;

create policy "owner reads own sessions" on public.venue_web_sessions
  for select to authenticated using (user_id = auth.uid());
create policy "service inserts sessions" on public.venue_web_sessions
  for insert to authenticated with check (user_id = auth.uid());

create policy "owner reads own downloads" on public.venue_downloads
  for select to authenticated using (user_id = auth.uid());
create policy "service inserts downloads" on public.venue_downloads
  for insert to authenticated with check (user_id = auth.uid());
create policy "owner deletes own downloads" on public.venue_downloads
  for delete to authenticated using (user_id = auth.uid());

create policy "owner reads own events" on public.venue_web_events
  for select to authenticated using (exists(
    select 1 from public.venue_web_sessions s
    where s.id = session_id and s.user_id = auth.uid()));
create policy "service inserts events" on public.venue_web_events
  for insert to authenticated with check (exists(
    select 1 from public.venue_web_sessions s
    where s.id = session_id and s.user_id = auth.uid()));

create policy "authenticated reports errors" on public.venue_web_errors
  for insert to authenticated with check (true);
create policy "authenticated reads aggregated errors" on public.venue_web_errors
  for select to authenticated using (true);
