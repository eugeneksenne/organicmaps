package app.organicmaps.chat.data

import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Message
import app.organicmaps.chats.ChatModels.Profile
import app.organicmaps.chats.ChatModels.SearchHit
import app.organicmaps.chats.ChatModels.Story
import org.json.JSONArray
import org.json.JSONObject

/** Maps Supabase JSON into shared chat models. */
object ChatJson {
  fun parseInbox(rows: JSONArray): List<Conversation> =
    (0 until rows.length()).mapNotNull { parseConversation(rows.optJSONObject(it)) }

  fun parseConversation(row: JSONObject?): Conversation? {
    if (row == null) return null
    val id = firstNonEmpty(row, "conversation_id", "id")
    if (id.isEmpty()) return null
    val title = firstNonEmpty(row, "title", "peer_display_name").ifEmpty { "Chat" }
    return Conversation(
      id = id,
      kind = row.optString("kind", "direct"),
      title = title,
      peerId = ChatLogic.emptyToNull(row.optString("peer_id")),
      peerUsername = ChatLogic.emptyToNull(row.optString("peer_username")),
      preview = ChatLogic.inboxPreview(
        row.optString("last_message_kind", "text"),
        ChatLogic.emptyToNull(row.optString("last_message_body", row.optString("preview")))
      ).ifEmpty { null },
      lastMessageId = ChatLogic.emptyToNull(row.optString("last_message_id")),
      lastMessageKind = row.optString("last_message_kind", "text"),
      lastMessageAt = ChatLogic.parseTime(firstNonEmpty(row, "last_message_at", "sent_at")),
      unread = row.optInt("unread_count", row.optInt("unread", 0)),
      pinned = row.optBoolean("pinned", false),
      verified = row.optBoolean("peer_verified", row.optBoolean("verified", false)),
      muted = row.optBoolean("muted", false),
      peerOnline = row.optBoolean("peer_online", false),
      peerLastSeen = ChatLogic.parseTime(row.optString("peer_last_seen"))
    )
  }

  fun parseMessages(rows: JSONArray, selfId: String?): List<Message> =
    (rows.length() - 1 downTo 0).mapNotNull { parseMessage(rows.optJSONObject(it), selfId) }

  fun parseMessage(row: JSONObject?, selfId: String?): Message? {
    if (row == null) return null
    val id = firstNonEmpty(row, "id", "client_operation_id", "clientOperationId")
    if (id.isEmpty()) return null
    val senderId = firstNonEmpty(row, "sender_id", "senderId")
    val deleted = !row.isNull("deleted_at") &&
      row.optString("deleted_at").isNotEmpty() &&
      row.optString("deleted_at") != "null"
    val reactionRows = reactionArray(row.opt("reactions"))
    val senderName = firstNonEmpty(row, "sender_name", "senderName").ifEmpty { "You" }
    val state = ChatLogic.emptyToNull(row.optString("state")) ?: if (deleted) "deleted" else "sent"
    return Message(
      id = id,
      conversationId = firstNonEmpty(row, "conversation_id", "conversationId"),
      senderId = senderId,
      senderName = senderName,
      kind = row.optString("kind", "text"),
      body = if (deleted) null else ChatLogic.emptyToNull(row.optString("body")),
      clientOperationId = ChatLogic.emptyToNull(firstNonEmpty(row, "client_operation_id", "clientOperationId")),
      replyToId = ChatLogic.emptyToNull(firstNonEmpty(row, "reply_to_id", "replyToId")),
      replyPreview = ChatLogic.emptyToNull(firstNonEmpty(row, "reply_preview", "replyPreview")),
      sentAt = ChatLogic.parseTime(firstNonEmpty(row, "sent_at", "sentAt")),
      editedAt = ChatLogic.parseTime(firstNonEmpty(row, "edited_at", "editedAt")),
      mine = selfId != null && selfId == senderId,
      deleted = deleted,
      state = state,
      reactions = summarizeReactions(reactionRows, row.optString("reactions")),
      ownReaction = ownReaction(reactionRows, selfId)
        ?: ChatLogic.emptyToNull(firstNonEmpty(row, "own_reaction", "ownReaction"))
    )
  }

  fun parseStories(rows: JSONArray): List<Story> =
    (0 until rows.length()).mapNotNull { index ->
      val row = rows.optJSONObject(index) ?: return@mapNotNull null
      val id = row.optString("id")
      if (id.isEmpty()) return@mapNotNull null
      Story(
        id = id,
        authorName = row.optString("author_name"),
        authorUsername = row.optString("author_username"),
        viewed = row.optBoolean("viewed"),
        verified = row.optBoolean("author_verified"),
        authorId = row.optString("author_id"),
        caption = row.optString("caption"),
        kind = row.optString("kind", "text"),
        audience = ChatLogic.normalizeStoryAudience(row.optString("audience", "friends")),
        createdAt = ChatLogic.parseTime(row.optString("created_at")),
        expiresAt = ChatLogic.parseTime(row.optString("expires_at")),
        mine = row.optBoolean("mine")
      )
    }

  fun parseProfiles(rows: JSONArray): List<Profile> =
    (0 until rows.length()).mapNotNull { index ->
      val row = rows.optJSONObject(index) ?: return@mapNotNull null
      Profile(row.optString("id"), row.optString("username"), row.optString("display_name"), row.optBoolean("is_verified"))
    }

  fun parseSearchHits(rows: JSONArray): List<SearchHit> =
    (0 until rows.length()).mapNotNull { index ->
      val row = rows.optJSONObject(index) ?: return@mapNotNull null
      val id = row.optString("id")
      if (id.isEmpty()) null
      else SearchHit(
        id,
        row.optString("conversation_id"),
        row.optString("conversation_title"),
        row.optString("sender_name"),
        row.optString("body"),
        ChatLogic.parseTime(row.optString("sent_at"))
      )
    }

  fun conversationJson(conversation: Conversation): JSONObject = JSONObject()
    .put("conversation_id", conversation.id)
    .put("kind", conversation.kind)
    .put("title", conversation.title)
    .put("peer_id", conversation.peerId)
    .put("peer_username", conversation.peerUsername)
    .put("last_message_body", conversation.preview)
    .put("last_message_id", conversation.lastMessageId)
    .put("last_message_kind", conversation.lastMessageKind)
    .put("last_message_at", conversation.lastMessageAt)
    .put("unread_count", conversation.unread)
    .put("pinned", conversation.pinned)
    .put("peer_verified", conversation.verified)
    .put("muted", conversation.muted)
    .put("peer_online", conversation.peerOnline)
    .put("peer_last_seen", conversation.peerLastSeen)

  fun messageJson(message: Message): JSONObject = JSONObject()
    .put("id", message.id)
    .put("conversation_id", message.conversationId)
    .put("sender_id", message.senderId)
    .put("sender_name", message.senderName)
    .put("kind", message.kind)
    .put("body", message.body)
    .put("client_operation_id", message.clientOperationId)
    .put("reply_to_id", message.replyToId)
    .put("reply_preview", message.replyPreview)
    .put("sent_at", message.sentAt)
    .put("edited_at", message.editedAt)
    .put("deleted_at", if (message.deleted) "1" else JSONObject.NULL)
    .put("state", message.state)
    .put("reactions", message.reactions)
    .put("own_reaction", message.ownReaction)

  fun storyJson(story: Story): JSONObject = JSONObject()
    .put("id", story.id)
    .put("author_name", story.authorName)
    .put("author_username", story.authorUsername)
    .put("viewed", story.viewed)
    .put("author_verified", story.verified)
    .put("author_id", story.authorId)
    .put("caption", story.caption)
    .put("kind", story.kind)
    .put("audience", story.audience)
    .put("created_at", story.createdAt)
    .put("expires_at", story.expiresAt)
    .put("mine", story.mine)

  private fun firstNonEmpty(row: JSONObject, vararg keys: String): String {
    for (key in keys) {
      val value = row.optString(key, "")
      if (value.isNotEmpty() && value != "null") return value
    }
    return ""
  }

  private fun reactionArray(raw: Any?): JSONArray? = when (raw) {
    is JSONArray -> raw
    is String -> if (raw.startsWith("[")) runCatching { JSONArray(raw) }.getOrNull() else null
    else -> null
  }

  private fun summarizeReactions(rows: JSONArray?, fallback: String): String {
    if (rows == null) return if (fallback.startsWith("[")) "" else fallback
    val counts = linkedMapOf<String, Int>()
    for (i in 0 until rows.length()) {
      val row = rows.optJSONObject(i) ?: continue
      val emoji = row.optString("emoji")
      if (emoji.isEmpty()) continue
      counts[emoji] = counts.getOrElse(emoji) { 0 } + 1
    }
    return counts.entries.joinToString("  ") { "${it.key} ${it.value}" }
  }

  private fun ownReaction(rows: JSONArray?, selfId: String?): String? {
    if (rows == null || selfId == null) return null
    for (i in 0 until rows.length()) {
      val row = rows.optJSONObject(i) ?: continue
      if (selfId == row.optString("user_id")) return row.optString("emoji")
    }
    return null
  }
}
