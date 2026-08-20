# FOMO Chats backend

Supabase PostgreSQL is the source of truth for conversations, messages,
membership, receipts, and reactions. Socket.IO only fans out ephemeral hints
after a durable write.

## Client contract

Android may only send the public Supabase URL and anonymous key. Every chat
RPC uses the signed-in user's JWT. The service-role key never ships in the app.

### Auth

`handle_new_user` creates a `profiles` row after `auth.users` insert.
`ensure_own_profile` backfills accounts created before the trigger.

### RPCs

| Function | Purpose |
| --- | --- |
| `chat_inbox()` | Pinned + recent conversations with unread counts |
| `chat_messages(target, before, page_size)` | Paginated history |
| `send_chat_message(...)` | Idempotent send via `client_operation_id` |
| `open_direct_conversation(other_user)` | Find or create a 1:1 thread |
| `create_group_conversation(title, member_ids)` | Owner + invited members |
| `mark_chat_read` / `set_conversation_pin` | Per-member state |
| `search_chat_profiles` | People picker, excluding blocks |
| `inbox_stories` | Unexpired stories the user may see |
| `apply_chat_operation` | Offline outbox apply path |

`body` is transport plaintext until a reviewed E2EE protocol is selected.
`ciphertext` currently stores the UTF-8 bytes of `body`.
