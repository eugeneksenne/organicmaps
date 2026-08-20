package app.organicmaps.chat.data

import app.organicmaps.BuildConfig
import app.organicmaps.chat.realtime.ChatCache
import app.organicmaps.chat.realtime.ChatRealtimeEngine
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Applies durable operations through the authenticated chat RPC. */
class SupabaseChatOperationDispatcher(
  private val client: OkHttpClient,
  supabaseUrl: String,
  private val tokens: ChatRealtimeEngine.AccessTokenProvider
) : ChatRealtimeEngine.OperationDispatcher {
  private val rpcEndpoint = supabaseUrl.trimEnd('/') + "/rest/v1/rpc/apply_chat_operation"
  private val functionEndpoint = supabaseUrl.trimEnd('/') + "/functions/v1/chat-operation"

  override fun dispatch(operation: ChatCache.PendingOperation): Boolean {
    val token = tokens.getAccessToken() ?: return false
    if (token.isEmpty()) return false
    return runCatching {
      val payload = JSONObject(operation.payload)
      val rpcBody = JSONObject()
        .put("operation_id", operation.id)
        .put("operation_type", operation.type)
        .put("payload", payload)
      if (post(rpcEndpoint, token, rpcBody.toString())) return true
      val functionBody = JSONObject()
        .put("id", operation.id)
        .put("type", operation.type)
        .put("payload", payload)
      post(functionEndpoint, token, functionBody.toString())
    }.getOrDefault(false)
  }

  private fun post(url: String, token: String, body: String): Boolean {
    val request = Request.Builder().url(url)
      .header("Authorization", "Bearer $token")
      .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
      .header("Content-Type", "application/json")
      .post(body.toRequestBody(JSON))
      .build()
    return runCatching { client.newCall(request).execute().use { it.isSuccessful } }.getOrDefault(false)
  }

  companion object {
    private val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
