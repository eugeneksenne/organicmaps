package app.organicmaps.chat.data

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chat.realtime.ChatConnectionState
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Message

/** Binds a conversation thread: cache-first open, optimistic send, realtime hints. */
class ChatConversationBinder(
  private val fragment: Fragment,
  private val root: View,
  private val conversationId: String,
  private val host: Host
) : ChatServices.Listener {
  interface Host {
    fun openCall(mode: String, name: String)
    fun openProfile(username: String)
    fun showNotice(message: String)
    fun openFeature(destination: String)
  }

  private val services = ChatServices.get(fragment.requireContext())
  private val repository = services.repository
  private val poll = Runnable { loop() }
  private var typingSent = false
  private val typingPeers = linkedSetOf<String>()
  private var replyTo: Message? = null
  private var messages: List<Message> = emptyList()
  private var search = ""
  private var lastTap = 0L

  fun start() {
    services.addListener(this)
    bindHeader()
    bindComposer()
    loop()
    if (!conversationId.startsWith("demo-")) services.engine()?.joinConversation(conversationId)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(view: View) {}
      override fun onViewDetachedFromWindow(view: View) {
        services.removeListener(this@ChatConversationBinder)
        root.removeCallbacks(poll)
        services.engine()?.setTyping(conversationId, false)
        services.engine()?.leaveConversation(conversationId)
        val input = root.findViewById<EditText>(R.id.conversation_input)
        repository.store.saveDraft(conversationId, input.text.toString(), replyTo?.id)
      }
    })
  }

  private fun bindHeader() {
    val conversation = repository.conversation(conversationId)
    val title = conversation?.title ?: conversationId
    root.findViewById<TextView>(R.id.conversation_name).text = title
    root.findViewById<TextView>(R.id.conversation_avatar).text = conversation?.initials() ?: "?"
    updatePresence(conversation)
    root.findViewById<View>(R.id.conversation_back).setOnClickListener {
      fragment.parentFragmentManager.popBackStack()
    }
    root.findViewById<View>(R.id.conversation_voice).setOnClickListener { host.openCall("outgoing_voice", title) }
    root.findViewById<View>(R.id.conversation_video).setOnClickListener { host.openCall("outgoing_video", title) }
    root.findViewById<View>(R.id.conversation_header).setOnClickListener {
      conversation?.peerUsername?.let { host.openProfile(it) }
    }
    root.findViewById<View>(R.id.conversation_menu).setOnClickListener { showMenu(it) }
    root.findViewById<View>(R.id.conversation_attach).setOnClickListener { showAttachSheet() }
    root.findViewById<View>(R.id.conversation_cancel_reply).setOnClickListener { setReply(null) }
    root.findViewById<EditText>(R.id.conversation_search).addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        search = s.toString().trim()
        renderMessages(messages, repository.conversation(conversationId))
      }
      override fun afterTextChanged(s: Editable) {}
    })
    repository.loadPresence(conversationId) { online, lastSeen ->
      runOnUi {
        val latest = repository.conversation(conversationId)
        root.findViewById<TextView>(R.id.conversation_presence).text =
          ChatLogic.presenceLabel(online, lastSeen, latest?.peerUsername, System.currentTimeMillis())
      }
    }
  }

  private fun updatePresence(conversation: Conversation?) {
    val presence = root.findViewById<TextView>(R.id.conversation_presence)
    if (conversation == null) {
      presence.setText(R.string.chats_connecting)
      return
    }
    presence.text = ChatLogic.presenceLabel(
      conversation.peerOnline, conversation.peerLastSeen, conversation.peerUsername, System.currentTimeMillis()
    )
  }

  private fun bindComposer() {
    val input = root.findViewById<EditText>(R.id.conversation_input)
    val send = root.findViewById<TextView>(R.id.conversation_send)
    val draft = repository.store.loadDraftState(conversationId)
    input.setText(draft.body)
    if (draft.replyToId != null) setReply(repository.findMessage(conversationId, draft.replyToId))
    send.text = if (input.text.isEmpty()) "🎤" else "↑"
    input.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        send.text = if (s.isEmpty()) "🎤" else "↑"
        val engine = services.engine() ?: return
        if (s.isNotEmpty() && !typingSent) {
          engine.setTyping(conversationId, true)
          typingSent = true
        } else if (s.isEmpty() && typingSent) {
          engine.setTyping(conversationId, false)
          typingSent = false
        }
      }
      override fun afterTextChanged(s: Editable) {}
    })
    send.setOnClickListener {
      val text = input.text.toString().trim()
      if (text.isEmpty()) {
        host.showNotice("Hold to record a voice note")
        return@setOnClickListener
      }
      input.setText("")
      repository.store.saveDraft(conversationId, "", null)
      repository.sendText(conversationId, text, replyTo?.id)
      setReply(null)
      refreshMessages()
    }
  }

  private fun loop() {
    refreshMessages()
    val delay = if (services.connectionState == ChatConnectionState.connected) 20_000L else 3_000L
    root.postDelayed(poll, delay)
  }

  private fun refreshMessages() {
    if (!fragment.isAdded) return
    repository.loadMessages(conversationId, object : ChatRepository.MessagesCallback {
      override fun onLoaded(messages: List<Message>, conversation: Conversation?) {
        runOnUi {
          this@ChatConversationBinder.messages = messages
          renderMessages(messages, conversation)
        }
      }
      override fun onStatus(status: String) {
        runOnUi {
          val view = root.findViewById<TextView>(R.id.conversation_status)
          view.text = status
          view.visibility = if (status.isEmpty()) View.GONE else View.VISIBLE
        }
      }
    })
  }

  private fun renderMessages(messages: List<Message>, conversation: Conversation?) {
    if (conversation != null) {
      root.findViewById<TextView>(R.id.conversation_name).text = conversation.title
      root.findViewById<TextView>(R.id.conversation_avatar).text = conversation.initials()
      if (!typingSent) updatePresence(conversation)
    }
    val list = root.findViewById<LinearLayout>(R.id.conversation_messages)
    list.removeAllViews()
    val visible = messages.filter {
      search.isEmpty() || it.body?.lowercase()?.contains(search.lowercase()) == true
    }
    if (visible.isEmpty()) {
      val empty = TextView(fragment.requireContext())
      empty.setText(R.string.chats_say_hello)
      empty.setTextColor(0x99000000.toInt())
      empty.gravity = Gravity.CENTER
      list.addView(empty)
      return
    }
    visible.forEach { list.addView(bubble(it)) }
    val scroll = root.findViewById<ScrollView>(R.id.conversation_scroll)
    if (search.isEmpty()) scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
  }

  private fun bubble(message: Message): View {
    val view = LayoutInflater.from(fragment.requireContext())
      .inflate(R.layout.chat_bubble, root.findViewById(R.id.conversation_messages), false)
    view.tag = message.id
    val text = view.findViewById<TextView>(R.id.chat_bubble_text)
    val meta = view.findViewById<TextView>(R.id.chat_bubble_meta)
    val reply = view.findViewById<TextView>(R.id.chat_bubble_reply)
    val reactions = view.findViewById<TextView>(R.id.chat_bubble_reactions)
    val params = text.layoutParams as LinearLayout.LayoutParams
    if (message.mine) {
      params.gravity = Gravity.END
      text.setBackgroundResource(R.drawable.chat_category_active)
      text.setTextColor(Color.WHITE)
      (meta.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
      (reply.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
      (reactions.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
    } else {
      params.gravity = Gravity.START
      text.setBackgroundResource(R.drawable.chat_avatar)
      text.setTextColor(0xDE000000.toInt())
    }
    text.layoutParams = params
    text.text = if (message.deleted || message.body == null) fragment.getString(R.string.chats_deleted) else message.body
    if (!message.replyPreview.isNullOrEmpty()) {
      reply.visibility = View.VISIBLE
      reply.text = message.replyPreview
    }
    if (message.reactions.isNotEmpty()) {
      reactions.visibility = View.VISIBLE
      reactions.text = message.reactions
    }
    val ticks = ChatLogic.deliveryTick(message.state, message.mine)
    val edited = if (message.editedAt > 0) " · ${fragment.getString(R.string.chats_edited)}" else ""
    meta.text = ChatLogic.relativeTime(message.sentAt, System.currentTimeMillis()) + edited + ticks
    view.setOnLongClickListener {
      showMessageActions(message)
      true
    }
    view.setOnClickListener {
      val now = SystemClock.uptimeMillis()
      if (now - lastTap < 280L) applyReaction(message, "❤️")
      lastTap = now
    }
    return view
  }

  private fun showMessageActions(message: Message) {
    val menu = PopupMenu(fragment.requireContext(), root.findViewById(R.id.conversation_input))
    menu.menu.add(fragment.getString(R.string.chats_react))
    menu.menu.add(fragment.getString(R.string.chats_reply))
    if (!message.deleted && message.body != null) menu.menu.add(fragment.getString(R.string.chats_copy))
    if (message.mine && !message.deleted) {
      menu.menu.add(fragment.getString(R.string.chats_edit))
      menu.menu.add(fragment.getString(R.string.chats_delete))
    }
    menu.setOnMenuItemClickListener { item ->
      when (item.title) {
        fragment.getString(R.string.chats_react) -> showReactions(message)
        fragment.getString(R.string.chats_reply) -> setReply(message)
        fragment.getString(R.string.chats_copy) -> copy(message)
        fragment.getString(R.string.chats_edit) -> edit(message)
        fragment.getString(R.string.chats_delete) -> confirmDelete(message)
      }
      true
    }
    menu.show()
  }

  private fun showReactions(message: Message) {
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.chats_react)
      .setItems(ChatLogic.reactions.toTypedArray()) { _, which -> applyReaction(message, ChatLogic.reactions[which]) }
      .show()
  }

  private fun applyReaction(message: Message, emoji: String) {
    repository.toggleReaction(conversationId, message.id, emoji)
    refreshMessages()
  }

  private fun setReply(message: Message?) {
    replyTo = message
    val bar = root.findViewById<View>(R.id.conversation_reply)
    val label = root.findViewById<TextView>(R.id.conversation_reply_label)
    if (message?.body == null) {
      bar.visibility = View.GONE
      return
    }
    bar.visibility = View.VISIBLE
    label.text = fragment.getString(R.string.chats_replying, message.senderName) + "\n" + message.body
  }

  private fun copy(message: Message) {
    val body = message.body ?: return
    val clipboard = fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", body))
    Toast.makeText(fragment.requireContext(), R.string.chats_copied, Toast.LENGTH_SHORT).show()
  }

  private fun edit(message: Message) {
    val input = EditText(fragment.requireContext())
    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    input.setText(message.body)
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.chats_edit)
      .setView(input)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.chats_edit) { _, _ ->
        val body = input.text.toString().trim()
        if (body.isNotEmpty()) {
          repository.editMessage(conversationId, message.id, body)
          refreshMessages()
        }
      }
      .show()
  }

  private fun confirmDelete(message: Message) {
    AlertDialog.Builder(fragment.requireContext())
      .setMessage(R.string.chats_delete_confirm)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.chats_delete) { _, _ ->
        repository.deleteMessage(conversationId, message.id)
        refreshMessages()
      }
      .show()
  }

  private fun showAttachSheet() {
    val items = arrayOf(
      fragment.getString(R.string.chats_attach_camera),
      fragment.getString(R.string.chats_attach_gallery),
      fragment.getString(R.string.chats_attach_document),
      fragment.getString(R.string.chats_attach_venue),
      fragment.getString(R.string.chats_attach_event),
      fragment.getString(R.string.chats_attach_location),
      fragment.getString(R.string.chats_attach_contact)
    )
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.chats_attach)
      .setItems(items) { _, which ->
        when (which) {
          0 -> host.openFeature("camera")
          1, 2 -> host.showNotice("${items[which]} will send after the media pipeline is enabled.")
          3 -> {
            repository.sendCard(conversationId, "venue", "📍 Cocoon Nightclub  ✓\n0.8 km • Open now • Crowd: High")
            refreshMessages()
          }
          4 -> {
            repository.sendCard(conversationId, "event", "🎟️ Amapiano Fridays\nTonight 22:00 • Truth Nightclub")
            refreshMessages()
          }
          5 -> {
            repository.sendCard(conversationId, "location", locationBody())
            refreshMessages()
          }
          6 -> pickAndSendContact()
        }
      }
      .show()
  }

  private fun pickAndSendContact() {
    DeviceContacts.request(fragment) { showContactPicker { contact ->
      repository.sendCard(conversationId, "contact", ChatLogic.contactCard(contact))
      refreshMessages()
    } }
  }

  private fun showContactPicker(onPicked: (app.organicmaps.chats.ChatModels.Contact) -> Unit) {
    val context = fragment.requireContext()
    if (!DeviceContacts.hasPermission(context)) {
      Toast.makeText(context, R.string.contacts_permission, Toast.LENGTH_LONG).show()
      return
    }
    val contacts = DeviceContacts.load(context)
    if (contacts.isEmpty()) {
      Toast.makeText(context, R.string.contacts_empty, Toast.LENGTH_SHORT).show()
      return
    }
    val input = EditText(context)
    input.hint = fragment.getString(R.string.contacts_search)
    input.inputType = InputType.TYPE_CLASS_TEXT
    val results = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val container = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(input)
      val scroll = ScrollView(context)
      scroll.addView(results)
      addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600))
    }
    val dialog = AlertDialog.Builder(context)
      .setTitle(R.string.chats_attach_contact)
      .setView(container)
      .setNegativeButton(R.string.cancel, null)
      .show()
    fun fill(query: String) {
      results.removeAllViews()
      contacts.filter { ChatLogic.matchesContactQuery(it, query) }.take(40).forEach { contact ->
        val row = TextView(context)
        row.setPadding(24, 24, 24, 24)
        row.text = contact.name + "\n" + listOfNotNull(contact.phone, contact.email).joinToString(" · ")
        row.setTextColor(0xDE000000.toInt())
        row.setOnClickListener {
          dialog.dismiss()
          onPicked(contact)
        }
        results.addView(row)
      }
    }
    fill("")
    input.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { fill(s.toString()) }
      override fun afterTextChanged(s: Editable) {}
    })
  }

  private fun locationBody(): String {
    return try {
      val manager = fragment.requireContext().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
      val location = manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
      if (location != null) "📍 ${location.latitude}, ${location.longitude}" else "📍 Current location"
    } catch (_: SecurityException) {
      "📍 Current location"
    }
  }

  private fun showMenu(anchor: View) {
    val conversation = repository.conversation(conversationId)
    val menu = PopupMenu(fragment.requireContext(), anchor)
    menu.menu.add("View Profile")
    menu.menu.add(fragment.getString(R.string.chats_search_conversation))
    menu.menu.add(if (conversation?.muted == true) fragment.getString(R.string.chats_unmute) else fragment.getString(R.string.chats_mute))
    menu.menu.add(if (conversation?.pinned == true) fragment.getString(R.string.chats_unpin) else fragment.getString(R.string.chats_pin))
    menu.menu.add(fragment.getString(R.string.chats_clear))
    menu.menu.add("NightGuard")
    menu.menu.add("Walk Me Home")
    menu.menu.add("Safety Check")
    menu.menu.add("Buddy Pair")
    if (conversation?.peerId != null) menu.menu.add(fragment.getString(R.string.chats_block))
    menu.menu.add(fragment.getString(R.string.chats_report))
    if (conversation?.kind == "group") menu.menu.add(fragment.getString(R.string.chats_leave))
    menu.setOnMenuItemClickListener { item ->
      when (item.title) {
        "View Profile" -> conversation?.peerUsername?.let { host.openProfile(it) }
        fragment.getString(R.string.chats_search_conversation) -> {
          val searchView = root.findViewById<View>(R.id.conversation_search)
          searchView.visibility = if (searchView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        fragment.getString(R.string.chats_mute), fragment.getString(R.string.chats_unmute) ->
          repository.setMuted(conversationId, conversation?.muted != true)
        fragment.getString(R.string.chats_pin), fragment.getString(R.string.chats_unpin) ->
          repository.setPinned(conversationId, conversation?.pinned != true)
        fragment.getString(R.string.chats_clear) -> {
          repository.clearHistory(conversationId)
          refreshMessages()
        }
        "NightGuard" -> host.openFeature("nightguard")
        "Walk Me Home" -> host.openFeature("walk_home")
        "Safety Check" -> host.openFeature("safety_check")
        "Buddy Pair" -> host.openFeature("buddy_pair")
        fragment.getString(R.string.chats_block) -> {
          conversation?.peerId?.let { repository.blockPeer(it, conversationId) }
          fragment.parentFragmentManager.popBackStack()
        }
        fragment.getString(R.string.chats_report) -> {
          repository.reportPeer(conversationId, conversation?.peerId, "abuse")
          host.showNotice(fragment.getString(R.string.chats_report))
        }
        fragment.getString(R.string.chats_leave) -> {
          repository.leaveConversation(conversationId)
          fragment.parentFragmentManager.popBackStack()
        }
      }
      true
    }
    menu.show()
  }

  private fun runOnUi(action: () -> Unit) {
    if (fragment.isAdded) fragment.requireActivity().runOnUiThread(action)
  }

  override fun onInboxHint() {}
  override fun onConversationHint(id: String) {
    if (conversationId == id) refreshMessages()
  }
  override fun onTyping(id: String, userId: String, isTyping: Boolean) {
    if (conversationId != id || !fragment.isAdded) return
    runOnUi {
      if (isTyping) typingPeers.add(userId) else typingPeers.remove(userId)
      val presence = root.findViewById<TextView>(R.id.conversation_presence)
      val label = ChatLogic.typingLabel(typingPeers.size)
      if (label.isEmpty()) updatePresence(repository.conversation(conversationId))
      else presence.text = label
    }
  }
  override fun onConnectionState(state: ChatConnectionState) {
    runOnUi {
      val view = root.findViewById<TextView>(R.id.conversation_status)
      val text = when (state) {
        ChatConnectionState.offline, ChatConnectionState.failed ->
          fragment.getString(R.string.chats_offline)
        ChatConnectionState.connecting, ChatConnectionState.reconnecting ->
          fragment.getString(R.string.chats_connecting)
        else -> ""
      }
      if (text.isNotEmpty()) {
        view.text = text
        view.visibility = View.VISIBLE
      }
    }
  }
}
