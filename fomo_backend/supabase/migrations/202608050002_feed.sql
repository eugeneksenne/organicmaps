create type public.moment_kind as enum ('photo', 'video', 'live', 'replay', 'template', 'sponsored');
create type public.moment_visibility as enum ('private', 'followers', 'public');
create type public.invitation_state as enum ('active', 'ended', 'venue_closed');

create table public.moments (
  id uuid primary key default gen_random_uuid(),
  creator_id uuid not null references public.profiles(id) on delete cascade,
  kind public.moment_kind not null,
  visibility public.moment_visibility not null default 'followers',
  caption text check (char_length(caption) <= 2200),
  media_path text not null,
  thumbnail_path text,
  duration_ms integer check (duration_ms is null or duration_ms >= 0),
  venue_id text,
  event_id text,
  published_at timestamptz not null default now(),
  deleted_at timestamptz,
  moderation_state text not null default 'pending' check (moderation_state in ('pending', 'approved', 'rejected'))
);
create index moments_creator_published_idx on public.moments(creator_id, published_at desc);
create index moments_published_idx on public.moments(published_at desc) where deleted_at is null;

create table public.moment_invitations (
  moment_id uuid primary key references public.moments(id) on delete cascade,
  venue_id text not null,
  state public.invitation_state not null default 'active',
  available_until timestamptz,
  ended_at timestamptz,
  created_at timestamptz not null default now()
);
create table public.moment_reactions (
  moment_id uuid not null references public.moments(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  reaction text not null check (reaction in ('like', 'ripple', 'save')),
  created_at timestamptz not null default now(),
  primary key(moment_id, user_id, reaction)
);
create table public.moment_comments (
  id uuid primary key default gen_random_uuid(),
  moment_id uuid not null references public.moments(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 2200),
  created_at timestamptz not null default now(),
  deleted_at timestamptz
);
create index moment_comments_moment_idx on public.moment_comments(moment_id, created_at desc);

-- Materialized/personalised feed rows are written by a trusted ranking worker, never by clients.
create table public.feed_items (
  user_id uuid not null references public.profiles(id) on delete cascade,
  moment_id uuid not null references public.moments(id) on delete cascade,
  feed_kind text not null check (feed_kind in ('for_you', 'following', 'nearby', 'live')),
  score numeric not null,
  reasons jsonb not null default '[]'::jsonb,
  generated_at timestamptz not null default now(),
  expires_at timestamptz,
  primary key(user_id, moment_id, feed_kind)
);
create index feed_items_user_kind_score_idx on public.feed_items(user_id, feed_kind, score desc, generated_at desc);

alter table public.moments enable row level security;
alter table public.moment_invitations enable row level security;
alter table public.moment_reactions enable row level security;
alter table public.moment_comments enable row level security;
alter table public.feed_items enable row level security;

create policy "approved public moments readable" on public.moments for select to authenticated using (
  deleted_at is null and moderation_state = 'approved' and (visibility = 'public' or creator_id = auth.uid())
);
create policy "creator creates moments" on public.moments for insert to authenticated with check (creator_id = auth.uid());
create policy "creator updates moments" on public.moments for update to authenticated using (creator_id = auth.uid()) with check (creator_id = auth.uid());
create policy "read invitation with moment" on public.moment_invitations for select to authenticated using (
  exists(select 1 from public.moments m where m.id = moment_id and m.deleted_at is null)
);
create policy "moment creator manages invitation" on public.moment_invitations for all to authenticated using (
  exists(select 1 from public.moments m where m.id = moment_id and m.creator_id = auth.uid())
) with check (
  exists(select 1 from public.moments m where m.id = moment_id and m.creator_id = auth.uid())
);
create policy "reactions readable" on public.moment_reactions for select to authenticated using (true);
create policy "user manages own reaction" on public.moment_reactions for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "comments readable" on public.moment_comments for select to authenticated using (deleted_at is null);
create policy "author adds comments" on public.moment_comments for insert to authenticated with check (author_id = auth.uid());
create policy "author manages comments" on public.moment_comments for update to authenticated using (author_id = auth.uid()) with check (author_id = auth.uid());
create policy "owner reads ranked feed" on public.feed_items for select to authenticated using (user_id = auth.uid());
