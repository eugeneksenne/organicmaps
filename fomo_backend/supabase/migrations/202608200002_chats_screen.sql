-- Chats screen: presence, mute/leave/block/clear, message search,
-- delivery state, reply previews, and a complete outbox apply path.

alter table public.conversation_members
  add column if not exists hidden_before timestamptz;

create table if not exists public.chat_presence (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  conversation_id uuid references public.conversations(id) on delete set null,
  status text not null default 'online' check (status in ('online', 'away', 'in_call', 'recording')),
  last_seen_at timestamptz not null default now()
);

create table if not exists public.chat_reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_user_id uuid references public.profiles(id) on delete set null,
  conversation_id uuid references public.conversations(id) on delete set null,
  message_id uuid references public.messages(id) on delete set null,
  reason text not null check (char_length(reason) between 1 and 280),
  created_at timestamptz not null default now()
);

alter table public.chat_presence enable row level security;
alter table public.chat_reports enable row level security;

drop policy if exists "members read presence" on public.chat_presence;
create policy "members read presence" on public.chat_presence for select to authenticated
  using (
    user_id = auth.uid()
    or conversation_id is not null and public.is_conversation_member(conversation_id)
  );

drop policy if exists "users write own presence" on public.chat_presence;
create policy "users write own presence" on public.chat_presence for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "users insert own reports" on public.chat_reports;
create policy "users insert own reports" on public.chat_reports for insert to authenticated
  with check (reporter_id = auth.uid());
drop policy if exists "users read own reports" on public.chat_reports;
create policy "users read own reports" on public.chat_reports for select to authenticated
  using (reporter_id = auth.uid());

create or replace function public.touch_chat_presence(target uuid default null, p_status text default 'online')
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  insert into public.chat_presence (user_id, conversation_id, status, last_seen_at)
  values (auth.uid(), target, coalesce(nullif(p_status, ''), 'online'), now())
  on conflict (user_id) do update
    set conversation_id = excluded.conversation_id,
        status = excluded.status,
        last_seen_at = now();
end;
$$;

create or replace function public.conversation_presence(target uuid)
returns table (
  user_id uuid,
  display_name text,
  status text,
  last_seen_at timestamptz,
  online boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select p.id,
         p.display_name,
         coalesce(pr.status, 'away'),
         pr.last_seen_at,
         pr.last_seen_at is not null and pr.last_seen_at > now() - interval '45 seconds'
  from public.conversation_members m
  join public.profiles p on p.id = m.user_id
  left join public.chat_presence pr on pr.user_id = m.user_id
  where public.is_conversation_member(target)
    and m.conversation_id = target
    and m.left_at is null
    and m.user_id <> auth.uid();
$$;

create or replace function public.set_conversation_mute(target uuid, muted boolean)
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
     set muted_until = case when muted then now() + interval '10 years' else null end
   where conversation_id = target and user_id = auth.uid();
end;
$$;

create or replace function public.leave_conversation(target uuid)
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
     set left_at = now()
   where conversation_id = target and user_id = auth.uid() and left_at is null;
end;
$$;

create or replace function public.clear_chat_history(target uuid)
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
     set hidden_before = now(), last_read_message_id = null
   where conversation_id = target and user_id = auth.uid();
end;
$$;

create or replace function public.block_chat_user(other_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null or other_user = auth.uid() then
    raise exception 'invalid_peer';
  end if;
  insert into public.user_blocks (blocker_id, blocked_id)
  values (auth.uid(), other_user)
  on conflict do nothing;
  update public.conversation_members me
     set left_at = now()
   where me.user_id = auth.uid()
     and me.left_at is null
     and exists (
       select 1
       from public.conversations c
       join public.conversation_members peer
         on peer.conversation_id = c.id and peer.user_id = other_user and peer.left_at is null
       where c.id = me.conversation_id and c.kind = 'direct'
     );
end;
$$;

create or replace function public.report_chat_content(
  target_user uuid default null,
  target_conversation uuid default null,
  target_message uuid default null,
  reason text default 'abuse'
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  insert into public.chat_reports (reporter_id, target_user_id, conversation_id, message_id, reason)
  values (auth.uid(), target_user, target_conversation, target_message, left(coalesce(reason, 'abuse'), 280));
end;
$$;

create or replace function public.search_chat_messages(query text, target uuid default null)
returns table (
  id uuid,
  conversation_id uuid,
  conversation_title text,
  sender_name text,
  body text,
  kind public.message_kind,
  sent_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  select m.id,
         m.conversation_id,
         coalesce(c.title, peer.display_name, 'Chat'),
         p.display_name,
         m.body,
         m.kind,
         m.sent_at
  from public.messages m
  join public.conversation_members me
    on me.conversation_id = m.conversation_id and me.user_id = auth.uid() and me.left_at is null
  join public.conversations c on c.id = m.conversation_id
  join public.profiles p on p.id = m.sender_id
  left join lateral (
    select pr.display_name
    from public.conversation_members other
    join public.profiles pr on pr.id = other.user_id
    where other.conversation_id = c.id and other.user_id <> auth.uid() and other.left_at is null
    limit 1
  ) peer on true
  where m.deleted_at is null
    and m.body is not null
    and char_length(btrim(coalesce(query, ''))) >= 2
    and m.body ilike '%' || replace(replace(btrim(query), '%', ''), '_', '') || '%'
    and (target is null or m.conversation_id = target)
    and (me.hidden_before is null or m.sent_at > me.hidden_before)
  order by m.sent_at desc
  limit 50;
$$;

drop function if exists public.chat_inbox();
create function public.chat_inbox()
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
  muted boolean,
  peer_online boolean,
  peer_last_seen timestamptz
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
             and (me.hidden_before is null or m.sent_at > me.hidden_before)
             and (
               me.last_read_message_id is null
               or m.sent_at > (select sent_at from public.messages r where r.id = me.last_read_message_id)
             )
         ),
         me.pinned_at is not null,
         me.muted_until is not null and me.muted_until > now(),
         coalesce(peer.last_seen_at > now() - interval '45 seconds', false),
         peer.last_seen_at
  from public.conversation_members me
  join public.conversations c on c.id = me.conversation_id
  left join lateral (
    select p.id, p.username, p.display_name, p.is_verified, pr.last_seen_at
    from public.conversation_members other
    join public.profiles p on p.id = other.user_id
    left join public.chat_presence pr on pr.user_id = p.id
    where other.conversation_id = c.id and other.user_id <> auth.uid() and other.left_at is null
    order by p.display_name
    limit 1
  ) peer on true
  left join lateral (
    select m.* from public.messages m
    where m.conversation_id = c.id
      and (me.hidden_before is null or m.sent_at > me.hidden_before)
    order by m.sent_at desc
    limit 1
  ) last on true
  where me.user_id = auth.uid() and me.left_at is null
  order by me.pinned_at desc nulls last, coalesce(last.sent_at, c.updated_at) desc;
$$;

drop function if exists public.chat_messages(uuid, timestamptz, integer);
create function public.chat_messages(target uuid, before timestamptz default null, page_size integer default 50)
returns table (
  id uuid,
  conversation_id uuid,
  sender_id uuid,
  sender_name text,
  kind public.message_kind,
  body text,
  client_operation_id uuid,
  reply_to_id uuid,
  reply_preview text,
  reply_sender text,
  sent_at timestamptz,
  edited_at timestamptz,
  deleted_at timestamptz,
  reactions jsonb,
  state text
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
         case when quoted.deleted_at is null then left(quoted.body, 80) else null end,
         quoted_profile.display_name,
         m.sent_at,
         m.edited_at,
         m.deleted_at,
         coalesce((
           select jsonb_agg(jsonb_build_object('emoji', r.emoji, 'user_id', r.user_id))
           from public.message_reactions r where r.message_id = m.id
         ), '[]'::jsonb),
         case
           when m.deleted_at is not null then 'deleted'
           when exists (
             select 1 from public.message_receipts r
             where r.message_id = m.id and r.state = 'read' and r.user_id <> m.sender_id
           ) then 'read'
           when exists (
             select 1 from public.message_receipts r
             where r.message_id = m.id and r.state = 'delivered' and r.user_id <> m.sender_id
           ) then 'delivered'
           else 'sent'
         end
  from public.messages m
  join public.profiles p on p.id = m.sender_id
  left join public.messages quoted on quoted.id = m.reply_to_id
  left join public.profiles quoted_profile on quoted_profile.id = quoted.sender_id
  where public.is_conversation_member(target)
    and m.conversation_id = target
    and (before is null or m.sent_at < before)
    and m.sent_at > coalesce((
      select hidden_before from public.conversation_members
      where conversation_id = target and user_id = auth.uid()
    ), '-infinity'::timestamptz)
  order by m.sent_at desc
  limit greatest(1, least(coalesce(page_size, 50), 100));
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
  elsif operation_type = 'edit' then
    perform public.edit_chat_message((payload->>'messageId')::uuid, payload->>'body');
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'delete' then
    perform public.delete_chat_message((payload->>'messageId')::uuid);
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'mute' then
    perform public.set_conversation_mute((payload->>'conversationId')::uuid, coalesce((payload->>'muted')::boolean, true));
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'pin' then
    perform public.set_conversation_pin((payload->>'conversationId')::uuid, coalesce((payload->>'pinned')::boolean, true));
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'presence' then
    perform public.touch_chat_presence(nullif(payload->>'conversationId', '')::uuid, coalesce(payload->>'status', 'online'));
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'leave' then
    perform public.leave_conversation((payload->>'conversationId')::uuid);
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'clear' then
    perform public.clear_chat_history((payload->>'conversationId')::uuid);
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'block' then
    perform public.block_chat_user((payload->>'userId')::uuid);
    result := jsonb_build_object('ok', true);
  elsif operation_type = 'report' then
    perform public.report_chat_content(
      nullif(payload->>'userId', '')::uuid,
      nullif(payload->>'conversationId', '')::uuid,
      nullif(payload->>'messageId', '')::uuid,
      coalesce(payload->>'reason', 'abuse')
    );
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

grant execute on function public.touch_chat_presence(uuid, text) to authenticated;
grant execute on function public.conversation_presence(uuid) to authenticated;
grant execute on function public.set_conversation_mute(uuid, boolean) to authenticated;
grant execute on function public.leave_conversation(uuid) to authenticated;
grant execute on function public.clear_chat_history(uuid) to authenticated;
grant execute on function public.block_chat_user(uuid) to authenticated;
grant execute on function public.report_chat_content(uuid, uuid, uuid, text) to authenticated;
grant execute on function public.search_chat_messages(text, uuid) to authenticated;
grant execute on function public.chat_inbox() to authenticated;
grant execute on function public.chat_messages(uuid, timestamptz, integer) to authenticated;
grant execute on function public.apply_chat_operation(uuid, text, jsonb) to authenticated;

do $$
begin
  begin
    alter publication supabase_realtime add table public.chat_presence;
  exception when duplicate_object then null;
  end;
  begin
    alter publication supabase_realtime add table public.message_receipts;
  exception when duplicate_object then null;
  end;
end $$;
