package app.organicmaps.chats

import app.organicmaps.chats.ChatModels.CallRecord
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Message
import app.organicmaps.chats.ChatModels.SearchHit
import app.organicmaps.chats.ChatModels.Story

/** Pure chat rules shared by Android and iOS. No Android or UIKit imports. */
object ChatLogic {
  val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉")
  const val STORY_AUDIENCE_FRIENDS = "friends"
  const val STORY_AUDIENCE_PRIVATE = "private"
  val storyAudiences = listOf(STORY_AUDIENCE_FRIENDS, STORY_AUDIENCE_PRIVATE)

  /** Stories are friends (mutual) or only-me. Followers/public are not allowed. */
  fun normalizeStoryAudience(audience: String?): String =
    if (audience?.equals(STORY_AUDIENCE_PRIVATE, ignoreCase = true) == true) STORY_AUDIENCE_PRIVATE
    else STORY_AUDIENCE_FRIENDS

  fun relativeTime(epochMs: Long, nowMs: Long): String {
    if (epochMs <= 0L) return ""
    val delta = maxOf(0L, nowMs - epochMs)
    return when {
      delta < 15_000L -> "Now"
      delta < 60_000L -> "${delta / 1000L}s"
      delta < 3_600_000L -> "${delta / 60_000L}m"
      delta < 86_400_000L -> "${delta / 3_600_000L}h"
      delta < 7 * 86_400_000L -> "${delta / 86_400_000L}d"
      else -> isoDate(epochMs)
    }
  }

  fun presenceLabel(
    online: Boolean,
    lastSeen: Long,
    username: String?,
    nowMs: Long,
    status: String? = null
  ): String {
    return when {
      status == "busy" -> "Busy"
      status == "in_call" -> "In a call"
      status == "recording" -> "Recording…"
      online || status == "online" -> "Online"
      lastSeen > 0 -> "Last seen ${relativeTime(lastSeen, nowMs)}"
      username.isNullOrEmpty() -> ""
      else -> "@$username"
    }
  }

  /** Sending → Sent ✓ → Delivered ✓✓ → Read ✓✓ */
  fun deliveryTick(state: String, mine: Boolean): String {
    if (!mine) return ""
    return when (state) {
      "queued", "sending" -> "  ○"
      "delivered" -> "  ✓✓"
      "read" -> "  ✓✓"
      else -> "  ✓"
    }
  }

  fun typingLabel(typerCount: Int): String = when {
    typerCount <= 0 -> ""
    typerCount == 1 -> "Typing…"
    else -> "$typerCount people typing…"
  }

  fun extractMentions(body: String?): List<String> {
    if (body.isNullOrEmpty()) return emptyList()
    return Regex("@([A-Za-z0-9_.]{2,32}|everyone|here)")
      .findAll(body)
      .map { it.groupValues[1].lowercase() }
      .distinct()
      .toList()
  }

  fun previewText(message: Message): String {
    if (message.deleted || message.body == null) return "This message was deleted"
    val first = message.body.substringBefore('\n')
    return when (message.kind) {
      "contact" -> "👤 ${parseContact(message.body)?.name ?: first.removePrefix("👤 ").substringBefore('\n')}"
      "venue" -> if (first.startsWith("📍")) first else "📍 $first"
      "event" -> if (first.startsWith("🎟️")) first else "🎟️ $first"
      "location" -> "📍 Location"
      "image" -> "📷 Photo"
      "video" -> "🎬 Video"
      "voice_note" -> "🎤 Voice note"
      "audio" -> "🎵 Audio"
      "document" -> "📄 $first"
      "moment" -> "Shared a Moment"
      "route" -> "🗺️ Shared a route"
      "flash_drop" -> "🔥 Flash Drop"
      else -> first
    }
  }

  fun inboxPreview(kind: String, body: String?, deleted: Boolean = false): String {
    if (deleted) return "This message was deleted"
    if (body.isNullOrEmpty()) return ""
    return previewText(
      Message(
        id = "",
        conversationId = "",
        senderId = "",
        senderName = "",
        kind = kind,
        body = body,
        sentAt = 0L,
        mine = false
      )
    )
  }

  fun contactCard(contact: ChatModels.Contact): String {
    val lines = mutableListOf("👤 ${contact.name}")
    contact.phone?.takeIf { it.isNotBlank() }?.let { lines.add(it) }
    contact.email?.takeIf { it.isNotBlank() }?.let { lines.add(it) }
    return lines.joinToString("\n")
  }

  fun parseContact(body: String?): ChatModels.Contact? {
    if (body.isNullOrBlank()) return null
    val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return null
    val name = lines[0].removePrefix("👤 ").trim()
    if (name.isEmpty()) return null
    var phone: String? = null
    var email: String? = null
    for (line in lines.drop(1)) {
      if (line.contains("@")) email = line else phone = line
    }
    return ChatModels.Contact(id = name, name = name, phone = phone, email = email)
  }

  fun matchesContactQuery(contact: ChatModels.Contact, query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.lowercase()
    return contact.name.lowercase().contains(needle) ||
      contact.phone?.lowercase()?.contains(needle) == true ||
      contact.email?.lowercase()?.contains(needle) == true
  }

  fun matchesCategory(conversation: Conversation, category: String): Boolean {
    return when (category) {
      ChatModels.FILTER_UNREAD -> conversation.unread > 0
      ChatModels.FILTER_PERSONAL -> conversation.kind == "direct"
      ChatModels.FILTER_GROUP -> conversation.kind == "group"
      ChatModels.FILTER_VENUE ->
        conversation.kind == "venue" || conversation.kind == "event" || conversation.kind == "nightguard"
      else -> true
    }
  }

  fun matchesQuery(conversation: Conversation, query: String): Boolean {
    if (query.isEmpty()) return true
    val needle = query.lowercase()
    return conversation.title.lowercase().contains(needle) ||
      conversation.preview?.lowercase()?.contains(needle) == true ||
      conversation.peerUsername?.lowercase()?.contains(needle) == true
  }

  fun mergeQueued(server: List<Message>, local: List<Message>): List<Message> {
    val ids = server.map { it.id }.toHashSet()
    val operations = server.mapNotNull { it.clientOperationId }.toHashSet()
    val extras = local.filter { message ->
      (message.state == "queued" || message.state == "sending") &&
        message.id !in ids &&
        (message.clientOperationId == null || message.clientOperationId !in operations)
    }
    return (server + extras).sortedBy { it.sentAt }
  }

  fun toggleReactionSummary(current: String, own: String?, emoji: String): Pair<String, String?> {
    val counts = linkedMapOf<String, Int>()
    if (current.isNotEmpty()) {
      for (part in current.split(Regex("\\s{2,}"))) {
        val trimmed = part.trim()
        val split = trimmed.lastIndexOf(' ')
        if (split <= 0) continue
        val count = trimmed.substring(split + 1).toIntOrNull() ?: continue
        counts[trimmed.substring(0, split)] = count
      }
    }
    val removing = emoji == own
    val next = counts.getOrElse(emoji) { 0 } + if (removing) -1 else 1
    if (next <= 0) counts.remove(emoji) else counts[emoji] = next
    if (!removing && !own.isNullOrEmpty() && own != emoji) {
      val previous = counts.getOrElse(own) { 0 } - 1
      if (previous <= 0) counts.remove(own) else counts[own] = previous
    }
    val summary = counts.entries.joinToString("  ") { "${it.key} ${it.value}" }
    return summary to if (removing) null else emoji
  }

  fun usernameFromEmail(email: String): String {
    val local = email.substringBefore('@')
    val cleaned = local.lowercase().replace(Regex("[^a-z0-9_.]"), "")
    return if (cleaned.length >= 3) cleaned.take(32)
    else "user" + email.hashCode().toUInt().toString(16)
  }

  fun displayNameFromEmail(email: String?): String {
    if (email.isNullOrEmpty()) return "You"
    val local = email.substringBefore('@')
    if (local.isEmpty()) return "You"
    return local.replaceFirstChar { it.uppercase() }
  }

  fun demoInbox(now: Long): List<Conversation> = listOf(
    Conversation(
      id = "demo-nightguard",
      kind = "nightguard",
      title = "NightGuard",
      preview = "Your safety check is ready",
      lastMessageKind = "system",
      lastMessageAt = now,
      pinned = true,
      verified = true,
      peerOnline = true,
      peerLastSeen = now
    ),
    Conversation(
      id = "demo-alfred",
      kind = "direct",
      title = "Alfred M.",
      peerId = "demo-alfred-user",
      peerUsername = "alfredm",
      preview = "I’m at Cocoon — pull through 🔥",
      lastMessageKind = "text",
      lastMessageAt = now - 120_000L,
      unread = 1,
      verified = true,
      peerOnline = true,
      peerLastSeen = now
    ),
    Conversation(
      id = "demo-truth",
      kind = "venue",
      title = "Truth Nightclub",
      peerUsername = "truth",
      preview = "Amapiano Fridays starts at 22:00",
      lastMessageKind = "event",
      lastMessageAt = now - 18 * 60_000L,
      verified = true,
      peerLastSeen = now - 3_600_000L
    ),
    Conversation(
      id = "demo-group",
      kind = "group",
      title = "Joburg Fridays",
      preview = "Nomsa: Who’s joining after dinner?",
      lastMessageAt = now - 3_600_000L
    ),
    Conversation(
      id = "demo-lerato",
      kind = "direct",
      title = "Lerato",
      peerId = "demo-lerato-user",
      peerUsername = "lerato",
      preview = "Shared a Moment",
      lastMessageKind = "moment",
      lastMessageAt = now - 3 * 3_600_000L,
      peerLastSeen = now - 86_400_000L
    )
  )

  fun demoMessages(conversationId: String, selfId: String?, now: Long): List<Message> {
    val me = selfId ?: "me"
    return listOf(
      Message(
        id = "$conversationId-1",
        conversationId = conversationId,
        senderId = "peer",
        senderName = "Alfred",
        kind = "text",
        body = "I’m at Cocoon — pull through 🔥",
        sentAt = now - 180_000L,
        mine = false,
        reactions = "🔥 1"
      ),
      Message(
        id = "$conversationId-2",
        conversationId = conversationId,
        senderId = "peer",
        senderName = "Alfred",
        kind = "venue",
        body = "📍 Cocoon Nightclub  ✓\n0.8 km • Open now • Crowd: High",
        sentAt = now - 120_000L,
        mine = false
      ),
      Message(
        id = "$conversationId-3",
        conversationId = conversationId,
        senderId = me,
        senderName = "You",
        kind = "text",
        body = "I’ll be there soon",
        replyToId = "$conversationId-1",
        replyPreview = "I’m at Cocoon — pull through 🔥",
        sentAt = now - 60_000L,
        mine = true,
        state = "read"
      )
    )
  }

  fun demoStories(now: Long = 0L): List<Story> {
    val created = if (now > 0) now else 1_700_000_000_000L
    return listOf(
      Story("demo-story-me", "You", "me", viewed = false, verified = false, authorId = "me",
            caption = "Tonight in Sandton ✨", kind = "text", audience = STORY_AUDIENCE_FRIENDS,
            createdAt = created - 20 * 60_000L, expiresAt = created + 24 * 3_600_000L, mine = true),
      Story("demo-story-alfred", "Alfred", "alfredm", viewed = false, verified = true, authorId = "demo-alfred-user",
            caption = "Cocoon is packed 🔥", kind = "text", audience = STORY_AUDIENCE_FRIENDS,
            createdAt = created - 12 * 60_000L, expiresAt = created + 24 * 3_600_000L),
      Story("demo-story-nomsa", "Nomsa", "nomsa", viewed = false, authorId = "demo-nomsa-user",
            caption = "Getting ready", kind = "text", audience = STORY_AUDIENCE_FRIENDS,
            createdAt = created - 40 * 60_000L, expiresAt = created + 24 * 3_600_000L),
      Story("demo-story-lerato", "Lerato", "lerato", viewed = true, authorId = "demo-lerato-user",
            caption = "See you later", kind = "text", audience = STORY_AUDIENCE_FRIENDS,
            createdAt = created - 3 * 3_600_000L, expiresAt = created + 20 * 3_600_000L)
    )
  }

  fun ownStory(stories: List<Story>, selfId: String?, selfUsername: String?): Story? =
    stories.firstOrNull { it.mine || (selfId != null && it.authorId == selfId) || it.authorUsername == selfUsername }

  fun orderedStories(stories: List<Story>, selfId: String?, selfUsername: String?): List<Story> {
    val others = stories.filter { ownStory(listOf(it), selfId, selfUsername) == null }
    return others.sortedWith(compareBy<Story> { if (it.viewed) 1 else 0 }.thenByDescending { it.createdAt })
  }

  fun demoCalls(now: Long): List<CallRecord> = listOf(
    CallRecord("call-alfred", "demo-alfred", "Alfred M.", "video", "outgoing", "ended", now - 3_600_000L, 12 * 60),
    CallRecord("call-nomsa", "demo-lerato", "Nomsa", "voice", "incoming", "ended", now - 5 * 3_600_000L, 4 * 60),
    CallRecord("call-lerato", "demo-lerato", "Lerato", "video", "incoming", "missed", now - 86_400_000L),
    CallRecord("call-group", "demo-group", "Joburg Fridays", "group_voice", "outgoing", "ended", now - 90_000_000L, 18 * 60)
  )

  fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes min" else "${minutes / 60}h ${minutes % 60}m"
  }

  fun liveTimer(elapsedSec: Int): String {
    val hours = elapsedSec / 3600
    val minutes = (elapsedSec % 3600) / 60
    val seconds = elapsedSec % 60
    return if (hours > 0) {
      "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
      "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
  }

  fun callPreview(record: CallRecord): String {
    val media = if (record.kind.contains("video")) "video" else "voice"
    val duration = formatDuration(record.durationSec)
    return when {
      record.status == "missed" -> "↙ Missed $media call"
      record.status == "declined" -> "Declined $media call"
      record.status == "cancelled" -> "Cancelled $media call"
      record.direction == "incoming" ->
        "↙ Incoming $media" + if (duration.isEmpty()) "" else " • $duration"
      else -> "↗ Outgoing $media" + if (duration.isEmpty()) "" else " • $duration"
    }
  }

  fun matchesCallFilter(record: CallRecord, filter: String): Boolean {
    return when (filter) {
      "missed" -> record.status == "missed"
      "voice" -> record.kind.contains("voice") && !record.kind.contains("video")
      "video" -> record.kind.contains("video")
      "groups" -> record.kind.startsWith("group")
      else -> true
    }
  }

  fun callRoomId(conversationId: String): String {
    val cleaned = conversationId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    return cleaned.take(128).ifEmpty { "fomo-call" }
  }

  fun searchHits(messages: List<Message>, conversations: List<Conversation>, query: String): List<SearchHit> {
    if (query.length < 2) return emptyList()
    val needle = query.lowercase()
    val titles = conversations.associate { it.id to it.title }
    return messages
      .filter { it.body?.lowercase()?.contains(needle) == true && !it.deleted }
      .map {
        SearchHit(it.id, it.conversationId, titles[it.conversationId] ?: "", it.senderName, it.body ?: "", it.sentAt)
      }
  }

  fun parseTime(iso: String?): Long {
    if (iso.isNullOrEmpty() || iso == "null") return 0L
    iso.toLongOrNull()?.let { return it }
    return parseIsoUtc(iso)
  }

  fun emptyToNull(value: String?): String? =
    if (value.isNullOrEmpty() || value == "null") null else value

  fun isoDate(epochMs: Long): String {
    val days = epochMs / 86_400_000L
    var z = days + 719468L
    val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
    val doe = z - era * 146097L
    val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
    var year = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val day = doy - (153L * mp + 2L) / 5L + 1L
    val month = if (mp < 10) mp + 3 else mp - 9
    if (month <= 2) year += 1
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
  }

  private fun parseIsoUtc(iso: String): Long {
    if (iso.length < 19 || iso[4] != '-' || iso[7] != '-' || iso[10] != 'T') return 0L
    val year = iso.substring(0, 4).toIntOrNull() ?: return 0L
    val month = iso.substring(5, 7).toIntOrNull() ?: return 0L
    val day = iso.substring(8, 10).toIntOrNull() ?: return 0L
    val hour = iso.substring(11, 13).toIntOrNull() ?: return 0L
    val minute = iso.substring(14, 16).toIntOrNull() ?: return 0L
    val second = iso.substring(17, 19).toIntOrNull() ?: return 0L
    return utcMillis(year, month, day, hour, minute, second)
  }

  private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era.toLong() * 146097L + doe - 719468L
    return ((days * 24 + hour) * 60 + minute) * 60 + second
  }
}
