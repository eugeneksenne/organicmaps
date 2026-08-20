package app.organicmaps.chat.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.BuildConfig;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import okhttp3.OkHttpClient;

/** Builds the realtime engine using public per-developer configuration from fomo.properties. */
public final class ChatEngineFactory
{
  private ChatEngineFactory() {}

  public static boolean isConfigured()
  {
    return !BuildConfig.FOMO_SUPABASE_URL.isEmpty() && !BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty();
  }

  @Nullable
  public static ChatRealtimeEngine create(@NonNull Context context,
                                          @NonNull ChatRealtimeEngine.AccessTokenProvider tokenProvider,
                                          @NonNull ChatRealtimeEngine.Listener listener)
  {
    if (!isConfigured())
      return null;
    final OkHttpClient client = new OkHttpClient();
    return new ChatRealtimeEngine(context, tokenProvider,
                                  new SupabaseChatOperationDispatcher(client, BuildConfig.FOMO_SUPABASE_URL,
                                                                      tokenProvider),
                                  listener);
  }

  @NonNull
  public static String socketUrl() { return BuildConfig.FOMO_SOCKET_URL; }
}
