# FOMO Realtime protocol

## Authority boundary

Supabase PostgreSQL/RPC and Storage are the source of truth. Socket.IO and Supabase Realtime are delivery mechanisms only. A client must persist a message, reaction, receipt, or membership change before emitting its associated realtime hint. Receivers always reconcile against the database using a monotonically ordered sync cursor.

## Transport selection

| Data | Primary transport | Socket.IO use |
| --- | --- | --- |
| Messages, edits, deletes, reactions, receipts | Supabase Postgres + Realtime | Optional low-latency hint after persistence |
| Typing, voice-recording, upload state | Socket.IO | Ephemeral, expires after 5 seconds |
| Presence | Supabase Realtime Presence initially | Socket.IO only when scale requires it |
| Call SDP/ICE | Socket.IO | Authenticated signaling only; media uses LiveKit/WebRTC |
| Stories/groups/venue updates | Supabase Realtime | Optional UI hint |

## Socket.IO events

All connections send a Supabase access token in `socket.auth.accessToken`. The VPS verifies the JWT against Supabase JWKS. A user must be an active conversation member before entering a room or sending a conversation event.

### Client to server

- `conversation:join { conversationId }`
- `conversation:leave { conversationId }`
- `typing:update { conversationId, isTyping }`
- `ephemeral:publish { conversationId, type, payload }`
  - Allowed types: `voice_recording`, `media_uploading`, `story_viewing`, `call_ringing`, `call_busy`.
- `message:published { conversationId, message }` — message must already be persisted.
- `receipt:hint { conversationId, messageId, state }` — receipt must already be persisted.
- `call:signal { conversationId, targetUserId, signal }`

### Server to client

- `typing:update { userId, isTyping, expiresInMs }`
- `ephemeral:update { userId, type, payload }`
- `message:published { ...message }`
- `receipt:hint { userId, messageId, state }`
- `call:signal { fromUserId, conversationId, signal }`

## Android engine requirements

The Android client must expose a connection state Flow/state machine: `offline`, `connecting`, `connected`, `reconnecting`, `background`, `failed`. It must use exponential backoff with jitter, pause ephemeral traffic in background, and re-sync database state after every reconnect. Never treat a socket event as proof of persistence.
