package app.organicmaps.chat.realtime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Socket.IO transport for low-latency chat hints. Supabase remains the source of truth for all
 * persistent writes. This class never treats a socket acknowledgement as a persisted message.
 */
public class ChatRealtimeEngine
{
  public interface AccessTokenProvider { @Nullable String getAccessToken(); }
  public interface OperationDispatcher { boolean dispatch(@NonNull ChatLocalStore.PendingOperation operation); }
  public interface Listener
  {
    void onConnectionStateChanged(@NonNull ChatConnectionState state);
    void onTyping(@NonNull String conversationId, @NonNull String userId, boolean isTyping, long expiresInMs);
    void onMessageHint(@NonNull JSONObject message);
    void onReceiptHint(@NonNull JSONObject receipt);
    void onCallSignal(@NonNull JSONObject signal);
  }

  private static final long INITIAL_RETRY_MS = 1_000L;
  private static final long MAX_RETRY_MS = 30_000L;
  private final Handler mMainHandler = new Handler(Looper.getMainLooper());
  private final ChatLocalStore mStore;
  private final AccessTokenProvider mTokenProvider;
  private final OperationDispatcher mDispatcher;
  private final Listener mListener;
  @Nullable private Socket mSocket;
  private boolean mBackground;
  private long mRetryMs = INITIAL_RETRY_MS;

  public ChatRealtimeEngine(@NonNull Context context, @NonNull AccessTokenProvider tokenProvider,
                            @NonNull OperationDispatcher dispatcher, @NonNull Listener listener)
  {
    mStore = new ChatLocalStore(context);
    mTokenProvider = tokenProvider;
    mDispatcher = dispatcher;
    mListener = listener;
  }

  public void connect(@NonNull String endpoint)
  {
    disconnectSocket();
    final String token = mTokenProvider.getAccessToken();
    if (token == null || token.isEmpty()) { updateState(ChatConnectionState.offline); return; }
    try
    {
      final IO.Options options = new IO.Options();
      options.reconnection = false; // Reconnect is owned here for app lifecycle awareness.
      options.auth = new HashMap<>();
      options.auth.put("accessToken", token);
      mSocket = IO.socket(endpoint, options);
      registerListeners(mSocket);
      updateState(ChatConnectionState.connecting);
      mSocket.connect();
    }
    catch (URISyntaxException e) { updateState(ChatConnectionState.failed); }
  }

  public void onForeground()
  {
    mBackground = false;
    if (mSocket == null || !mSocket.connected()) scheduleReconnect();
  }

  public void onBackground()
  {
    mBackground = true;
    disconnectSocket();
    updateState(ChatConnectionState.background);
  }

  public void close()
  {
    mMainHandler.removeCallbacksAndMessages(null);
    disconnectSocket();
    mStore.close();
  }

  public void joinConversation(@NonNull String conversationId)
  {
    try { emit("conversation:join", new JSONObject().put("conversationId", conversationId)); }
    catch (JSONException ignored) {}
  }

  public void leaveConversation(@NonNull String conversationId)
  {
    try { emit("conversation:leave", new JSONObject().put("conversationId", conversationId)); }
    catch (JSONException ignored) {}
  }

  public void setTyping(@NonNull String conversationId, boolean isTyping)
  {
    try { emit("typing:update", new JSONObject().put("conversationId", conversationId).put("isTyping", isTyping)); }
    catch (JSONException ignored) {}
  }

  /** Queue a persistent operation first; a repository writes it to Supabase during flush. */
  public void queueOperation(@NonNull String type, @NonNull JSONObject payload)
  {
    mStore.enqueue(UUID.randomUUID().toString(), type, payload.toString());
    flushOutbox();
  }

  public void flushOutbox()
  {
    final List<ChatLocalStore.PendingOperation> operations = mStore.pendingOperations(50);
    for (ChatLocalStore.PendingOperation operation : operations)
    {
      if (mDispatcher.dispatch(operation)) mStore.markComplete(operation.id);
      else mStore.markAttempted(operation.id);
    }
  }

  private void registerListeners(@NonNull Socket socket)
  {
    socket.on(Socket.EVENT_CONNECT, args -> {
      mRetryMs = INITIAL_RETRY_MS;
      updateState(ChatConnectionState.connected);
      flushOutbox();
    });
    socket.on(Socket.EVENT_DISCONNECT, args -> { if (!mBackground) scheduleReconnect(); });
    socket.on(Socket.EVENT_CONNECT_ERROR, args -> { if (!mBackground) scheduleReconnect(); });
    socket.on("typing:update", args -> withObject(args, object -> mListener.onTyping(object.optString("conversationId"), object.optString("userId"), object.optBoolean("isTyping"), object.optLong("expiresInMs", 5_000L))));
    socket.on("message:published", args -> withObject(args, mListener::onMessageHint));
    socket.on("receipt:hint", args -> withObject(args, mListener::onReceiptHint));
    socket.on("call:signal", args -> withObject(args, mListener::onCallSignal));
  }

  private interface ObjectConsumer { void accept(@NonNull JSONObject object); }
  private static void withObject(@NonNull Object[] args, @NonNull ObjectConsumer consumer)
  {
    if (args.length > 0 && args[0] instanceof JSONObject) consumer.accept((JSONObject) args[0]);
  }

  private void emit(@NonNull String event, @NonNull JSONObject payload)
  {
    if (mSocket != null && mSocket.connected()) mSocket.emit(event, payload);
  }

  private void scheduleReconnect()
  {
    updateState(ChatConnectionState.reconnecting);
    final long delay = mRetryMs + (long) (Math.random() * 250L);
    mRetryMs = Math.min(mRetryMs * 2, MAX_RETRY_MS);
    mMainHandler.removeCallbacksAndMessages(null);
    mMainHandler.postDelayed(() -> { if (!mBackground && mSocket != null) mSocket.connect(); }, delay);
  }

  private void disconnectSocket()
  {
    if (mSocket != null) { mSocket.disconnect(); mSocket.off(); mSocket = null; }
  }

  private void updateState(@NonNull ChatConnectionState state)
  {
    mMainHandler.post(() -> mListener.onConnectionStateChanged(state));
  }
}
