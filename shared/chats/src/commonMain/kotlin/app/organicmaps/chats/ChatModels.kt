package app.organicmaps.chats

/** Platform-agnostic chat records used by Android and iOS. */
object ChatModels {
  const val FILTER_ALL = "all"
  const val FILTER_UNREAD = "unread"
  const val FILTER_PERSONAL = "personal"
  const val FILTER_GROUP = "group"
  const val FILTER_VENUE = "venue"

  data class Conversation(
    val id: String,
    val kind: String,
    val title: String,
    val peerId: String? = null,
    val peerUsername: String? = null,
    val preview: String? = null,
    val lastMessageId: String? = null,
    val lastMessageKind: String = "text",
    val lastMessageAt: Long = 0L,
    val unread: Int = 0,
    val pinned: Boolean = false,
    val verified: Boolean = false,
    val muted: Boolean = false,
    val peerOnline: Boolean = false,
    val peerLastSeen: Long = 0L
  ) {
    fun initials(): String {
      val source = title.replace("✓", "").trim()
      if (source.isEmpty()) return "?"
      val parts = source.split(Regex("\\s+"))
      return if (parts.size == 1) {
        parts[0].take(1).uppercase()
      } else {
        (parts[0].take(1) + parts[1].take(1)).uppercase()
      }
    }

    fun withPreview(newPreview: String?, messageId: String?, sentAt: Long) =
      copy(preview = newPreview, lastMessageId = messageId, lastMessageAt = sentAt, unread = 0)
  }

  data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val kind: String,
    val body: String?,
    val clientOperationId: String? = null,
    val replyToId: String? = null,
    val replyPreview: String? = null,
    val sentAt: Long,
    val editedAt: Long = 0L,
    val mine: Boolean,
    val deleted: Boolean = false,
    val state: String = "sent",
    val reactions: String = "",
    val ownReaction: String? = null
  )

  data class Story(
    val id: String,
    val authorName: String,
    val authorUsername: String,
    val viewed: Boolean = false,
    val verified: Boolean = false,
    val authorId: String = "",
    val caption: String = "",
    val kind: String = "text",
    val audience: String = "friends",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val mine: Boolean = false
  )

  data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val verified: Boolean = false
  )

  data class SearchHit(
    val id: String,
    val conversationId: String,
    val conversationTitle: String,
    val senderName: String,
    val body: String,
    val sentAt: Long
  )

  data class CallRecord(
    val id: String,
    val conversationId: String,
    val title: String,
    val kind: String,
    val direction: String,
    val status: String,
    val startedAt: Long,
    val durationSec: Int = 0
  )

  data class Contact(
    val id: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null
  )
}
