-- V1 realtime: delivery receipts, busy presence, extra hint apply types.
-- Socket.IO / supabase-kt Realtime only hint; these RPCs persist.

do $$
declare cname text;
begin
  for cname in
    select con.conname
    from pg_constraint con
    join pg_class rel on rel.oid = con.conrelid
    join pg_namespace nsp on nsp.oid = rel.relnamespace
    where nsp.nspname = 'public' and rel.relname = 'chat_presence' and con.contype = 'c'
      and pg_get_constraintdef(con.oid) ilike '%status%'
  loop
    execute format('alter table public.chat_presence drop constraint if exists %I', cname);
  end loop;
end $$;

alter table public.chat_presence
  add constraint chat_presence_status_check
  check (status in ('online', 'away', 'busy', 'in_call', 'recording', 'offline'));

create or replace function public.mark_chat_delivered(target uuid, message_id uuid)
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
  insert into public.message_receipts (message_id, user_id, state)
  values (message_id, auth.uid(), 'delivered')
  on conflict (message_id, user_id) do update
    set state = case when public.message_receipts.state = 'read' then 'read' else 'delivered' end,
        updated_at = now();
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
  elsif operation_type = 'delivered' then
    perform public.mark_chat_delivered((payload->>'conversationId')::uuid, (payload->>'messageId')::uuid);
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

grant execute on function public.mark_chat_delivered(uuid, uuid) to authenticated;
grant execute on function public.apply_chat_operation(uuid, text, jsonb) to authenticated;
