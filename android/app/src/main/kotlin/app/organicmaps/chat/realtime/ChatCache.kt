package app.organicmaps.chat.realtime

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.organicmaps.chat.data.ChatJson
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Message
import app.organicmaps.chats.ChatModels.Story
import org.json.JSONArray
import org.json.JSONObject

/** Durable outbox, inbox, and message cache. Server state is reconciled separately. */
class ChatCache(context: Context) : SQLiteOpenHelper(context.applicationContext, DATABASE, null, VERSION) {
  data class PendingOperation(val id: String, val type: String, val payload: String, val attempts: Int)
  data class Draft(val body: String, val replyToId: String?)

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE pending_operations (id TEXT PRIMARY KEY, type TEXT NOT NULL, payload TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)"
    )
    createCacheTables(db)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) createCacheTables(db)
    if (oldVersion < 3 && oldVersion >= 2) db.execSQL("ALTER TABLE drafts ADD COLUMN reply_to TEXT")
  }

  fun enqueue(id: String, type: String, payload: String) {
    val values = ContentValues()
    values.put("id", id)
    values.put("type", type)
    values.put("payload", payload)
    values.put("created_at", System.currentTimeMillis())
    writableDatabase.insertWithOnConflict("pending_operations", null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  fun pendingOperations(limit: Int): List<PendingOperation> {
    val result = ArrayList<PendingOperation>()
    readableDatabase.query(
      "pending_operations", arrayOf("id", "type", "payload", "attempts"),
      null, null, null, null, "created_at ASC", limit.toString()
    ).use { cursor ->
      while (cursor.moveToNext()) {
        result.add(PendingOperation(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3)))
      }
    }
    return result
  }

  fun markComplete(id: String) {
    writableDatabase.delete("pending_operations", "id = ?", arrayOf(id))
  }

  fun markAttempted(id: String) {
    writableDatabase.execSQL("UPDATE pending_operations SET attempts = attempts + 1 WHERE id = ?", arrayOf(id))
  }

  fun replaceInbox(conversations: List<Conversation>) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      db.delete("conversation_cache", null, null)
      conversations.forEach { upsertConversation(db, it) }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun upsertConversation(conversation: Conversation) = upsertConversation(writableDatabase, conversation)

  fun deleteConversation(id: String) {
    writableDatabase.delete("conversation_cache", "id = ?", arrayOf(id))
    writableDatabase.delete("message_cache", "conversation_id = ?", arrayOf(id))
    writableDatabase.delete("drafts", "conversation_id = ?", arrayOf(id))
  }

  fun clearMessages(conversationId: String) {
    writableDatabase.delete("message_cache", "conversation_id = ?", arrayOf(conversationId))
  }

  fun loadInbox(): List<Conversation> {
    val result = ArrayList<Conversation>()
    readableDatabase.query(
      "conversation_cache", arrayOf("payload"), null, null, null, null, "pinned DESC, last_message_at DESC"
    ).use { cursor ->
      while (cursor.moveToNext()) {
        runCatching { ChatJson.parseConversation(JSONObject(cursor.getString(0))) }.getOrNull()?.let { result.add(it) }
      }
    }
    return result
  }

  fun loadConversation(id: String): Conversation? {
    readableDatabase.query("conversation_cache", arrayOf("payload"), "id = ?", arrayOf(id), null, null, null).use { cursor ->
      if (!cursor.moveToFirst()) return null
      return runCatching { ChatJson.parseConversation(JSONObject(cursor.getString(0))) }.getOrNull()
    }
  }

  fun replaceMessages(conversationId: String, messages: List<Message>) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      db.delete("message_cache", "conversation_id = ?", arrayOf(conversationId))
      messages.forEach { upsertMessage(db, it) }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun upsertMessage(message: Message) = upsertMessage(writableDatabase, message)

  fun loadMessages(conversationId: String, selfId: String?): List<Message> {
    val result = ArrayList<Message>()
    readableDatabase.query(
      "message_cache", arrayOf("payload"), "conversation_id = ?", arrayOf(conversationId),
      null, null, "sent_at ASC"
    ).use { cursor ->
      while (cursor.moveToNext()) {
        runCatching { ChatJson.parseMessage(JSONObject(cursor.getString(0)), selfId) }.getOrNull()?.let { result.add(it) }
      }
    }
    return result
  }

  fun searchMessages(conversationId: String?, query: String, selfId: String?): List<Message> {
    val needle = "%${query.replace("%", "").replace("_", "")}%"
    val selection = if (conversationId == null) "body LIKE ? COLLATE NOCASE"
    else "conversation_id = ? AND body LIKE ? COLLATE NOCASE"
    val args = if (conversationId == null) arrayOf(needle) else arrayOf(conversationId, needle)
    val result = ArrayList<Message>()
    readableDatabase.query("message_cache", arrayOf("payload"), selection, args, null, null, "sent_at DESC", "50")
      .use { cursor ->
        while (cursor.moveToNext()) {
          val message = runCatching { ChatJson.parseMessage(JSONObject(cursor.getString(0)), selfId) }.getOrNull()
          if (message != null && !message.deleted) result.add(message)
        }
      }
    return result
  }

  fun saveDraft(conversationId: String, body: String, replyToId: String? = null) {
    val values = ContentValues()
    values.put("conversation_id", conversationId)
    values.put("body", body)
    values.put("reply_to", replyToId)
    writableDatabase.insertWithOnConflict("drafts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  fun loadDraftState(conversationId: String): Draft {
    readableDatabase.query(
      "drafts", arrayOf("body", "reply_to"), "conversation_id = ?", arrayOf(conversationId), null, null, null
    ).use { cursor ->
      if (!cursor.moveToFirst()) return Draft("", null)
      return Draft(cursor.getString(0), if (cursor.isNull(1)) null else cursor.getString(1))
    }
  }

  fun replaceStories(stories: List<Story>) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      db.delete("story_cache", null, null)
      stories.forEach { story ->
        val values = ContentValues()
        values.put("id", story.id)
        values.put("payload", ChatJson.storyJson(story).toString())
        db.insertWithOnConflict("story_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
      }
      db.setTransactionSuccessful()
    } finally {
      db.endTransaction()
    }
  }

  fun loadStories(): List<Story> {
    val result = ArrayList<Story>()
    readableDatabase.query("story_cache", arrayOf("payload"), null, null, null, null, null).use { cursor ->
      while (cursor.moveToNext()) {
        runCatching {
          result.addAll(ChatJson.parseStories(JSONArray().put(JSONObject(cursor.getString(0)))))
        }
      }
    }
    return result
  }

  private fun upsertConversation(db: SQLiteDatabase, conversation: Conversation) {
    val values = ContentValues()
    values.put("id", conversation.id)
    values.put("kind", conversation.kind)
    values.put("pinned", if (conversation.pinned) 1 else 0)
    values.put("last_message_at", conversation.lastMessageAt)
    values.put("payload", ChatJson.conversationJson(conversation).toString())
    db.insertWithOnConflict("conversation_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  private fun upsertMessage(db: SQLiteDatabase, message: Message) {
    val values = ContentValues()
    values.put("id", message.id)
    values.put("conversation_id", message.conversationId)
    values.put("body", message.body)
    values.put("state", message.state)
    values.put("sent_at", message.sentAt)
    values.put("payload", ChatJson.messageJson(message).toString())
    db.insertWithOnConflict("message_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  companion object {
    private const val DATABASE = "fomo_chat.db"
    private const val VERSION = 3

    private fun createCacheTables(db: SQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS message_cache (id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL, body TEXT, state TEXT NOT NULL, sent_at INTEGER NOT NULL, payload TEXT NOT NULL DEFAULT '{}')")
      db.execSQL("CREATE INDEX IF NOT EXISTS message_cache_conversation_sent ON message_cache(conversation_id, sent_at DESC)")
      db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (id TEXT PRIMARY KEY, kind TEXT NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, last_message_at INTEGER NOT NULL DEFAULT 0, payload TEXT NOT NULL)")
      db.execSQL("CREATE TABLE IF NOT EXISTS drafts (conversation_id TEXT PRIMARY KEY, body TEXT NOT NULL, reply_to TEXT)")
      db.execSQL("CREATE TABLE IF NOT EXISTS story_cache (id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
    }
  }
}
