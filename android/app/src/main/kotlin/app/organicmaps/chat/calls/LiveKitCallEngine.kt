package app.organicmaps.chat.calls

import android.content.Context
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns one real LiveKit/WebRTC room session. Tokens are obtained from the authenticated
 * Supabase Edge Function; API secrets never enter the Android process.
 */
class LiveKitCallEngine(
  context: Context,
  private val listener: Listener
) {
  interface Listener {
    fun onConnected()
    fun onDisconnected(reason: String?)
    fun onFailure(error: Throwable)
  }

  private val applicationContext = context.applicationContext
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var room: Room? = null

  fun connect(url: String, participantToken: String, videoEnabled: Boolean) {
    disconnect()
    val newRoom = LiveKit.create(applicationContext)
    room = newRoom
    scope.launch {
      try {
        newRoom.connect(url, participantToken)
        newRoom.localParticipant.setMicrophoneEnabled(true)
        if (videoEnabled) newRoom.localParticipant.setCameraEnabled(true)
        listener.onConnected()
      } catch (error: Throwable) {
        listener.onFailure(error)
      }
    }
  }

  fun setMuted(muted: Boolean) {
    scope.launch { room?.localParticipant?.setMicrophoneEnabled(!muted) }
  }

  fun setCameraEnabled(enabled: Boolean) {
    scope.launch { room?.localParticipant?.setCameraEnabled(enabled) }
  }

  fun disconnect() {
    val activeRoom = room ?: return
    room = null
    scope.launch {
      try {
        activeRoom.disconnect()
        listener.onDisconnected(null)
      } catch (error: Throwable) {
        listener.onFailure(error)
      }
    }
  }

  fun close() {
    disconnect()
    scope.cancel()
  }
}
