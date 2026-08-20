package app.organicmaps.chat.data

import android.content.Context
import app.organicmaps.BuildConfig
import app.organicmaps.chat.realtime.ChatRealtimeEngine
import okhttp3.OkHttpClient

/** Builds the realtime engine using public per-developer configuration from fomo.properties. */
object ChatEngineFactory {
  fun isConfigured(): Boolean =
    BuildConfig.FOMO_SUPABASE_URL.isNotEmpty() && BuildConfig.FOMO_SUPABASE_ANON_KEY.isNotEmpty()

  fun create(
    context: Context,
    tokenProvider: ChatRealtimeEngine.AccessTokenProvider,
    listener: ChatRealtimeEngine.Listener
  ): ChatRealtimeEngine? {
    if (!isConfigured()) return null
    return ChatRealtimeEngine(
      context,
      tokenProvider,
      SupabaseChatOperationDispatcher(OkHttpClient(), BuildConfig.FOMO_SUPABASE_URL, tokenProvider),
      listener
    )
  }

  fun socketUrl(): String = BuildConfig.FOMO_SOCKET_URL
}
