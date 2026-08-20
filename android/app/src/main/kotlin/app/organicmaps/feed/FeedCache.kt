package app.organicmaps.feed

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.organicmaps.feed.RippleModels.PendingRipple

/** Durable Ripple outbox. Desired state is stored, not a toggle, so retries stay idempotent. */
class FeedCache(context: Context) : SQLiteOpenHelper(context.applicationContext, DATABASE, null, VERSION) {
  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      "CREATE TABLE ripple_outbox (moment_id TEXT PRIMARY KEY, want INTEGER NOT NULL, queued_at INTEGER NOT NULL)"
    )
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

  fun enqueueRipple(momentId: String, wantRippled: Boolean) {
    if (momentId.isEmpty()) return
    val values = ContentValues()
    values.put("moment_id", momentId)
    values.put("want", if (wantRippled) 1 else 0)
    values.put("queued_at", System.currentTimeMillis())
    writableDatabase.insertWithOnConflict("ripple_outbox", null, values, SQLiteDatabase.CONFLICT_REPLACE)
  }

  fun pendingRipples(): List<PendingRipple> {
    val result = ArrayList<PendingRipple>()
    readableDatabase.query(
      "ripple_outbox", arrayOf("moment_id", "want", "queued_at"),
      null, null, null, null, "queued_at ASC"
    ).use { cursor ->
      while (cursor.moveToNext()) {
        result.add(PendingRipple(cursor.getString(0), cursor.getInt(1) == 1, cursor.getLong(2)))
      }
    }
    return result
  }

  fun markRippleSent(momentId: String) {
    writableDatabase.delete("ripple_outbox", "moment_id = ?", arrayOf(momentId))
  }

  companion object {
    private const val DATABASE = "fomo_feed.db"
    private const val VERSION = 1
  }
}
