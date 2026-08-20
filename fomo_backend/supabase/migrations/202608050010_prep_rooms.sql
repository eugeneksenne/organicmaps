-- Preparation is tied to a planned event, not a commerce catalogue.
create table public.prep_sessions (
  id uuid primary key default gen_random_uuid(), event_id uuid, venue_name text not null, starts_at timestamptz not null,
  dress_code text, ready_percent smallint not null default 0 check (ready_percent between 0 and 100), active boolean not null default true, created_at timestamptz not null default now()
);
create table public.prep_posts (
  id uuid primary key default gen_random_uuid(), session_id uuid references public.prep_sessions(id) on delete cascade, creator_id uuid references public.profiles(id) on delete cascade,
  kind text not null check (kind in ('look', 'makeup', 'hair', 'nails', 'tutorial', 'tip', 'venue_guide', 'poll')), caption text not null, media_url text, created_at timestamptz not null default now(), moderation_state text not null default 'review' check (moderation_state in ('review', 'approved', 'rejected'))
);
create table public.prep_checklist_items (
  session_id uuid not null references public.prep_sessions(id) on delete cascade, label text not null, position smallint not null, primary key(session_id, position)
);
create table public.prep_checklist_completions (session_id uuid not null references public.prep_sessions(id) on delete cascade, user_id uuid not null references public.profiles(id) on delete cascade, position smallint not null, completed_at timestamptz not null default now(), primary key(session_id, user_id, position));
alter table public.prep_sessions enable row level security; alter table public.prep_posts enable row level security; alter table public.prep_checklist_items enable row level security; alter table public.prep_checklist_completions enable row level security;
create policy "active prep sessions readable" on public.prep_sessions for select to anon, authenticated using (active and starts_at > now() - interval '12 hours');
create policy "approved prep posts readable" on public.prep_posts for select to anon, authenticated using (moderation_state = 'approved');
create policy "prep checklist readable" on public.prep_checklist_items for select to authenticated using (true);
create policy "own prep checklist completion" on public.prep_checklist_completions for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
