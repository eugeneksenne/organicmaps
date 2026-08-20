-- Chats engine: readable message body until a vetted E2EE protocol is selected,
-- authorization RPCs, profile bootstrap, receipts, reactions, and inbox queries.

alter table public.messages
  add column if not exists body text;

alter table public.messages
  alter column ciphertext set default '\x'::bytea;

alter table public.conversation_members
  add column if not exists pinned_at timestamptz;

create table if not exists public.message_reactions (
  message_id uuid not null references public.messages(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  emoji text not null check (char_length(emoji) between 1 and 16),
  created_at timestamptz not null default now(),
  primary key (message_id, user_id, emoji)
);

create table if not exists public.message_receipts (
  message_id uuid not null references public.messages(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  state text not null check (state in ('delivered', 'read')),
  updated_at timestamptz not null default now(),
  primary key (message_id, user_id)
);

alter table public.message_reactions enable row level security;
alter table public.message_receipts enable row level security;

create policy "members read reactions" on public.message_reactions for select to authenticated
  using (exists (
    select 1 from public.messages m
    where m.id = message_id and public.is_conversation_member(m.conversation_id)
  ));
create policy "members write own reactions" on public.message_reactions for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "members read receipts" on public.message_receipts for select to authenticated
  using (exists (
    select 1 from public.messages m
    where m.id = message_id and public.is_conversation_member(m.conversation_id)
  ));
create policy "members write own receipts" on public.message_receipts for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "authors edit messages" on public.messages;
create policy "authors edit messages" on public.messages for update to authenticated
  using (sender_id = auth.uid()) with check (sender_id = auth.uid());

drop policy if exists "members update self membership" on public.conversation_members;
create policy "members update self membership" on public.conversation_members for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "followers read stories" on public.stories;
create policy "followers read stories" on public.stories for select to authenticated
  using (
    deleted_at is null
    and expires_at > now()
    and (
      author_id = auth.uid()
      or audience = 'public'
      or (audience = 'followers' and exists (
        select 1 from public.user_follows f where f.followee_id = author_id and f.follower_id = auth.uid()
      ))
      or (audience = 'friends' and exists (
        select 1 from public.user_follows a
        join public.user_follows b on a.follower_id = b.followee_id and a.followee_id = b.follower_id
        where a.follower_id = auth.uid() and a.followee_id = author_id
      ))
    )
  );

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  uname text;
  dname text;
begin
  uname := lower(coalesce(new.raw_user_meta_data->>'username', split_part(coalesce(new.email, 'user'), '@', 1)));
  uname := regexp_replace(uname, '[^a-z0-9_.]', '', 'g');
  if char_length(uname) < 3 then
    uname := 'user' || substr(replace(new.id::text, '-', ''), 1, 8);
  end if;
  uname := left(uname, 32);
  while exists(select 1 from public.profiles where username = uname) loop
    uname := left(uname, 24) || substr(replace(gen_random_uuid()::text, '-', ''), 1, 8);
  end loop;
  dname := coalesce(nullif(new.raw_user_meta_data->>'display_name', ''), initcap(uname));
  insert into public.profiles (id, username, display_name)
  values (new.id, uname, left(dname, 80))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

create or replace function public.ensure_own_profile(p_username text default null, p_display_name text default null)
returns public.profiles
language plpgsql
security definer
set search_path = public
as $$
declare
  result public.profiles;
  uname text;
  dname text;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  select * into result from public.profiles where id = auth.uid();
  if found then
    return result;
  end if;
  uname := lower(coalesce(nullif(p_username, ''), 'user' || substr(replace(auth.uid()::text, '-', ''), 1, 8)));
  uname := left(regexp_replace(uname, '[^a-z0-9_.]', '', 'g'), 32);
  if char_length(uname) < 3 then
    uname := 'user' || substr(replace(auth.uid()::text, '-', ''), 1, 8);
  end if;
  while exists(select 1 from public.profiles where username = uname) loop
    uname := left(uname, 24) || substr(replace(gen_random_uuid()::text, '-', ''), 1, 8);
  end loop;
  dname := left(coalesce(nullif(p_display_name, ''), initcap(uname)), 80);
  insert into public.profiles (id, username, display_name)
  values (auth.uid(), uname, dname)
  returning * into result;
  return result;
end;
$$;

create or replace function public.open_direct_conversation(other_user uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  existing uuid;
  created uuid;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if other_user = auth.uid() then
    raise exception 'invalid_peer';
  end if;
  if not exists(select 1 from public.profiles where id = other_user) then
    raise exception 'unknown_user';
  end if;
  if exists (
    select 1 from public.user_blocks
    where (blocker_id = auth.uid() and blocked_id = other_user)
       or (blocker_id = other_user and blocked_id = auth.uid())
  ) then
    raise exception 'blocked';
  end if;
  select cm1.conversation_id into existing
  from public.conversation_members cm1
  join public.conversation_members cm2 on cm1.conversation_id = cm2.conversation_id
  join public.conversations c on c.id = cm1.conversation_id
  where c.kind = 'direct'
    and cm1.user_id = auth.uid() and cm1.left_at is null
    and cm2.user_id = other_user and cm2.left_at is null
    and (select count(*) from public.conversation_members m
         where m.conversation_id = c.id and m.left_at is null) = 2
  limit 1;
  if existing is not null then
    return existing;
  end if;
  insert into public.conversations (kind, created_by)
  values ('direct', auth.uid())
  returning id into created;
  insert into public.conversation_members (conversation_id, user_id, role)
  values (created, auth.uid(), 'owner'), (created, other_user, 'member');
  return created;
end;
$$;

create or replace function public.create_group_conversation(p_title text, member_ids uuid[] default '{}')
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  created uuid;
  member uuid;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if p_title is null or char_length(btrim(p_title)) < 1 or char_length(p_title) > 80 then
    raise exception 'invalid_title';
  end if;
  insert into public.conversations (kind, title, created_by)
  values ('group', btrim(p_title), auth.uid())
  returning id into created;
  insert into public.conversation_members (conversation_id, user_id, role)
  values (created, auth.uid(), 'owner');
  foreach member in array coalesce(member_ids, '{}') loop
    if member <> auth.uid() and exists(select 1 from public.profiles where id = member)
       and not exists (
         select 1 from public.user_blocks
         where (blocker_id = auth.uid() and blocked_id = member)
            or (blocker_id = member and blocked_id = auth.uid())
       ) then
      insert into public.conversation_members (conversation_id, user_id, role)
      values (created, member, 'member')
      on conflict do nothing;
    end if;
  end loop;
  return created;
end;
$$;

create or replace function public.send_chat_message(
  target uuid,
  operation_id uuid,
  kind public.message_kind default 'text',
  body text default '',
  reply_to uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  stored public.messages;
  clean text;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  perform public.ensure_own_profile();
  if not public.is_conversation_member(target) then
    raise exception 'forbidden';
  end if;
  clean := left(coalesce(body, ''), 4000);
  if kind = 'text' and char_length(btrim(clean)) = 0 then
    raise exception 'empty_message';
  end if;
  if (select count(*) from public.messages
      where sender_id = auth.uid() and sent_at > now() - interval '1 minute') >= 30 then
    raise exception 'rate_limited';
  end if;
  if exists (
    select 1 from public.conversations c
    join public.conversation_members peer on peer.conversation_id = c.id and peer.user_id <> auth.uid() and peer.left_at is null
    join public.user_blocks b on (b.blocker_id = peer.user_id and b.blocked_id = auth.uid())
                             or (b.blocker_id = auth.uid() and b.blocked_id = peer.user_id)
    where c.id = target and c.kind = 'direct'
  ) then
    raise exception 'blocked';
  end if;
  if reply_to is not null and not exists (
    select 1 from public.messages m where m.id = reply_to and m.conversation_id = target
  ) then
    raise exception 'invalid_reply';
  end if;

  insert into public.messages (conversation_id, sender_id, kind, ciphertext, body, client_operation_id, reply_to_id)
  values (target, auth.uid(), kind, convert_to(clean, 'UTF8'), clean, operation_id, reply_to)
  on conflict (sender_id, client_operation_id) do nothing;

  select * into stored
  from public.messages
  where sender_id = auth.uid() and client_operation_id = operation_id;

  update public.conversations set updated_at = now() where id = target;
  insert into public.client_operations (id, user_id, operation_type, payload, status, completed_at)
  values (operation_id, auth.uid(), 'message',
          jsonb_build_object('conversationId', target, 'kind', kind, 'body', clean),
          'completed', now())
  on conflict (id) do update
    set status = 'completed', completed_at = now();

  return jsonb_build_object(
    'id', stored.id,
    'conversation_id', stored.conversation_id,
    'sender_id', stored.sender_id,
    'kind', stored.kind,
    'body', stored.body,
    'client_operation_id', stored.client_operation_id,
    'reply_to_id', stored.reply_to_id,
    'sent_at', stored.sent_at,
    'edited_at', stored.edited_at,
    'deleted_at', stored.deleted_at,
    'state', 'sent'
  );
end;
$$;

create or replace function public.chat_inbox()
returns table (
  conversation_id uuid,
  kind public.conversation_kind,
  title text,
  image_path text,
  peer_id uuid,
  peer_username text,
  peer_display_name text,
  peer_verified boolean,
  last_message_id uuid,
  last_message_body text,
  last_message_kind public.message_kind,
  last_message_at timestamptz,
  last_sender_id uuid,
  unread_count bigint,
  pinned boolean,
  muted boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select c.id,
         c.kind,
         coalesce(c.title, peer.display_name, 'Chat'),
         c.image_path,
         peer.id,
         peer.username,
         peer.display_name,
         coalesce(peer.is_verified, false),
         last.id,
         case when last.deleted_at is null then last.body else null end,
         last.kind,
         coalesce(last.sent_at, c.updated_at),
         last.sender_id,
         (
           select count(*) from public.messages m
           where m.conversation_id = c.id
             and m.deleted_at is null
             and m.sender_id <> auth.uid()
             and (
               me.last_read_message_id is null
               or m.sent_at > (select sent_at from public.messages r where r.id = me.last_read_message_id)
             )
         ),
         me.pinned_at is not null,
         me.muted_until is not null and me.muted_until > now()
  from public.conversation_members me
  join public.conversations c on c.id = me.conversation_id
  left join lateral (
    select p.id, p.username, p.display_name, p.is_verified
    from public.conversation_members other
    join public.profiles p on p.id = other.user_id
    where other.conversation_id = c.id and other.user_id <> auth.uid() and other.left_at is null
    order by p.display_name
    limit 1
  ) peer on true
  left join lateral (
    select m.* from public.messages m
    where m.conversation_id = c.id
    order by m.sent_at desc
    limit 1
  ) last on true
  where me.user_id = auth.uid() and me.left_at is null
  order by me.pinned_at desc nulls last, coalesce(last.sent_at, c.updated_at) desc;
$$;

create or replace function public.chat_messages(target uuid, before timestamptz default null, page_size integer default 50)
returns table (
  id uuid,
  conversation_id uuid,
  sender_id uuid,
  sender_name text,
  kind public.message_kind,
  body text,
  client_operation_id uuid,
  reply_to_id uuid,
  sent_at timestamptz,
  edited_at timestamptz,
  deleted_at timestamptz,
  reactions jsonb
)
language sql
stable
security definer
set search_path = public
as $$
  select m.id,
         m.conversation_id,
         m.sender_id,
         p.display_name,
         m.kind,
         case when m.deleted_at is null then m.body else null end,
         m.client_operation_id,
         m.reply_to_id,
         m.sent_at,
         m.edited_at,
         m.deleted_at,
         coalesce((
           select jsonb_agg(jsonb_build_object('emoji', r.emoji, 'user_id', r.user_id))
           from public.message_reactions r where r.message_id = m.id
         ), '[]'::jsonb)
  from public.messages m
  join public.profiles p on p.id = m.sender_id
  where public.is_conversation_member(target)
    and m.conversation_id = target
    and (before is null or m.sent_at < before)
  order by m.sent_at desc
  limit greatest(1, least(coalesce(page_size, 50), 100));
$$;

create or replace function public.mark_chat_read(target uuid, message_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_conversation_member(target) then
    raise exception 'forbidden';
  end if;
  if not exists(select 1 from public.messages where id = message_id and conversation_id = target) then
    raise exception 'unknown_message';
  end if;
  update public.conversation_members
     set last_read_message_id = message_id
   where conversation_id = target and user_id = auth.uid();
  insert into public.message_receipts (message_id, user_id, state)
  values (message_id, auth.uid(), 'read')
  on conflict (message_id, user_id) do update
    set state = 'read', updated_at = now();
end;
$$;

create or replace function public.set_conversation_pin(target uuid, pinned boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_conversation_member(target) then
    raise exception 'forbidden';
  end if;
  update public.conversation_members
     set pinned_at = case when pinned then now() else null end
   where conversation_id = target and user_id = auth.uid();
end;
$$;

create or replace function public.search_chat_profiles(query text)
returns table (
  id uuid,
  username text,
  display_name text,
  is_verified boolean,
  avatar_path text
)
language sql
stable
security definer
set search_path = public
as $$
  select p.id, p.username, p.display_name, p.is_verified, p.avatar_path
  from public.profiles p
  where auth.uid() is not null
    and p.id <> auth.uid()
    and (
      p.username ilike '%' || replace(replace(coalesce(query, ''), '%', ''), '_', '') || '%'
      or p.display_name ilike '%' || replace(replace(coalesce(query, ''), '%', ''), '_', '') || '%'
    )
    and not exists (
      select 1 from public.user_blocks b
      where (b.blocker_id = auth.uid() and b.blocked_id = p.id)
         or (b.blocker_id = p.id and b.blocked_id = auth.uid())
    )
  order by p.display_name
  limit 20;
$$;

create or replace function public.inbox_stories()
returns table (
  id uuid,
  author_id uuid,
  author_name text,
  author_username text,
  author_verified boolean,
  expires_at timestamptz,
  created_at timestamptz,
  viewed boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select s.id, s.author_id, p.display_name, p.username, p.is_verified, s.expires_at, s.created_at,
         exists(select 1 from public.story_views v where v.story_id = s.id and v.viewer_id = auth.uid())
  from public.stories s
  join public.profiles p on p.id = s.author_id
  where s.deleted_at is null and s.expires_at > now()
    and (
      s.author_id = auth.uid()
      or s.audience = 'public'
      or (s.audience = 'followers' and exists (
        select 1 from public.user_follows f where f.followee_id = s.author_id and f.follower_id = auth.uid()
      ))
      or (s.audience = 'friends' and exists (
        select 1 from public.user_follows a
        join public.user_follows b on a.follower_id = b.followee_id and a.followee_id = b.follower_id
        where a.follower_id = auth.uid() and a.followee_id = s.author_id
      ))
    )
  order by s.created_at desc
  limit 40;
$$;

create or replace function public.edit_chat_message(message_id uuid, new_body text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.messages
     set body = left(coalesce(new_body, ''), 4000),
         ciphertext = convert_to(left(coalesce(new_body, ''), 4000), 'UTF8'),
         edited_at = now()
   where id = message_id
     and sender_id = auth.uid()
     and deleted_at is null
     and sent_at > now() - interval '15 minutes';
  if not found then
    raise exception 'cannot_edit';
  end if;
end;
$$;

create or replace function public.delete_chat_message(message_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  update public.messages
     set deleted_at = now(), body = null
   where id = message_id and sender_id = auth.uid() and deleted_at is null;
  if not found then
    raise exception 'cannot_delete';
  end if;
end;
$$;

create or replace function public.toggle_message_reaction(message_id uuid, emoji text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  conv uuid;
begin
  select conversation_id into conv from public.messages where id = message_id;
  if conv is null or not public.is_conversation_member(conv) then
    raise exception 'forbidden';
  end if;
  if exists (
    select 1 from public.message_reactions r
    where r.message_id = toggle_message_reaction.message_id
      and r.user_id = auth.uid()
      and r.emoji = toggle_message_reaction.emoji
  ) then
    delete from public.message_reactions r
    where r.message_id = toggle_message_reaction.message_id
      and r.user_id = auth.uid()
      and r.emoji = toggle_message_reaction.emoji;
  else
    insert into public.message_reactions (message_id, user_id, emoji)
    values (message_id, auth.uid(), left(emoji, 16));
  end if;
end;
$$;

create or replace function public.apply_chat_operation(operation_id uuid, operation_type text, payload jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  result jsonb;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if operation_type = 'message' then
    result := public.send_chat_message(
      (payload->>'conversationId')::uuid,
      operation_id,
      coalesce((payload->>'kind')::public.message_kind, 'text'),
      payload->>'body',
      nullif(payload->>'replyToId', '')::uuid
    );
  elsif operation_type = 'receipt' then
    perform public.mark_chat_read((payload->>'conversationId')::uuid, (payload->>'messageId')::uuid);
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'reaction' then
    perform public.toggle_message_reaction((payload->>'messageId')::uuid, payload->>'emoji');
    result := jsonb_build_object('ok', true);
  else
    insert into public.client_operations (id, user_id, operation_type, payload, status)
    values (operation_id, auth.uid(), operation_type, coalesce(payload, '{}'::jsonb), 'queued')
    on conflict (id) do nothing;
    result := jsonb_build_object('queued', true);
  end if;
  return result;
end;
$$;

grant execute on function public.ensure_own_profile(text, text) to authenticated;
grant execute on function public.open_direct_conversation(uuid) to authenticated;
grant execute on function public.create_group_conversation(text, uuid[]) to authenticated;
grant execute on function public.send_chat_message(uuid, uuid, public.message_kind, text, uuid) to authenticated;
grant execute on function public.chat_inbox() to authenticated;
grant execute on function public.chat_messages(uuid, timestamptz, integer) to authenticated;
grant execute on function public.mark_chat_read(uuid, uuid) to authenticated;
grant execute on function public.set_conversation_pin(uuid, boolean) to authenticated;
grant execute on function public.search_chat_profiles(text) to authenticated;
grant execute on function public.inbox_stories() to authenticated;
grant execute on function public.edit_chat_message(uuid, text) to authenticated;
grant execute on function public.delete_chat_message(uuid) to authenticated;
grant execute on function public.toggle_message_reaction(uuid, text) to authenticated;
grant execute on function public.apply_chat_operation(uuid, text, jsonb) to authenticated;

do $$
begin
  begin
    alter publication supabase_realtime add table public.messages;
  exception when duplicate_object then null;
  end;
  begin
    alter publication supabase_realtime add table public.conversation_members;
  exception when duplicate_object then null;
  end;
  begin
    alter publication supabase_realtime add table public.message_reactions;
  exception when duplicate_object then null;
  end;
end $$;
