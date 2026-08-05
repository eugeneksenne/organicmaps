package app.organicmaps.chat.data;

import androidx.annotation.NonNull;
import app.organicmaps.chat.realtime.ChatLocalStore;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

/** Sends durable operations to the authenticated Supabase Edge Function. */
public class SupabaseChatOperationDispatcher implements ChatRealtimeEngine.OperationDispatcher
{
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient;
  private final String mEndpoint;
  private final ChatRealtimeEngine.AccessTokenProvider mTokens;

  public SupabaseChatOperationDispatcher(@NonNull OkHttpClient client, @NonNull String supabaseUrl,
                                         @NonNull ChatRealtimeEngine.AccessTokenProvider tokens)
  {
    mClient = client;
    mEndpoint = supabaseUrl.replaceAll("/+$", "") + "/functions/v1/chat-operation";
    mTokens = tokens;
  }

  @Override
  public boolean dispatch(@NonNull ChatLocalStore.PendingOperation operation)
  {
    final String token = mTokens.getAccessToken();
    if (token == null || token.isEmpty()) return false;
    try
    {
      final JSONObject body = new JSONObject().put("id", operation.id).put("type", operation.type)
                                                .put("payload", new JSONObject(operation.payload));
      final Request request = new Request.Builder().url(mEndpoint).header("Authorization", "Bearer " + token)
          .post(RequestBody.create(body.toString(), JSON)).build();
      try (Response response = mClient.newCall(request).execute()) { return response.isSuccessful(); }
    }
    catch (JSONException | IOException e) { return false; }
  }
}
