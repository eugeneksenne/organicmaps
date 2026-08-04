create extension if not exists pgcrypto;

create type public.conversation_kind as enum ('direct', 'group', 'venue', 'event', 'nightguard');
create type public.message_kind as enum ('text', 'image', 'video', 'audio', 'voice_note', 'document', 'venue', 'event', 'moment', 'route', 'location', 'system');
create type public.member_role as enum ('owner', 'admin', 'moderator', 'member', 'readonly');
create type public.call_kind as enum ('voice', 'video', 'group_voice', 'group_video');
create type public.call_status as enum ('ringing', 'active', 'ended', 'missed', 'declined', 'cancelled');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null check (username ~ '^[a-z0-9_.]{3,32}$'),
  display_name text not null check (char_length(display_name) between 1 and 80),
  avatar_path text,
  is_verified boolean not null default false,
  presence_visibility text not null default 'friends' check (presence_visibility in ('everyone','followers','friends','nobody')),
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);

create table public.conversations (
  id uuid primary key default gen_random_uuid(), kind public.conversation_kind not null,
  title text, image_path text, description text, created_by uuid not null references public.profiles(id),
  encryption_version smallint not null default 1, created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table public.conversation_members (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role public.member_role not null default 'member', muted_until timestamptz,
  last_read_message_id uuid, joined_at timestamptz not null default now(), left_at timestamptz,
  primary key (conversation_id, user_id)
);
create table public.messages (
  id uuid primary key default gen_random_uuid(), conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id), kind public.message_kind not null default 'text',
  ciphertext bytea not null, encrypted_metadata jsonb not null default '{}'::jsonb,
  client_operation_id uuid not null, reply_to_id uuid references public.messages(id),
  sent_at timestamptz not null default now(), edited_at timestamptz, deleted_at timestamptz,
  unique(sender_id, client_operation_id)
);
create index messages_conversation_sent_idx on public.messages(conversation_id, sent_at desc);
alter table public.conversation_members add constraint conversation_members_last_read_fkey foreign key (last_read_message_id) references public.messages(id) on delete set null;

create table public.message_attachments (
  id uuid primary key default gen_random_uuid(), message_id uuid not null references public.messages(id) on delete cascade,
  storage_path text not null unique, mime_type text not null, byte_size bigint not null check (byte_size >= 0),
  encrypted_key bytea not null, checksum text not null, created_at timestamptz not null default now()
);
create table public.stories (
  id uuid primary key default gen_random_uuid(), author_id uuid not null references public.profiles(id),
  storage_path text not null, encrypted_key bytea not null, audience text not null default 'followers' check (audience in ('public','followers','friends','private')),
  expires_at timestamptz not null, created_at timestamptz not null default now(), deleted_at timestamptz,
  check (expires_at <= created_at + interval '30 days')
);
create table public.story_views (story_id uuid references public.stories(id) on delete cascade, viewer_id uuid references public.profiles(id) on delete cascade, viewed_at timestamptz not null default now(), primary key(story_id, viewer_id));
create table public.call_sessions (
  id uuid primary key default gen_random_uuid(), conversation_id uuid not null references public.conversations(id), initiated_by uuid not null references public.profiles(id), kind public.call_kind not null, status public.call_status not null default 'ringing', started_at timestamptz not null default now(), answered_at timestamptz, ended_at timestamptz, end_reason text
);
create table public.client_operations (
  id uuid primary key, user_id uuid not null references public.profiles(id), operation_type text not null, payload jsonb not null, status text not null default 'queued' check (status in ('queued','processing','completed','failed')), created_at timestamptz not null default now(), completed_at timestamptz
);

create or replace function public.is_conversation_member(target uuid) returns boolean language sql stable security definer set search_path = public as $$
  select exists(select 1 from public.conversation_members where conversation_id = target and user_id = auth.uid() and left_at is null);
$$;
alter table public.profiles enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.message_attachments enable row level security;
alter table public.stories enable row level security;
alter table public.story_views enable row level security;
alter table public.call_sessions enable row level security;
alter table public.client_operations enable row level security;

create policy "profile readable to authenticated" on public.profiles for select to authenticated using (true);
create policy "profile self update" on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid());
create policy "members read conversations" on public.conversations for select to authenticated using (public.is_conversation_member(id));
create policy "members read memberships" on public.conversation_members for select to authenticated using (public.is_conversation_member(conversation_id));
create policy "members read messages" on public.messages for select to authenticated using (public.is_conversation_member(conversation_id));
create policy "members send messages" on public.messages for insert to authenticated with check (sender_id = auth.uid() and public.is_conversation_member(conversation_id));
create policy "authors manage stories" on public.stories for all to authenticated using (author_id = auth.uid()) with check (author_id = auth.uid());
create policy "owner sees operation" on public.client_operations for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
