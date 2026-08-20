package app.organicmaps.chat.data

import android.content.Context
import app.organicmaps.chat.realtime.ChatCache
import app.organicmaps.chat.realtime.ChatConnectionState
import app.organicmaps.chat.realtime.ChatRealtimeEngine
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/** Process-wide chats stack: auth, cache, repository, and optional Socket.IO transport. */
class ChatServices private constructor(context: Context) : ChatRealtimeEngine.Listener {
  interface Listener {
    fun onInboxHint()
    fun onConversationHint(conversationId: String)
    fun onTyping(conversationId: String, userId: String, isTyping: Boolean)
    fun onConnectionState(state: ChatConnectionState)
  }

  private val client = OkHttpClient()
  val session = FomoAuthSession(context)
  val auth = FomoAuthRepository(client, session)
  private val engine: ChatRealtimeEngine? = ChatEngineFactory.create(context, { session.accessToken }, this)
  private val store: ChatCache = engine?.store ?: ChatCache(context)
  val repository = ChatRepository(
    OkHttpChatRpcClient.fromSession(client, session),
    session,
    auth,
    store,
    engine
  )
  private val listeners = CopyOnWriteArrayList<Listener>()
  var connectionState: ChatConnectionState = ChatConnectionState.offline
    private set

  init {
    FomoSupabase.init()
  }

  fun addListener(listener: Listener) { listeners.add(listener) }
  fun removeListener(listener: Listener) { listeners.remove(listener) }
  fun engine(): ChatRealtimeEngine? = engine

  fun onSignedIn() {
    connectIfPossible()
    engine?.flushOutbox()
  }

  fun onSignedOut() {
    engine?.onBackground()
  }

  fun onForeground() {
    connectIfPossible()
    engine?.onForeground()
  }

  fun onBackground() {
    engine?.onBackground()
  }

  private fun connectIfPossible() {
    if (engine == null || !session.isSignedIn) return
    val socket = ChatEngineFactory.socketUrl()
    if (socket.isNotEmpty()) engine.connect(socket) else engine.flushOutbox()
  }

  override fun onConnectionStateChanged(state: ChatConnectionState) {
    connectionState = state
    listeners.forEach { it.onConnectionState(state) }
  }

  override fun onTyping(conversationId: String, userId: String, isTyping: Boolean, expiresInMs: Long) {
    listeners.forEach { it.onTyping(conversationId, userId, isTyping) }
  }

  override fun onMessageHint(message: JSONObject) {
    val parsed = ChatJson.parseMessage(message, session.userId)
    if (parsed != null) store.upsertMessage(parsed)
    val conversationId = parsed?.conversationId
      ?: message.optString("conversationId", message.optString("conversation_id", ""))
    listeners.forEach {
      it.onInboxHint()
      if (conversationId.isNotEmpty()) it.onConversationHint(conversationId)
    }
  }

  override fun onReceiptHint(receipt: JSONObject) {
    val conversationId = receipt.optString("conversationId", "")
    listeners.forEach {
      it.onInboxHint()
      if (conversationId.isNotEmpty()) it.onConversationHint(conversationId)
    }
  }

  override fun onCallSignal(signal: JSONObject) {}

  companion object {
    @Volatile private var instance: ChatServices? = null

    @JvmStatic
    fun get(context: Context): ChatServices {
      return instance ?: synchronized(this) {
        instance ?: ChatServices(context.applicationContext).also { instance = it }
      }
    }
  }
}
