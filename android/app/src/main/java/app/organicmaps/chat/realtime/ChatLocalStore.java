package app.organicmaps.chat.realtime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.chat.data.ChatModels;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Durable outbox, inbox, and message cache. Server state is reconciled separately. */
public class ChatLocalStore extends SQLiteOpenHelper
{
  private static final String DATABASE = "fomo_chat.db";
  private static final int VERSION = 2;

  public static class PendingOperation
  {
    @NonNull public final String id;
    @NonNull public final String type;
    @NonNull public final String payload;
    public final int attempts;

    PendingOperation(@NonNull String id, @NonNull String type, @NonNull String payload, int attempts)
    {
      this.id = id;
      this.type = type;
      this.payload = payload;
      this.attempts = attempts;
    }
  }

  public ChatLocalStore(@NonNull Context context)
  {
    super(context.getApplicationContext(), DATABASE, null, VERSION);
  }

  @Override
  public void onCreate(@NonNull SQLiteDatabase db)
  {
    db.execSQL("CREATE TABLE pending_operations (id TEXT PRIMARY KEY, type TEXT NOT NULL, payload TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL)");
    createCacheTables(db);
  }

  @Override
  public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion)
  {
    if (oldVersion < 2)
      createCacheTables(db);
  }

  private static void createCacheTables(@NonNull SQLiteDatabase db)
  {
    db.execSQL("CREATE TABLE IF NOT EXISTS message_cache (id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL, body TEXT, state TEXT NOT NULL, sent_at INTEGER NOT NULL, payload TEXT NOT NULL DEFAULT '{}')");
    db.execSQL("CREATE INDEX IF NOT EXISTS message_cache_conversation_sent ON message_cache(conversation_id, sent_at DESC)");
    db.execSQL("CREATE TABLE IF NOT EXISTS conversation_cache (id TEXT PRIMARY KEY, kind TEXT NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, last_message_at INTEGER NOT NULL DEFAULT 0, payload TEXT NOT NULL)");
    db.execSQL("CREATE TABLE IF NOT EXISTS drafts (conversation_id TEXT PRIMARY KEY, body TEXT NOT NULL)");
    db.execSQL("CREATE TABLE IF NOT EXISTS story_cache (id TEXT PRIMARY KEY, payload TEXT NOT NULL)");
  }

  public void enqueue(@NonNull String id, @NonNull String type, @NonNull String payload)
  {
    final ContentValues values = new ContentValues();
    values.put("id", id);
    values.put("type", type);
    values.put("payload", payload);
    values.put("created_at", System.currentTimeMillis());
    getWritableDatabase().insertWithOnConflict("pending_operations", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @NonNull
  public List<PendingOperation> pendingOperations(int limit)
  {
    final List<PendingOperation> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query("pending_operations",
             new String[] {"id", "type", "payload", "attempts"}, null, null, null, null, "created_at ASC",
             Integer.toString(limit)))
    {
      while (cursor.moveToNext())
        result.add(new PendingOperation(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3)));
    }
    return result;
  }

  public void markComplete(@NonNull String id)
  {
    getWritableDatabase().delete("pending_operations", "id = ?", new String[] {id});
  }

  public void markAttempted(@NonNull String id)
  {
    getWritableDatabase().execSQL("UPDATE pending_operations SET attempts = attempts + 1 WHERE id = ?", new Object[] {id});
  }

  public void replaceInbox(@NonNull List<ChatModels.Conversation> conversations)
  {
    final SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try
    {
      db.delete("conversation_cache", null, null);
      for (ChatModels.Conversation conversation : conversations)
        upsertConversation(db, conversation);
      db.setTransactionSuccessful();
    }
    finally
    {
      db.endTransaction();
    }
  }

  public void upsertConversation(@NonNull ChatModels.Conversation conversation)
  {
    upsertConversation(getWritableDatabase(), conversation);
  }

  private void upsertConversation(@NonNull SQLiteDatabase db, @NonNull ChatModels.Conversation conversation)
  {
    final ContentValues values = new ContentValues();
    values.put("id", conversation.id);
    values.put("kind", conversation.kind);
    values.put("pinned", conversation.pinned ? 1 : 0);
    values.put("last_message_at", conversation.lastMessageAt);
    values.put("payload", toJson(conversation).toString());
    db.insertWithOnConflict("conversation_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @NonNull
  public List<ChatModels.Conversation> loadInbox()
  {
    final List<ChatModels.Conversation> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query("conversation_cache", new String[] {"payload"},
             null, null, null, null, "pinned DESC, last_message_at DESC"))
    {
      while (cursor.moveToNext())
      {
        try
        {
          final ChatModels.Conversation conversation = ChatModels.parseConversation(new JSONObject(cursor.getString(0)));
          if (conversation != null)
            result.add(conversation);
        }
        catch (JSONException ignored) {}
      }
    }
    return result;
  }

  @Nullable
  public ChatModels.Conversation loadConversation(@NonNull String id)
  {
    try (Cursor cursor = getReadableDatabase().query("conversation_cache", new String[] {"payload"},
             "id = ?", new String[] {id}, null, null, null))
    {
      if (!cursor.moveToFirst())
        return null;
      return ChatModels.parseConversation(new JSONObject(cursor.getString(0)));
    }
    catch (JSONException e)
    {
      return null;
    }
  }

  public void replaceMessages(@NonNull String conversationId, @NonNull List<ChatModels.Message> messages)
  {
    final SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try
    {
      db.delete("message_cache", "conversation_id = ?", new String[] {conversationId});
      for (ChatModels.Message message : messages)
        upsertMessage(db, message);
      db.setTransactionSuccessful();
    }
    finally
    {
      db.endTransaction();
    }
  }

  public void upsertMessage(@NonNull ChatModels.Message message)
  {
    upsertMessage(getWritableDatabase(), message);
  }

  private void upsertMessage(@NonNull SQLiteDatabase db, @NonNull ChatModels.Message message)
  {
    final ContentValues values = new ContentValues();
    values.put("id", message.id);
    values.put("conversation_id", message.conversationId);
    values.put("body", message.body);
    values.put("state", message.state);
    values.put("sent_at", message.sentAt);
    values.put("payload", toJson(message).toString());
    db.insertWithOnConflict("message_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @NonNull
  public List<ChatModels.Message> loadMessages(@NonNull String conversationId, @Nullable String selfId)
  {
    final List<ChatModels.Message> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query("message_cache", new String[] {"payload"},
             "conversation_id = ?", new String[] {conversationId}, null, null, "sent_at ASC"))
    {
      while (cursor.moveToNext())
      {
        try
        {
          final ChatModels.Message message = ChatModels.parseMessage(new JSONObject(cursor.getString(0)), selfId);
          if (message != null)
            result.add(message);
        }
        catch (JSONException ignored) {}
      }
    }
    return result;
  }

  public void saveDraft(@NonNull String conversationId, @NonNull String body)
  {
    final ContentValues values = new ContentValues();
    values.put("conversation_id", conversationId);
    values.put("body", body);
    getWritableDatabase().insertWithOnConflict("drafts", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @NonNull
  public String loadDraft(@NonNull String conversationId)
  {
    try (Cursor cursor = getReadableDatabase().query("drafts", new String[] {"body"},
             "conversation_id = ?", new String[] {conversationId}, null, null, null))
    {
      return cursor.moveToFirst() ? cursor.getString(0) : "";
    }
  }

  public void replaceStories(@NonNull List<ChatModels.Story> stories)
  {
    final SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try
    {
      db.delete("story_cache", null, null);
      for (ChatModels.Story story : stories)
      {
        final ContentValues values = new ContentValues();
        values.put("id", story.id);
        values.put("payload", toJson(story).toString());
        db.insertWithOnConflict("story_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
      }
      db.setTransactionSuccessful();
    }
    finally
    {
      db.endTransaction();
    }
  }

  @NonNull
  public List<ChatModels.Story> loadStories()
  {
    final List<ChatModels.Story> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query("story_cache", new String[] {"payload"}, null, null, null, null, null))
    {
      while (cursor.moveToNext())
      {
        try
        {
          final JSONArray array = new JSONArray().put(new JSONObject(cursor.getString(0)));
          result.addAll(ChatModels.parseStories(array));
        }
        catch (JSONException ignored) {}
      }
    }
    return result;
  }

  @NonNull
  private static JSONObject toJson(@NonNull ChatModels.Conversation conversation)
  {
    try
    {
      return new JSONObject()
          .put("conversation_id", conversation.id)
          .put("kind", conversation.kind)
          .put("title", conversation.title)
          .put("peer_id", conversation.peerId)
          .put("peer_username", conversation.peerUsername)
          .put("last_message_body", conversation.preview)
          .put("last_message_id", conversation.lastMessageId)
          .put("last_message_at", conversation.lastMessageAt)
          .put("unread_count", conversation.unread)
          .put("pinned", conversation.pinned)
          .put("peer_verified", conversation.verified);
    }
    catch (JSONException e)
    {
      return new JSONObject();
    }
  }

  @NonNull
  private static JSONObject toJson(@NonNull ChatModels.Message message)
  {
    try
    {
      return new JSONObject()
          .put("id", message.id)
          .put("conversation_id", message.conversationId)
          .put("sender_id", message.senderId)
          .put("sender_name", message.senderName)
          .put("kind", message.kind)
          .put("body", message.body)
          .put("client_operation_id", message.clientOperationId)
          .put("sent_at", message.sentAt)
          .put("deleted_at", message.deleted ? "1" : JSONObject.NULL)
          .put("state", message.state);
    }
    catch (JSONException e)
    {
      return new JSONObject();
    }
  }

  @NonNull
  private static JSONObject toJson(@NonNull ChatModels.Story story)
  {
    try
    {
      return new JSONObject()
          .put("id", story.id)
          .put("author_name", story.authorName)
          .put("author_username", story.authorUsername)
          .put("viewed", story.viewed)
          .put("author_verified", story.verified);
    }
    catch (JSONException e)
    {
      return new JSONObject();
    }
  }
}
