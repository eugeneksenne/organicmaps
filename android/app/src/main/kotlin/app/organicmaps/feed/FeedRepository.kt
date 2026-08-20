package app.organicmaps.feed

import app.organicmaps.chat.data.ChatRepository
import app.organicmaps.chat.data.ChatRpcClient
import app.organicmaps.chat.data.FomoAuthRepository
import app.organicmaps.chat.data.FomoAuthSession
import app.organicmaps.feed.FeedJson.ReactResult
import app.organicmaps.feed.FeedModels.Comment
import app.organicmaps.feed.FeedModels.Moment
import org.json.JSONArray
import org.json.JSONObject

/** Feed + Ripple client. Momentum is never computed here. */
class FeedRepository(
  private val rpcClient: ChatRpcClient,
  private val session: FomoAuthSession,
  private val auth: FomoAuthRepository,
  private val cache: FeedCache
) {
  interface PageCallback {
    fun onLoaded(moments: List<Moment>, live: Boolean)
    fun onStatus(status: String)
  }

  interface CommentsCallback {
    fun onLoaded(comments: List<Comment>)
    fun onError(message: String)
  }

  interface ReactCallback {
    fun onReady(result: ReactResult)
    fun onError(message: String)
  }

  fun isConfigured(): Boolean = auth.isConfigured()
  fun isSignedIn(): Boolean = session.isSignedIn()

  fun load(tab: String, callback: PageCallback) {
    if (!isConfigured()) {
      callback.onLoaded(FeedLogic.demoFeed(tab, System.currentTimeMillis()), false)
      callback.onStatus("Demo feed. Add android/fomo.properties to sync live Moments.")
      return
    }
    if (!isSignedIn()) {
      callback.onLoaded(emptyList(), false)
      callback.onStatus("Sign in to see Moments for you.")
      return
    }
    Thread {
      flushRipples()
      try {
        auth.refreshIfNeeded()
        val rows = rpcArray("feed_page", JSONObject().put("p_kind", tab).put("page_size", 12))
        callback.onLoaded(FeedJson.parseMoments(rows), true)
        callback.onStatus("")
      } catch (_: Exception) {
        callback.onLoaded(FeedLogic.demoFeed(tab, System.currentTimeMillis()), false)
        callback.onStatus("Couldn't refresh Feed. Showing saved Moments.")
      }
    }.start()
  }

  fun toggle(momentId: String, reaction: String, wantOn: Boolean, callback: ReactCallback) {
    if (momentId.startsWith("demo-") || !isConfigured() || !isSignedIn()) {
      callback.onReady(ReactResult(removed = !wantOn, snapshot = null))
      return
    }
    Thread {
      try {
        auth.refreshIfNeeded()
        val body = JSONObject().put("target", momentId).put("p_reaction", reaction).put("p_on", wantOn)
        val raw = rpcClient.rpc("set_moment_reaction", body)
        cache.markRippleSent(momentId)
        callback.onReady(FeedJson.parseReact(raw))
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        if (reaction == "ripple" && isTransient(message)) {
          cache.enqueueRipple(momentId, wantOn)
          callback.onReady(ReactResult(removed = !wantOn, snapshot = null))
        } else {
          callback.onError(error.message ?: "Couldn't update")
        }
      }
    }.start()
  }

  fun flushRipples() {
    if (!isConfigured() || !isSignedIn()) return
    cache.pendingRipples().forEach { pending ->
      try {
        auth.refreshIfNeeded()
        rpcClient.rpc(
          "set_moment_reaction",
          JSONObject().put("target", pending.momentId).put("p_reaction", "ripple").put("p_on", pending.wantRippled)
        )
        cache.markRippleSent(pending.momentId)
      } catch (error: Exception) {
        val message = error.message.orEmpty()
        if (message.contains("unknown_moment") || message.contains("unauthorized") || message.contains("invalid_reaction")) {
          cache.markRippleSent(pending.momentId)
        }
      }
    }
  }

  fun follow(userId: String, following: Boolean) {
    if (!userId.matches(UUID) || !isConfigured() || !isSignedIn()) return
    Thread {
      runCatching {
        auth.refreshIfNeeded()
        rpcClient.rpc("follow_profile", JSONObject().put("other_user", userId).put("following", following))
      }
    }.start()
  }

  fun comments(momentId: String, callback: CommentsCallback) {
    if (momentId.startsWith("demo-") || !isConfigured() || !isSignedIn()) {
      callback.onLoaded(emptyList())
      return
    }
    Thread {
      try {
        callback.onLoaded(FeedJson.parseComments(rpcArray("moment_comments", JSONObject().put("target", momentId))))
      } catch (_: Exception) {
        callback.onError("Couldn't load comments")
      }
    }.start()
  }

  fun comment(momentId: String, body: String, callback: ChatRepository.IdCallback) {
    if (momentId.startsWith("demo-") || !isConfigured() || !isSignedIn()) {
      callback.onError("Sign in to comment")
      return
    }
    Thread {
      try {
        auth.refreshIfNeeded()
        val id = rpcClient.rpc("add_moment_comment", JSONObject().put("target", momentId).put("p_body", body))
        callback.onReady(id.replace("\"", "").trim())
      } catch (_: Exception) {
        callback.onError("Couldn't comment")
      }
    }.start()
  }

  private fun rpcArray(name: String, body: JSONObject): JSONArray {
    val trimmed = rpcClient.rpc(name, body).trim()
    if (trimmed.startsWith("[")) return JSONArray(trimmed)
    if (trimmed.isEmpty() || trimmed == "null") return JSONArray()
    return JSONArray().put(JSONObject(trimmed))
  }

  companion object {
    private val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    fun isTransient(message: String): Boolean {
      val lower = message.lowercase()
      if (lower.contains("rate_limited") || lower.contains("unauthorized") || lower.contains("unknown_moment")) {
        return false
      }
      return lower.contains("failed to connect") ||
        lower.contains("timeout") ||
        lower.contains("unable to resolve") ||
        lower.contains("network") ||
        lower.contains("signed_out") ||
        lower.contains("connection")
    }
  }
}
