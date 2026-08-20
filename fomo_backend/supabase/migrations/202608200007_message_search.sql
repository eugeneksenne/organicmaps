-- Local message search uses PostgreSQL trigram matching. No paid search API.

create extension if not exists pg_trgm;

create index if not exists messages_body_trgm_idx
  on public.messages using gin (body gin_trgm_ops)
  where deleted_at is null and body is not null;

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

grant execute on function public.search_chat_messages(text, uuid) to authenticated;
