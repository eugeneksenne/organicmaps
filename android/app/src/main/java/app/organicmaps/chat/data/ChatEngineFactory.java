package app.organicmaps.chat.data;

import android.content.Context;
import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import okhttp3.OkHttpClient;

/** Builds the realtime engine using public per-developer configuration from fomo.properties. */
public final class ChatEngineFactory
{
  private ChatEngineFactory() {}

  @NonNull
  public static ChatRealtimeEngine create(@NonNull Context context,
                                          @NonNull ChatRealtimeEngine.AccessTokenProvider tokenProvider,
                                          @NonNull ChatRealtimeEngine.Listener listener)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SOCKET_URL.isEmpty())
      throw new IllegalStateException("Configure android/fomo.properties before enabling FOMO realtime");
    final OkHttpClient client = new OkHttpClient();
    return new ChatRealtimeEngine(context, tokenProvider,
                                  new SupabaseChatOperationDispatcher(client, BuildConfig.FOMO_SUPABASE_URL, tokenProvider),
                                  listener);
  }

  @NonNull
  public static String socketUrl() { return BuildConfig.FOMO_SOCKET_URL; }
}
