# FOMO Chats shared logic

Kotlin Multiplatform source of truth for Android and iOS.

```
src/commonMain/kotlin/app/organicmaps/chats/
  ChatModels.kt   data records
  ChatLogic.kt    merge, filters, previews, reactions, demo data
src/androidMain/kotlin/.../ChatTime.android.kt
src/iosMain/kotlin/.../ChatTime.ios.kt
```

Platform apps own HTTP, SQLite/cache, and UI. They must not fork merge/filter rules.

Android talks to Supabase through `ChatRpcClient` (OkHttp PostgREST today) and
`FomoSupabase` (supabase-kt Auth/Realtime/Storage/Functions). See
`fomo_backend/docs/chats_stack.md`. No Stream/Sendbird/Firebase Chat.

Story privacy is `friends` or `private` only. `ChatLogic.normalizeStoryAudience` maps any other
value (including `followers` and `public`) to `friends`.

## RPC contract

Supabase RPCs (JWT + anon key only):

| RPC | Use |
| --- | --- |
| `chat_inbox` | Inbox rows |
| `chat_messages` | History |
| `send_chat_message` / `apply_chat_operation` | Durable writes |
| `search_chat_profiles` / `search_chat_messages` | People + message search |
| `open_direct_conversation` / `create_group_conversation` | Threads |
| `mark_chat_read` / `set_conversation_pin` / `set_conversation_mute` | Member state |
| `edit_chat_message` / `delete_chat_message` / `toggle_message_reaction` | Message actions |
| `touch_chat_presence` / `conversation_presence` | Online / last seen |
| `block_chat_user` / `report_chat_content` / `leave_conversation` / `clear_chat_history` | Safety |
| `inbox_stories` / `publish_story` | Stories (audience `friends` or `private`) |

`apply_chat_operation` accepts `message`, `receipt`, `delivered`, `reaction`, `edit`, `delete`,
`mute`, `pin`, `presence`, `leave`, `clear`, `block`, `report`.
