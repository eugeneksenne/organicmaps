-- Call history and start/end RPCs used by the Android/iOS call screens.

drop policy if exists "members read calls" on public.call_sessions;
create policy "members read calls" on public.call_sessions for select to authenticated
  using (public.is_conversation_member(conversation_id));

drop policy if exists "members insert calls" on public.call_sessions;
create policy "members insert calls" on public.call_sessions for insert to authenticated
  with check (initiated_by = auth.uid() and public.is_conversation_member(conversation_id));

drop policy if exists "members update calls" on public.call_sessions;
create policy "members update calls" on public.call_sessions for update to authenticated
  using (public.is_conversation_member(conversation_id));

create or replace function public.start_chat_call(target uuid, kind public.call_kind)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  created uuid;
begin
  if auth.uid() is null then
    raise exception 'unauthorized';
  end if;
  if not public.is_conversation_member(target) then
    raise exception 'forbidden';
  end if;
  insert into public.call_sessions (conversation_id, initiated_by, kind, status)
  values (target, auth.uid(), kind, 'ringing')
  returning id into created;
  return created;
end;
$$;

create or replace function public.end_chat_call(target uuid, reason text default 'ended')
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_conversation_member(target) then
    raise exception 'forbidden';
  end if;
  update public.call_sessions
     set status = case when status = 'ringing' then 'cancelled' else 'ended' end,
         ended_at = now(),
         end_reason = left(coalesce(reason, 'ended'), 40)
   where conversation_id = target
     and ended_at is null
     and initiated_by = auth.uid();
end;
$$;

create or replace function public.call_inbox()
returns table (
  id uuid,
  conversation_id uuid,
  title text,
  kind public.call_kind,
  direction text,
  status public.call_status,
  started_at timestamptz,
  duration_sec integer
)
language sql
stable
security definer
set search_path = public
as $$
  select s.id,
         s.conversation_id,
         coalesce(c.title, peer.display_name, 'Call'),
         s.kind,
         case when s.initiated_by = auth.uid() then 'outgoing' else 'incoming' end,
         s.status,
         s.started_at,
         greatest(0, floor(extract(epoch from (coalesce(s.ended_at, now()) - coalesce(s.answered_at, s.started_at))))::int)
  from public.call_sessions s
  join public.conversations c on c.id = s.conversation_id
  join public.conversation_members me
    on me.conversation_id = s.conversation_id and me.user_id = auth.uid() and me.left_at is null
  left join lateral (
    select p.display_name
    from public.conversation_members other
    join public.profiles p on p.id = other.user_id
    where other.conversation_id = c.id and other.user_id <> auth.uid() and other.left_at is null
    limit 1
  ) peer on true
  order by s.started_at desc
  limit 80;
$$;

grant execute on function public.start_chat_call(uuid, public.call_kind) to authenticated;
grant execute on function public.end_chat_call(uuid, text) to authenticated;
grant execute on function public.call_inbox() to authenticated;
