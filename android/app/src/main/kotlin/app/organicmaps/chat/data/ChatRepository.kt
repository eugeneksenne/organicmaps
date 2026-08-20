package app.organicmaps.chat.data

import app.organicmaps.chat.realtime.ChatCache
import app.organicmaps.chat.realtime.ChatRealtimeEngine
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.CallRecord
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Message
import app.organicmaps.chats.ChatModels.Profile
import app.organicmaps.chats.ChatModels.SearchHit
import app.organicmaps.chats.ChatModels.Story
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Offline-first chats repository. Supabase RPCs are the source of truth. */
class ChatRepository(
  private val rpcClient: ChatRpcClient,
  private val session: FomoAuthSession,
  private val auth: FomoAuthRepository,
  val store: ChatCache,
  private val engine: ChatRealtimeEngine?
) {
  interface InboxCallback {
    fun onLoaded(conversations: List<Conversation>, stories: List<Story>, live: Boolean)
    fun onStatus(status: String)
  }

  interface MessagesCallback {
    fun onLoaded(messages: List<Message>, conversation: Conversation?)
    fun onStatus(status: String)
  }

  interface ProfilesCallback {
    fun onLoaded(profiles: List<Profile>)
    fun onError(message: String)
  }

  interface SearchCallback {
    fun onLoaded(hits: List<SearchHit>)
    fun onError(message: String)
  }

  fun interface PresenceCallback {
    fun onLoaded(online: Boolean, lastSeen: Long)
  }

  interface IdCallback {
    fun onReady(id: String)
    fun onError(message: String)
  }

  fun isConfigured(): Boolean = auth.isConfigured()
  fun isSignedIn(): Boolean = session.isSignedIn()
  fun session(): FomoAuthSession = session
  fun engine(): ChatRealtimeEngine? = engine

  fun loadInbox(callback: InboxCallback) {
    val cached = store.loadInbox()
    val cachedStories = store.loadStories()
    if (cached.isNotEmpty() || cachedStories.isNotEmpty()) callback.onLoaded(cached, cachedStories, false)
    if (!isConfigured()) {
      val demo = ChatLogic.demoInbox(System.currentTimeMillis())
      val stories = ChatLogic.demoStories(System.currentTimeMillis())
      store.replaceInbox(demo)
      store.replaceStories(stories)
      callback.onLoaded(demo, stories, false)
      callback.onStatus("Demo mode. Add android/fomo.properties to use a live backend.")
      return
    }
    if (!isSignedIn()) {
      if (cached.isEmpty()) callback.onLoaded(emptyList(), emptyList(), false)
      callback.onStatus("Sign in to sync your conversations.")
      return
    }
    Thread {
      if (!auth.refreshIfNeeded()) {
        callback.onStatus("Session expired. Sign in again.")
        return@Thread
      }
      try {
        touchPresence(null)
        val conversations = ChatJson.parseInbox(rpcArray("chat_inbox", JSONObject()))
        val stories = ChatJson.parseStories(rpcArray("inbox_stories", JSONObject()))
        store.replaceInbox(conversations)
        store.replaceStories(stories)
        callback.onLoaded(conversations, stories, true)
        callback.onStatus("")
        engine?.flushOutbox()
      } catch (_: Exception) {
        if (cached.isEmpty()) callback.onLoaded(store.loadInbox(), store.loadStories(), false)
        callback.onStatus("Couldn't refresh chats. Showing saved conversations.")
      }
    }.start()
  }

  fun loadMessages(conversationId: String, callback: MessagesCallback) {
    val selfId = session.userId
    var cached = store.loadMessages(conversationId, selfId)
    var conversation = store.loadConversation(conversationId) ?: findDemoConversation(conversationId)
    if (conversationId.startsWith("demo-") && cached.isEmpty()) {
      cached = ChatLogic.demoMessages(conversationId, selfId, System.currentTimeMillis())
      store.replaceMessages(conversationId, cached)
    }
    if (cached.isNotEmpty()) callback.onLoaded(cached, conversation)
    if (!isConfigured() || !isSignedIn() || conversationId.startsWith("demo-")) return
    Thread {
      try {
        if (!auth.refreshIfNeeded()) {
          callback.onStatus("Session expired.")
          return@Thread
        }
        touchPresence(conversationId)
        val rows = rpcArray("chat_messages", JSONObject().put("target", conversationId).put("page_size", 80))
        val server = ChatJson.parseMessages(rows, session.userId)
        val merged = ChatLogic.mergeQueued(server, store.loadMessages(conversationId, session.userId))
        store.replaceMessages(conversationId, merged)
        callback.onLoaded(merged, store.loadConversation(conversationId))
        callback.onStatus("")
        val incoming = merged.lastOrNull { !it.mine && !it.deleted }
        if (incoming != null) markDelivered(conversationId, incoming.id)
        if (merged.isNotEmpty()) markRead(conversationId, merged.last().id)
      } catch (_: Exception) {
        callback.onStatus("Couldn't refresh this conversation.")
      }
    }.start()
  }

  fun sendText(conversationId: String, text: String, replyToId: String? = null): Message =
    send(conversationId, "text", text, replyToId)

  fun sendCard(conversationId: String, kind: String, body: String): Message =
    send(conversationId, kind, body, null)

  fun send(conversationId: String, kind: String, text: String, replyToId: String?): Message {
    val operationId = UUID.randomUUID().toString()
    val selfId = session.userId ?: "me"
    val replyPreview = replyToId?.let { id -> store.loadMessages(conversationId, selfId).firstOrNull { it.id == id }?.body }
    val optimistic = Message(
      id = operationId,
      conversationId = conversationId,
      senderId = selfId,
      senderName = "You",
      kind = kind,
      body = text,
      clientOperationId = operationId,
      replyToId = replyToId,
      replyPreview = replyPreview,
      sentAt = System.currentTimeMillis(),
      mine = true,
      state = "queued"
    )
    store.upsertMessage(optimistic)
    store.loadConversation(conversationId)?.let {
      store.upsertConversation(it.withPreview(ChatLogic.previewText(optimistic), optimistic.id, optimistic.sentAt))
    }
    queue("message", operationId, conversationId) { json ->
      json.put("conversationId", conversationId).put("kind", kind).put("body", text)
        .put("replyToId", replyToId ?: "")
    }
    return optimistic
  }

  fun editMessage(conversationId: String, messageId: String, body: String): Message? {
    val current = findMessage(conversationId, messageId) ?: return null
    if (!current.mine || current.deleted) return null
    val edited = current.copy(body = body, editedAt = System.currentTimeMillis(), deleted = false)
    store.upsertMessage(edited)
    queue("edit", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("messageId", messageId).put("body", body).put("conversationId", conversationId)
    }
    return edited
  }

  fun deleteMessage(conversationId: String, messageId: String): Message? {
    val current = findMessage(conversationId, messageId) ?: return null
    if (!current.mine || current.deleted) return null
    val deleted = current.copy(body = null, deleted = true, state = "deleted")
    store.upsertMessage(deleted)
    queue("delete", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("messageId", messageId).put("conversationId", conversationId)
    }
    return deleted
  }

  fun toggleReaction(conversationId: String, messageId: String, emoji: String): Message? {
    val current = findMessage(conversationId, messageId) ?: return null
    if (current.deleted) return null
    val (summary, own) = ChatLogic.toggleReactionSummary(current.reactions, current.ownReaction, emoji)
    val updated = current.copy(reactions = summary, ownReaction = own)
    store.upsertMessage(updated)
    queue("reaction", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("messageId", messageId).put("emoji", emoji).put("conversationId", conversationId)
    }
    return updated
  }

  fun searchPeople(query: String, callback: ProfilesCallback) {
    if (!isConfigured() || !isSignedIn()) {
      callback.onError("Sign in to find people.")
      return
    }
    Thread {
      try {
        auth.refreshIfNeeded()
        callback.onLoaded(ChatJson.parseProfiles(rpcArray("search_chat_profiles", JSONObject().put("query", query))))
      } catch (_: Exception) {
        callback.onError("Couldn't search people.")
      }
    }.start()
  }

  fun searchMessages(conversationId: String?, query: String, callback: SearchCallback) {
    val local = store.searchMessages(conversationId, query, session.userId)
    callback.onLoaded(ChatLogic.searchHits(local, store.loadInbox(), query))
    if (!isConfigured() || !isSignedIn() || query.trim().length < 2) return
    Thread {
      try {
        auth.refreshIfNeeded()
        val body = JSONObject().put("query", query)
        if (conversationId != null) body.put("target", conversationId)
        callback.onLoaded(ChatJson.parseSearchHits(rpcArray("search_chat_messages", body)))
      } catch (_: Exception) {
        callback.onError("Couldn't search messages.")
      }
    }.start()
  }

  fun loadPresence(conversationId: String, callback: PresenceCallback) {
    val conversation = conversation(conversationId)
    if (conversation != null) callback.onLoaded(conversation.peerOnline, conversation.peerLastSeen)
    if (!isConfigured() || !isSignedIn() || conversationId.startsWith("demo-")) return
    Thread {
      try {
        val rows = rpcArray("conversation_presence", JSONObject().put("target", conversationId))
        var online = false
        var lastSeen = conversation?.peerLastSeen ?: 0L
        for (i in 0 until rows.length()) {
          val row = rows.optJSONObject(i) ?: continue
          online = online || row.optBoolean("online")
          lastSeen = maxOf(lastSeen, ChatLogic.parseTime(row.optString("last_seen_at")))
        }
        callback.onLoaded(online, lastSeen)
      } catch (_: Exception) {
      }
    }.start()
  }

  fun openDirect(userId: String, callback: IdCallback) {
    mutateId("open_direct_conversation", { it.put("other_user", userId) }, callback)
  }

  fun createGroup(title: String, callback: IdCallback) = createGroup(title, emptyList(), callback)

  fun createGroup(title: String, memberIds: List<String>, callback: IdCallback) {
    mutateId("create_group_conversation", { json ->
      val members = JSONArray()
      memberIds.forEach { members.put(it) }
      json.put("p_title", title).put("member_ids", members)
    }, callback)
  }

  fun markRead(conversationId: String, messageId: String) {
    queue("receipt", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId).put("messageId", messageId)
    }
  }

  fun markDelivered(conversationId: String, messageId: String) {
    queue("delivered", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId).put("messageId", messageId)
    }
  }

  fun setPinned(conversationId: String, pinned: Boolean) {
    store.loadConversation(conversationId)?.let { store.upsertConversation(it.copy(pinned = pinned)) }
    queue("pin", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId).put("pinned", pinned)
    }
  }

  fun setMuted(conversationId: String, muted: Boolean) {
    store.loadConversation(conversationId)?.let { store.upsertConversation(it.copy(muted = muted)) }
    queue("mute", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId).put("muted", muted)
    }
  }

  fun clearHistory(conversationId: String) {
    store.clearMessages(conversationId)
    queue("clear", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId)
    }
  }

  fun leaveConversation(conversationId: String) {
    store.deleteConversation(conversationId)
    queue("leave", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId)
    }
  }

  fun blockPeer(userId: String, conversationId: String) {
    store.deleteConversation(conversationId)
    queue("block", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("userId", userId).put("conversationId", conversationId)
    }
  }

  fun reportPeer(conversationId: String, userId: String?, reason: String) {
    queue("report", UUID.randomUUID().toString(), conversationId) { json ->
      json.put("conversationId", conversationId).put("userId", userId ?: "").put("reason", reason)
    }
  }

  fun touchPresence(conversationId: String?) {
    if (!isConfigured() || !isSignedIn()) return
    val payload = JSONObject().put("conversationId", conversationId ?: "").put("status", "online")
    if (engine != null) engine.queueOperation(UUID.randomUUID().toString(), "presence", payload)
    else Thread {
      runCatching { dispatchDirect(UUID.randomUUID().toString(), "presence", payload) }
    }.start()
  }

  interface CallsCallback {
    fun onLoaded(calls: List<CallRecord>)
  }

  fun loadCalls(callback: CallsCallback) {
    val demo = ChatLogic.demoCalls(System.currentTimeMillis())
    callback.onLoaded(demo)
    if (!isConfigured() || !isSignedIn()) return
    Thread {
      try {
        auth.refreshIfNeeded()
        val rows = rpcArray("call_inbox", JSONObject())
        val parsed = ArrayList<CallRecord>()
        for (i in 0 until rows.length()) {
          val row = rows.optJSONObject(i) ?: continue
          val id = row.optString("id")
          if (id.isEmpty()) continue
          parsed.add(
            CallRecord(
              id = id,
              conversationId = row.optString("conversation_id"),
              title = row.optString("title"),
              kind = row.optString("kind"),
              direction = row.optString("direction"),
              status = row.optString("status"),
              startedAt = ChatLogic.parseTime(row.optString("started_at")),
              durationSec = row.optInt("duration_sec")
            )
          )
        }
        if (parsed.isNotEmpty()) callback.onLoaded(parsed)
      } catch (_: Exception) {
      }
    }.start()
  }

  fun startCall(conversationId: String, kind: String) {
    if (!looksLikeUuid(conversationId) || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("start_chat_call", JSONObject().put("target", conversationId).put("kind", kind)) }
    }.start()
  }

  fun endCall(conversationId: String) {
    if (!looksLikeUuid(conversationId) || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("end_chat_call", JSONObject().put("target", conversationId).put("reason", "ended")) }
    }.start()
  }

  fun findStory(id: String): Story? =
    store.loadStories().firstOrNull { it.id == id || it.authorUsername == id || it.authorName == id }

  fun publishStory(caption: String, audience: String = ChatLogic.STORY_AUDIENCE_FRIENDS, callback: IdCallback) {
    val text = caption.trim()
    if (text.isEmpty()) {
      callback.onError("Write something first.")
      return
    }
    val allowed = ChatLogic.normalizeStoryAudience(audience)
    val local = Story(
      id = "local-" + UUID.randomUUID(),
      authorName = session.displayName ?: "You",
      authorUsername = session.userId ?: "me",
      authorId = session.userId ?: "me",
      caption = text,
      kind = "text",
      audience = allowed,
      createdAt = System.currentTimeMillis(),
      expiresAt = System.currentTimeMillis() + 24 * 3_600_000L,
      mine = true
    )
    store.replaceStories(listOf(local) + store.loadStories())
    if (!isConfigured() || !isSignedIn()) {
      callback.onReady(local.id)
      return
    }
    Thread {
      try {
        auth.refreshIfNeeded()
        val raw = rpc(
          "publish_story",
          JSONObject().put("p_caption", text).put("p_kind", "text").put("p_audience", allowed)
        )
        callback.onReady(raw.replace("\"", "").trim().ifEmpty { local.id })
      } catch (_: Exception) {
        callback.onReady(local.id)
      }
    }.start()
  }

  fun markStoryViewed(storyId: String) {
    val updated = store.loadStories().map { if (it.id == storyId) it.copy(viewed = true) else it }
    store.replaceStories(updated)
    if (storyId.startsWith("demo-") || storyId.startsWith("local-") || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("mark_story_viewed", JSONObject().put("target", storyId)) }
    }.start()
  }

  fun reactToStory(storyId: String, emoji: String = "❤️") {
    if (storyId.startsWith("demo-") || storyId.startsWith("local-") || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("react_to_story", JSONObject().put("target", storyId).put("emoji", emoji)) }
    }.start()
  }

  fun deleteStory(storyId: String) {
    store.replaceStories(store.loadStories().filterNot { it.id == storyId })
    if (storyId.startsWith("demo-") || storyId.startsWith("local-") || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("delete_own_story", JSONObject().put("target", storyId)) }
    }.start()
  }

  fun muteStoryAuthor(authorId: String) {
    store.replaceStories(store.loadStories().filterNot { it.authorId == authorId })
    if (!looksLikeUuid(authorId) || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching { rpc("mute_story_author", JSONObject().put("target", authorId).put("muted", true)) }
    }.start()
  }

  fun conversation(id: String): Conversation? = store.loadConversation(id) ?: findDemoConversation(id)

  fun findMessage(conversationId: String, messageId: String): Message? =
    store.loadMessages(conversationId, session.userId).firstOrNull { it.id == messageId }

  private fun mutateId(rpcName: String, builder: (JSONObject) -> JSONObject, callback: IdCallback) {
    if (!isConfigured() || !isSignedIn()) {
      callback.onError("Sign in first.")
      return
    }
    Thread {
      try {
        auth.refreshIfNeeded()
        val id = rpc(rpcName, builder(JSONObject())).replace("\"", "").trim()
        if (id.isEmpty()) callback.onError("Could not open conversation.")
        else callback.onReady(id)
      } catch (_: Exception) {
        callback.onError("Could not open conversation.")
      }
    }.start()
  }

  private fun queue(type: String, operationId: String, conversationId: String, builder: (JSONObject) -> JSONObject) {
    if (!conversationId.matches(Regex("[0-9a-fA-F-]{36}")) || !isConfigured() || !isSignedIn()) return
    val payload = builder(JSONObject())
    if (engine != null) engine.queueOperation(operationId, type, payload)
    else Thread { runCatching { dispatchDirect(operationId, type, payload) } }.start()
  }

  private fun dispatchDirect(id: String, type: String, payload: JSONObject) {
    rpc("apply_chat_operation", JSONObject().put("operation_id", id).put("operation_type", type).put("payload", payload))
  }

  private fun rpcArray(name: String, body: JSONObject): JSONArray {
    val trimmed = rpc(name, body).trim()
    if (trimmed.startsWith("[")) return JSONArray(trimmed)
    if (trimmed.isEmpty() || trimmed == "null") return JSONArray()
    return JSONArray().put(JSONObject(trimmed))
  }

  private fun rpc(name: String, body: JSONObject): String = rpcClient.rpc(name, body)

  private fun findDemoConversation(id: String): Conversation =
    ChatLogic.demoInbox(System.currentTimeMillis()).firstOrNull { it.id == id }
      ?: Conversation(id, "direct", if (id.startsWith("demo-")) "Chat" else "Conversation")

  private fun looksLikeUuid(value: String): Boolean =
    value.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))

}
