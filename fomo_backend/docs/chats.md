# FOMO Chats backend

The open-source client stack is documented in `chats_stack.md`. There is no paid
chat, video, or search API.

Supabase PostgreSQL is the source of truth for conversations, messages,
membership, receipts, and reactions. Socket.IO only fans out ephemeral hints
after a durable write.

Shared client rules live in `shared/chats` (Kotlin) and are ported to iOS in
`iphone/Maps/UI/Chats/ChatLogic.swift`.

## Client contract

Android and iOS may only send the public Supabase URL and anonymous key. Every
chat RPC uses the signed-in user's JWT. The service-role key never ships in the app.

### Auth

`handle_new_user` creates a `profiles` row after `auth.users` insert.
`ensure_own_profile` backfills accounts created before the trigger.

### RPCs

| Function | Purpose |
| --- | --- |
| `chat_inbox()` | Pinned + recent conversations with unread, mute, presence |
| `chat_messages(target, before, page_size)` | Paginated history with replies, reactions, delivery state |
| `send_chat_message(...)` | Idempotent send via `client_operation_id` |
| `open_direct_conversation(other_user)` | Find or create a 1:1 thread |
| `create_group_conversation(title, member_ids)` | Owner + invited members |
| `mark_chat_read` / `set_conversation_pin` / `set_conversation_mute` | Per-member state |
| `search_chat_profiles` / `search_chat_messages` | People picker and message search |
| `inbox_stories` | Unexpired stories the user may see (own + friends) |
| `publish_story` / `mark_story_viewed` / `react_to_story` / `delete_own_story` / `mute_story_author` | Story lifecycle |
| `edit_chat_message` / `delete_chat_message` / `toggle_message_reaction` | Message actions |
| `touch_chat_presence` / `conversation_presence` | Online / last seen |
| `leave_conversation` / `clear_chat_history` | Membership and delete-for-me |
| `block_chat_user` / `report_chat_content` | Safety |
| `call_inbox` / `start_chat_call` / `end_chat_call` | Call history and session lifecycle |
| `apply_chat_operation` | Offline outbox apply path |

`apply_chat_operation` accepts `message`, `receipt`, `delivered`, `reaction`, `edit`, `delete`,
`mute`, `pin`, `presence`, `leave`, `clear`, `block`, and `report`.

Realtime behaviour is in `chats_realtime.md`.

Stories accept audience `friends` (mutual follows) or `private` (only me). Followers and
public/everyone are rejected by the client and coerced to `friends` on publish.

Contact cards use `message_kind = contact` with a display body of name, phone, and email.
The client never bulk-uploads the device address book; only a contact the user picks is sent.

`body` is transport plaintext until a reviewed E2EE protocol is selected.
`ciphertext` currently stores the UTF-8 bytes of `body`.
