package app.organicmaps.chat.data

import app.organicmaps.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/** PostgREST RPC transport. Swap implementations without changing ChatRepository. */
fun interface ChatRpcClient {
  fun rpc(name: String, body: JSONObject): String
}

/**
 * Default transport: OkHttp to `/rest/v1/rpc`. Same HTTP contract supabase-kt PostgREST uses.
 * JWT + anon key only.
 */
class OkHttpChatRpcClient(
  private val http: OkHttpClient,
  private val supabaseUrl: String,
  private val anonKey: String,
  private val accessToken: () -> String?
) : ChatRpcClient {
  override fun rpc(name: String, body: JSONObject): String {
    val token = accessToken() ?: throw IOException("signed_out")
    val request = Request.Builder()
      .url(supabaseUrl.trimEnd('/') + "/rest/v1/rpc/" + name)
      .header("apikey", anonKey)
      .header("Authorization", "Bearer $token")
      .header("Content-Type", "application/json")
      .post(body.toString().toRequestBody(JSON))
      .build()
    http.newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) throw IOException(raw)
      return raw
    }
  }

  companion object {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun fromSession(http: OkHttpClient, session: FomoAuthSession): OkHttpChatRpcClient =
      OkHttpChatRpcClient(
        http,
        BuildConfig.FOMO_SUPABASE_URL,
        BuildConfig.FOMO_SUPABASE_ANON_KEY
      ) { session.accessToken }
  }
}
