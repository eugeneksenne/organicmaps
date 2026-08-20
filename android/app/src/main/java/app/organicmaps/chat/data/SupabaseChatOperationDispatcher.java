package app.organicmaps.chat.data;

import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
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

/** Applies durable operations through the authenticated chat RPC. */
public class SupabaseChatOperationDispatcher implements ChatRealtimeEngine.OperationDispatcher
{
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient;
  private final String mRpcEndpoint;
  private final String mFunctionEndpoint;
  private final ChatRealtimeEngine.AccessTokenProvider mTokens;

  public SupabaseChatOperationDispatcher(@NonNull OkHttpClient client, @NonNull String supabaseUrl,
                                         @NonNull ChatRealtimeEngine.AccessTokenProvider tokens)
  {
    mClient = client;
    final String root = supabaseUrl.replaceAll("/+$", "");
    mRpcEndpoint = root + "/rest/v1/rpc/apply_chat_operation";
    mFunctionEndpoint = root + "/functions/v1/chat-operation";
    mTokens = tokens;
  }

  @Override
  public boolean dispatch(@NonNull ChatLocalStore.PendingOperation operation)
  {
    final String token = mTokens.getAccessToken();
    if (token == null || token.isEmpty())
      return false;
    try
    {
      final JSONObject payload = new JSONObject(operation.payload);
      final JSONObject rpcBody = new JSONObject()
          .put("operation_id", operation.id)
          .put("operation_type", operation.type)
          .put("payload", payload);
      if (post(mRpcEndpoint, token, rpcBody.toString()))
        return true;
      final JSONObject functionBody = new JSONObject()
          .put("id", operation.id).put("type", operation.type).put("payload", payload);
      return post(mFunctionEndpoint, token, functionBody.toString());
    }
    catch (JSONException e)
    {
      return false;
    }
  }

  private boolean post(@NonNull String url, @NonNull String token, @NonNull String body)
  {
    final Request request = new Request.Builder().url(url)
        .header("Authorization", "Bearer " + token)
        .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("Content-Type", "application/json")
        .post(RequestBody.create(body, JSON))
        .build();
    try (Response response = mClient.newCall(request).execute())
    {
      return response.isSuccessful();
    }
    catch (IOException e)
    {
      return false;
    }
  }
}
