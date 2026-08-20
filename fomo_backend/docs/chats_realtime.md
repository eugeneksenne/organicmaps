# FOMO Chats — realtime (V1)

Durable state lives in PostgreSQL. Clients never treat a socket ack as a saved
message. `apply_chat_operation` is the write path.

## Live today

| Feature | How |
| --- | --- |
| Optimistic send | Local SQLite row `queued` / `sending`, then server merge |
| Offline queue | `ChatCache.pending_operations` + outbox flush |
| Sent / delivered / read | `message_receipts`; UI ticks `✓` / `✓✓` |
| Typing | Ephemeral Socket.IO `typing:update` (not stored) |
| Presence | `chat_presence` + `touch_chat_presence` (`online`, `busy`, `in_call`, …) |
| Unread | `chat_inbox.unread_count`; inbox refreshes on hints |
| Reactions / edit / delete | Outbox + `message:edited` / `reaction:hint` |
| Replies | `reply_to_id` / `reply_preview` |
| Connection banner | offline / reconnecting / connected |
| Mentions | Parsed in `ChatLogic.extractMentions`; push later |
| Venue / event / location / contact cards | Native `message_kind` bodies |

## Not yet

Push notifications, media upload, voice notes, live location timer, Flash Drop /
Live Moment cards, @everyone notifications, night-plan board, “Who’s out?”,
shared Night Session, AI suggestions. Those are V2/V3.

No Stream/Sendbird. supabase-kt Realtime is initialized for Postgres changes;
Socket.IO still carries typing and call signals.
