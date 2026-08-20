package app.organicmaps.chat.realtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.UUID
import kotlin.math.min

/**
 * Socket.IO transport for low-latency chat hints. Supabase remains the source of truth for all
 * persistent writes. This class never treats a socket acknowledgement as a persisted message.
 */
class ChatRealtimeEngine(
  context: Context,
  private val tokenProvider: AccessTokenProvider,
  private val dispatcher: OperationDispatcher,
  private val listener: Listener
) {
  fun interface AccessTokenProvider {
    fun getAccessToken(): String?
  }

  fun interface OperationDispatcher {
    fun dispatch(operation: ChatCache.PendingOperation): Boolean
  }

  interface Listener {
    fun onConnectionStateChanged(state: ChatConnectionState)
    fun onTyping(conversationId: String, userId: String, isTyping: Boolean, expiresInMs: Long)
    fun onMessageHint(message: JSONObject)
    fun onReceiptHint(receipt: JSONObject)
    fun onCallSignal(signal: JSONObject)
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  val store = ChatCache(context)
  private var socket: Socket? = null
  private var background = false
  private var retryMs = INITIAL_RETRY_MS

  fun connect(endpoint: String) {
    disconnectSocket()
    val token = tokenProvider.getAccessToken()
    if (token.isNullOrEmpty()) {
      updateState(ChatConnectionState.offline)
      return
    }
    try {
      val options = IO.Options()
      options.reconnection = false
      options.auth = hashMapOf<String, String>("accessToken" to token)
      val created = IO.socket(endpoint, options)
      registerListeners(created)
      updateState(ChatConnectionState.connecting)
      created.connect()
      socket = created
    } catch (_: URISyntaxException) {
      updateState(ChatConnectionState.failed)
    }
  }

  fun onForeground() {
    background = false
    if (socket == null || socket?.connected() != true) scheduleReconnect()
  }

  fun onBackground() {
    background = true
    disconnectSocket()
    updateState(ChatConnectionState.background)
  }

  fun close() {
    mainHandler.removeCallbacksAndMessages(null)
    disconnectSocket()
    store.close()
  }

  fun joinConversation(conversationId: String) {
    emit("conversation:join", JSONObject().put("conversationId", conversationId))
  }

  fun leaveConversation(conversationId: String) {
    emit("conversation:leave", JSONObject().put("conversationId", conversationId))
  }

  fun setTyping(conversationId: String, isTyping: Boolean) {
    emit("typing:update", JSONObject().put("conversationId", conversationId).put("isTyping", isTyping))
  }

  fun queueOperation(type: String, payload: JSONObject) {
    queueOperation(UUID.randomUUID().toString(), type, payload)
  }

  fun queueOperation(id: String, type: String, payload: JSONObject) {
    store.enqueue(id, type, payload.toString())
    flushOutbox()
  }

  fun flushOutbox() {
    store.pendingOperations(50).forEach { operation ->
      if (dispatcher.dispatch(operation)) store.markComplete(operation.id)
      else store.markAttempted(operation.id)
    }
  }

  private fun registerListeners(socket: Socket) {
    socket.on(Socket.EVENT_CONNECT) {
      retryMs = INITIAL_RETRY_MS
      updateState(ChatConnectionState.connected)
      flushOutbox()
    }
    socket.on(Socket.EVENT_DISCONNECT) { if (!background) scheduleReconnect() }
    socket.on(Socket.EVENT_CONNECT_ERROR) { if (!background) scheduleReconnect() }
    socket.on("typing:update") { args ->
      withObject(args) { obj ->
        listener.onTyping(
          obj.optString("conversationId"),
          obj.optString("userId"),
          obj.optBoolean("isTyping"),
          obj.optLong("expiresInMs", 5_000L)
        )
      }
    }
    socket.on("message:published") { args -> withObject(args) { listener.onMessageHint(it) } }
    socket.on("message:edited") { args -> withObject(args) { listener.onMessageHint(it) } }
    socket.on("message:deleted") { args -> withObject(args) { listener.onMessageHint(it) } }
    socket.on("reaction:hint") { args -> withObject(args) { listener.onMessageHint(it) } }
    socket.on("receipt:hint") { args -> withObject(args) { listener.onReceiptHint(it) } }
    socket.on("presence:hint") { args -> withObject(args) { listener.onReceiptHint(it) } }
    socket.on("call:signal") { args -> withObject(args) { listener.onCallSignal(it) } }
  }

  private fun emit(event: String, payload: JSONObject) {
    val active = socket
    if (active != null && active.connected()) active.emit(event, payload)
  }

  private fun scheduleReconnect() {
    updateState(ChatConnectionState.reconnecting)
    val delay = retryMs + (Math.random() * 250L).toLong()
    retryMs = min(retryMs * 2, MAX_RETRY_MS)
    mainHandler.removeCallbacksAndMessages(null)
    mainHandler.postDelayed({ if (!background) socket?.connect() }, delay)
  }

  private fun disconnectSocket() {
    socket?.disconnect()
    socket?.off()
    socket = null
  }

  private fun updateState(state: ChatConnectionState) {
    mainHandler.post { listener.onConnectionStateChanged(state) }
  }

  companion object {
    private const val INITIAL_RETRY_MS = 1_000L
    private const val MAX_RETRY_MS = 30_000L

    private fun withObject(args: Array<Any>, consumer: (JSONObject) -> Unit) {
      if (args.isNotEmpty() && args[0] is JSONObject) consumer(args[0] as JSONObject)
    }
  }
}
