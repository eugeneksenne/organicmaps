package app.organicmaps.chat.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.chat.realtime.ChatConnectionState;
import app.organicmaps.chat.realtime.ChatLocalStore;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import java.util.concurrent.CopyOnWriteArrayList;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

/** Process-wide chats stack: auth, cache, repository, and optional Socket.IO transport. */
public final class ChatServices implements ChatRealtimeEngine.Listener
{
  public interface Listener
  {
    void onInboxHint();
    void onConversationHint(@NonNull String conversationId);
    void onTyping(@NonNull String conversationId, @NonNull String userId, boolean isTyping);
    void onConnectionState(@NonNull ChatConnectionState state);
  }

  private static ChatServices sInstance;
  private final OkHttpClient mClient = new OkHttpClient();
  private final FomoAuthSession mSession;
  private final FomoAuthRepository mAuth;
  private final ChatLocalStore mStore;
  @Nullable private ChatRealtimeEngine mEngine;
  private final ChatRepository mRepository;
  private final CopyOnWriteArrayList<Listener> mListeners = new CopyOnWriteArrayList<>();
  @NonNull private ChatConnectionState mState = ChatConnectionState.offline;

  private ChatServices(@NonNull Context context)
  {
    final Context app = context.getApplicationContext();
    mSession = new FomoAuthSession(app);
    mAuth = new FomoAuthRepository(mClient, mSession);
    mEngine = ChatEngineFactory.create(app, mSession::getAccessToken, this);
    mStore = mEngine != null ? mEngine.store() : new ChatLocalStore(app);
    mRepository = new ChatRepository(mClient, mSession, mAuth, mStore, mEngine);
    connectIfPossible();
  }

  @NonNull
  public static synchronized ChatServices get(@NonNull Context context)
  {
    if (sInstance == null)
      sInstance = new ChatServices(context);
    return sInstance;
  }

  @NonNull public ChatRepository repository() { return mRepository; }
  @NonNull public FomoAuthRepository auth() { return mAuth; }
  @NonNull public FomoAuthSession session() { return mSession; }
  @Nullable public ChatRealtimeEngine engine() { return mEngine; }
  @NonNull public ChatConnectionState connectionState() { return mState; }

  public void addListener(@NonNull Listener listener) { mListeners.add(listener); }
  public void removeListener(@NonNull Listener listener) { mListeners.remove(listener); }

  public void onSignedIn()
  {
    connectIfPossible();
    if (mEngine != null)
      mEngine.flushOutbox();
  }

  public void onSignedOut()
  {
    if (mEngine != null)
      mEngine.onBackground();
  }

  public void onForeground()
  {
    connectIfPossible();
    if (mEngine != null)
      mEngine.onForeground();
  }

  public void onBackground()
  {
    if (mEngine != null)
      mEngine.onBackground();
  }

  private void connectIfPossible()
  {
    if (mEngine == null || !mSession.isSignedIn())
      return;
    final String socket = ChatEngineFactory.socketUrl();
    if (!socket.isEmpty())
      mEngine.connect(socket);
    else
      mEngine.flushOutbox();
  }

  @Override
  public void onConnectionStateChanged(@NonNull ChatConnectionState state)
  {
    mState = state;
    for (Listener listener : mListeners)
      listener.onConnectionState(state);
  }

  @Override
  public void onTyping(@NonNull String conversationId, @NonNull String userId, boolean isTyping, long expiresInMs)
  {
    for (Listener listener : mListeners)
      listener.onTyping(conversationId, userId, isTyping);
  }

  @Override
  public void onMessageHint(@NonNull JSONObject message)
  {
    final String conversationId = message.optString("conversationId", message.optString("conversation_id", ""));
    for (Listener listener : mListeners)
    {
      listener.onInboxHint();
      if (!conversationId.isEmpty())
        listener.onConversationHint(conversationId);
    }
  }

  @Override
  public void onReceiptHint(@NonNull JSONObject receipt)
  {
    final String conversationId = receipt.optString("conversationId", "");
    for (Listener listener : mListeners)
    {
      listener.onInboxHint();
      if (!conversationId.isEmpty())
        listener.onConversationHint(conversationId);
    }
  }

  @Override
  public void onCallSignal(@NonNull JSONObject signal) {}
}
