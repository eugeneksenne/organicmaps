package app.organicmaps.chat.data

import app.organicmaps.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Shared supabase-kt client for FOMO Chats.
 *
 * Public URL + anon key only. Durable writes still go through PostgREST RPCs
 * ([ChatRpcClient]); this client is the KMP Auth/Realtime/Storage/Functions surface.
 */
object FomoSupabase {
  @Volatile
  var client: SupabaseClient? = null
    private set

  @Synchronized
  fun init() {
    if (client != null) return
    val url = BuildConfig.FOMO_SUPABASE_URL.trim()
    val key = BuildConfig.FOMO_SUPABASE_ANON_KEY.trim()
    if (url.isEmpty() || key.isEmpty()) return
    client = createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
      install(Auth)
      install(Postgrest)
      install(Realtime)
      install(Storage)
      install(Functions)
    }
  }
}
