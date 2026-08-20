package app.organicmaps.chat.calls

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.BuildConfig
import app.organicmaps.R
import app.organicmaps.chat.data.ChatServices
import app.organicmaps.chats.ChatLogic
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Full-screen call UI: outgoing, incoming, active voice/video, group, reconnecting. */
class CallScreenBinder(
  private val fragment: Fragment,
  private val root: View,
  private val mode: String,
  private val peerName: String,
  private val host: Host
) : LiveKitCallEngine.Listener {
  interface Host {
    fun endCall()
    fun openChat()
    fun showNotice(message: String)
  }

  private val handler = Handler(Looper.getMainLooper())
  private val services = ChatServices.get(fragment.requireContext())
  private val audio = fragment.requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val engine = LiveKitCallEngine(fragment.requireContext(), this)
  private val video = mode.contains("video")
  private val group = mode.contains("group")
  private val incoming = mode.startsWith("incoming")
  private var phase = if (incoming) Phase.INCOMING else if (mode.startsWith("outgoing") || mode.isEmpty()) Phase.OUTGOING else Phase.ACTIVE
  private var muted = false
  private var speakerOn = video
  private var bluetoothOn = false
  private var cameraOn = video
  private var elapsed = 0
  private val tick = object : Runnable {
    override fun run() {
      if (phase != Phase.ACTIVE) return
      elapsed += 1
      root.findViewById<TextView>(R.id.call_screen_status).text = ChatLogic.liveTimer(elapsed)
      handler.postDelayed(this, 1_000L)
    }
  }

  fun start() {
    bindIdentity()
    bindControls()
    renderPhase()
    if (!incoming) connectMedia()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) {
        handler.removeCallbacks(tick)
        engine.close()
        audio.isSpeakerphoneOn = false
        audio.mode = AudioManager.MODE_NORMAL
      }
    })
  }

  private fun bindIdentity() {
    val safeName = peerName.ifEmpty { "FOMO Call" }
    root.findViewById<TextView>(R.id.call_screen_name).text = safeName
    root.findViewById<TextView>(R.id.call_screen_avatar).text = safeName.trim().take(1).uppercase()
    root.findViewById<TextView>(R.id.call_screen_type).text =
      if (group) fragment.getString(if (video) R.string.call_video else R.string.call_voice) + " · Group"
      else fragment.getString(if (video) R.string.call_video else R.string.call_voice)
    val nightguard = root.findViewById<View>(R.id.call_nightguard)
    nightguard.visibility = if (safeName.contains("NightGuard", ignoreCase = true) || safeName.contains("Buddy")) View.VISIBLE else View.GONE
  }

  private fun bindControls() {
    root.findViewById<View>(R.id.call_screen_mute).setOnClickListener {
      muted = !muted
      engine.setMuted(muted)
      (it as TextView).setText(if (muted) R.string.call_muted else R.string.call_mute)
    }
    root.findViewById<View>(R.id.call_screen_speaker).setOnClickListener {
      speakerOn = !speakerOn
      audio.mode = AudioManager.MODE_IN_COMMUNICATION
      audio.isSpeakerphoneOn = speakerOn
      (it as TextView).setText(if (speakerOn) R.string.call_speaker_on else R.string.call_speaker)
    }
    root.findViewById<View>(R.id.call_bluetooth).setOnClickListener {
      bluetoothOn = !bluetoothOn
      runCatching {
        if (bluetoothOn) audio.startBluetoothSco() else audio.stopBluetoothSco()
        audio.isBluetoothScoOn = bluetoothOn
      }
      (it as TextView).setText(if (bluetoothOn) R.string.call_bluetooth_on else R.string.call_bluetooth)
    }
    root.findViewById<View>(R.id.call_screen_camera).setOnClickListener {
      cameraOn = !cameraOn
      engine.setCameraEnabled(cameraOn)
      (it as TextView).setText(if (cameraOn) R.string.call_camera else R.string.call_camera_off)
      root.findViewById<View>(R.id.call_screen_local_preview).visibility =
        if (cameraOn && video && phase == Phase.ACTIVE) View.VISIBLE else View.GONE
    }
    root.findViewById<View>(R.id.call_flip).setOnClickListener { engine.switchCamera() }
    root.findViewById<View>(R.id.call_screen_participant_button).setOnClickListener { toggleParticipants() }
    root.findViewById<View>(R.id.call_chat).setOnClickListener { host.openChat() }
    root.findViewById<View>(R.id.call_screen_answer).setOnClickListener { answer() }
    root.findViewById<View>(R.id.call_screen_decline).setOnClickListener { hangUp() }
    root.findViewById<View>(R.id.call_reply_message).setOnClickListener { replyWithMessage() }
    root.findViewById<View>(R.id.call_silence).setOnClickListener {
      host.showNotice(fragment.getString(R.string.call_silence))
      root.findViewById<View>(R.id.call_incoming_extras).visibility = View.GONE
    }
    root.findViewById<View>(R.id.call_sheet_close).setOnClickListener {
      root.findViewById<View>(R.id.call_participants_sheet).visibility = View.GONE
    }
  }

  private fun renderPhase() {
    val incomingUi = phase == Phase.INCOMING
    val active = phase == Phase.ACTIVE
    val videoActive = video && (active || phase == Phase.OUTGOING)
    root.findViewById<View>(R.id.call_screen_answer).visibility = if (incomingUi) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_incoming_extras).visibility = if (incomingUi) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_screen_camera).visibility = if (video) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_flip).visibility = if (video) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_screen_participant_button).visibility = if (group) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_screen_local_preview).visibility =
      if (videoActive && cameraOn && !incomingUi) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_remote_video).visibility = if (video && active) View.VISIBLE else View.GONE
    root.findViewById<View>(R.id.call_screen_avatar).visibility = if (video && active) View.GONE else View.VISIBLE
    val participants = root.findViewById<TextView>(R.id.call_screen_participants)
    participants.visibility = if (group) View.VISIBLE else View.GONE
    if (group) participants.text = "8 participants • 3 speaking"
    val status = root.findViewById<TextView>(R.id.call_screen_status)
    status.text = when (phase) {
      Phase.INCOMING -> fragment.getString(if (video) R.string.call_incoming_video else R.string.call_incoming_voice)
      Phase.OUTGOING -> fragment.getString(R.string.call_calling)
      Phase.RECONNECTING -> fragment.getString(R.string.call_reconnecting)
      Phase.ENDED -> fragment.getString(R.string.call_ended)
      Phase.ACTIVE -> ChatLogic.liveTimer(elapsed)
    }
    audio.mode = AudioManager.MODE_IN_COMMUNICATION
    audio.isSpeakerphoneOn = speakerOn
    if (video) {
      root.findViewById<TextView>(R.id.call_screen_speaker).setText(R.string.call_speaker_on)
    }
  }

  private fun answer() {
    phase = Phase.ACTIVE
    renderPhase()
    startTimer()
    connectMedia()
  }

  private fun hangUp() {
    phase = Phase.ENDED
    handler.removeCallbacks(tick)
    engine.disconnect()
    services.repository.endCall(ChatLogic.callRoomId(peerName))
    host.endCall()
  }

  private fun startTimer() {
    handler.removeCallbacks(tick)
    handler.post(tick)
  }

  private fun connectMedia() {
    if (phase == Phase.INCOMING) return
    if (!ensurePermissions()) return
    if (phase == Phase.OUTGOING) {
      root.findViewById<TextView>(R.id.call_screen_status).setText(R.string.call_connecting_media)
    }
    val room = ChatLogic.callRoomId(peerName)
    services.repository.startCall(room, if (group && video) "group_video" else if (group) "group_voice" else if (video) "video" else "voice")
    fetchToken(room) { url, token ->
      if (url.isNullOrEmpty() || token.isNullOrEmpty()) {
        // UI-only demo path when LiveKit is not configured.
        val activate = {
          phase = Phase.ACTIVE
          renderPhase()
          startTimer()
        }
        if (phase == Phase.OUTGOING) handler.postDelayed(activate, 1_500L) else activate()
        return@fetchToken
      }
      engine.connect(url, token, video && cameraOn)
    }
  }

  private fun ensurePermissions(): Boolean {
    val needed = ArrayList<String>()
    if (fragment.requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      needed.add(Manifest.permission.RECORD_AUDIO)
    }
    if (video && fragment.requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      needed.add(Manifest.permission.CAMERA)
    }
    if (needed.isEmpty()) return true
    fragment.requestPermissions(needed.toTypedArray(), REQUEST)
    Toast.makeText(
      fragment.requireContext(),
      if (video) R.string.call_permission_video else R.string.call_permission,
      Toast.LENGTH_LONG
    ).show()
    return false
  }

  private fun fetchToken(room: String, done: (String?, String?) -> Unit) {
    val token = services.session.accessToken
    val rootUrl = BuildConfig.FOMO_SUPABASE_URL.trimEnd('/')
    if (token.isNullOrEmpty() || rootUrl.isEmpty()) {
      done(null, null)
      return
    }
    Thread {
      try {
        val body = JSONObject().put("room", room).put("publish", true).toString()
        val request = Request.Builder()
          .url("$rootUrl/functions/v1/livekit-token")
          .header("Authorization", "Bearer $token")
          .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
          .header("Content-Type", "application/json")
          .post(body.toRequestBody(JSON))
          .build()
        val client = okhttp3.OkHttpClient()
        client.newCall(request).execute().use { response ->
          val raw = response.body?.string().orEmpty()
          if (!response.isSuccessful) {
            fragment.requireActivity().runOnUiThread { done(null, null) }
            return@use
          }
          val json = JSONObject(raw)
          fragment.requireActivity().runOnUiThread {
            done(json.optString("url").ifEmpty { null }, json.optString("token").ifEmpty { null })
          }
        }
      } catch (_: Exception) {
        fragment.requireActivity().runOnUiThread { done(null, null) }
      }
    }.start()
  }

  private fun toggleParticipants() {
    val sheet = root.findViewById<View>(R.id.call_participants_sheet)
    if (sheet.visibility == View.VISIBLE) {
      sheet.visibility = View.GONE
      return
    }
    val list = root.findViewById<LinearLayout>(R.id.call_participants_list)
    list.removeAllViews()
    val names = if (group) listOf(peerName, "Nomsa", "Lerato", "You") else listOf(peerName, "You")
    names.forEachIndexed { index, name ->
      val row = TextView(fragment.requireContext())
      row.setPadding(0, 14, 0, 14)
      row.setTextColor(0xFFFFFFFF.toInt())
      row.textSize = 15f
      row.gravity = Gravity.START
      val speaking = index == 0
      val mutedMark = if (name == "You" && muted) " · muted" else if (index == 2) " · muted" else ""
      row.text = (if (speaking) "●  " else "○  ") + name + mutedMark
      list.addView(row)
    }
    sheet.visibility = View.VISIBLE
  }

  private fun replyWithMessage() {
    val input = EditText(fragment.requireContext())
    input.hint = fragment.getString(R.string.chats_message_hint)
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.call_reply_message)
      .setView(input)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.chats_send) { _, _ ->
        val text = input.text.toString().trim()
        if (text.isNotEmpty()) host.showNotice(text)
        hangUp()
      }
      .show()
  }

  override fun onConnected() {
    phase = Phase.ACTIVE
    renderPhase()
    startTimer()
    root.findViewById<TextView>(R.id.call_screen_quality).setText(R.string.call_quality_good)
  }

  override fun onDisconnected(reason: String?) {
    if (phase == Phase.ENDED) return
    phase = Phase.RECONNECTING
    renderPhase()
  }

  override fun onFailure(error: Throwable) {
    if (phase == Phase.ENDED) return
    phase = Phase.RECONNECTING
    renderPhase()
    root.findViewById<TextView>(R.id.call_screen_quality).setText(R.string.call_quality_poor)
  }

  private enum class Phase { OUTGOING, INCOMING, ACTIVE, RECONNECTING, ENDED }

  companion object {
    const val REQUEST = 214
    private val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
