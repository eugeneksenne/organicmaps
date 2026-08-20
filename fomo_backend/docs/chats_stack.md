# FOMO Chats — open-source library stack

FOMO Chats does **not** use Stream, Sendbird, Twilio Chat, Pusher, Ably, Firebase
Realtime/Storage, Algolia, Cloudinary, Mapbox, Agora, or Twilio Video.

Clients send only the public Supabase URL, the anonymous key, and the user JWT.

| Area | Library | FOMO use |
| --- | --- | --- |
| Backend | **Supabase** | Postgres, Auth, Realtime, Storage, Edge Functions, RLS |
| Kotlin client | **supabase-kt** | Auth / PostgREST / Realtime / Storage / Functions modules |
| HTTP | **Ktor OkHttp engine** + **OkHttp** | supabase-kt engine; `ChatRpcClient` PostgREST RPCs |
| Realtime hints | **supabase-kt Realtime** + **Socket.IO** | DB changes later; Socket.IO remains ephemeral (typing, call signal) |
| Async | **Kotlin Coroutines** | Background RPC and outbox |
| Serialization | **kotlinx.serialization** | supabase-kt; chat models stay shared Kotlin data classes |
| Local cache | **SQLite `ChatCache`** | Inbox, messages, drafts, pending outbox. SQLDelight/Room is a later KMP swap |
| Images | **Coil 3** | Not wired until chat image/thumbnail pipeline exists |
| Video/audio | **Media3** | Not wired until voice-note and in-chat video exist |
| Camera | **FOMO Camera / CameraX later** | Chat camera opens the existing FOMO camera screen |
| Maps | **MapLibre Native** | Already the map engine; location/venue cards use it |
| Calls | **LiveKit Android (WebRTC)** | Voice/video rooms; signaling via Socket.IO + `livekit-token` function |
| Encryption | TLS + RLS now | No homemade E2EE. libsodium only after a reviewed protocol |
| Notifications | Platform APIs | Android notifications + APNs; no paid push SaaS |
| Search | **PostgreSQL `pg_trgm`** | `search_chat_messages`; local SQLite LIKE for offline |
| UI | XML + Kotlin binders | Not a paid chat UI SDK. Shared rules in `shared/chats` |
| Testing | JUnit + shared `ChatLogicTest` | |

## Data flow

```
Chat UI (XML binders / UIKit)
        │
        ▼
ChatRepository  ←  ChatLogic (shared Kotlin)
        │
   ┌────┴─────┐
   ▼          ▼
ChatCache   ChatRpcClient → Supabase PostgREST RPCs
(SQLite)         │
                 ▼
         FomoSupabase (supabase-kt)
                 │
          PostgreSQL source of truth
```

Socket.IO never persists messages. `apply_chat_operation` is the durable write.

## What is not in this stack

Paid chat/video/search SDKs. Invented cryptography. Service-role keys in the app.
Bulk address-book upload.
