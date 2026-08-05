package app.organicmaps.chat.realtime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

/** Small durable outbox used while the device is offline. Server state is reconciled separately. */
public class ChatLocalStore extends SQLiteOpenHelper
{
  private static final String DATABASE = "fomo_chat.db";
  private static final int VERSION = 1;

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
    db.execSQL("CREATE TABLE message_cache (id TEXT PRIMARY KEY, conversation_id TEXT NOT NULL, body TEXT NOT NULL, state TEXT NOT NULL, sent_at INTEGER NOT NULL)");
    db.execSQL("CREATE INDEX message_cache_conversation_sent ON message_cache(conversation_id, sent_at DESC)");
  }

  @Override
  public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {}

  public void enqueue(@NonNull String id, @NonNull String type, @NonNull String payload)
  {
    final ContentValues values = new ContentValues();
    values.put("id", id); values.put("type", type); values.put("payload", payload); values.put("created_at", System.currentTimeMillis());
    getWritableDatabase().insertWithOnConflict("pending_operations", null, values, SQLiteDatabase.CONFLICT_REPLACE);
  }

  @NonNull
  public List<PendingOperation> pendingOperations(int limit)
  {
    final List<PendingOperation> result = new ArrayList<>();
    try (Cursor cursor = getReadableDatabase().query("pending_operations", new String[] {"id", "type", "payload", "attempts"}, null, null, null, null, "created_at ASC", Integer.toString(limit)))
    {
      while (cursor.moveToNext()) result.add(new PendingOperation(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3)));
    }
    return result;
  }

  public void markComplete(@NonNull String id) { getWritableDatabase().delete("pending_operations", "id = ?", new String[] {id}); }
  public void markAttempted(@NonNull String id) { getWritableDatabase().execSQL("UPDATE pending_operations SET attempts = attempts + 1 WHERE id = ?", new Object[] {id}); }
}
