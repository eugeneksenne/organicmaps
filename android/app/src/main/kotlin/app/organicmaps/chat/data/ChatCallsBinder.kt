package app.organicmaps.chat.data

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.CallRecord
import app.organicmaps.chats.ChatModels.Conversation

/** Calls tab: favorites, filters, search, history, and start-call picker. */
class ChatCallsBinder(
  private val fragment: Fragment,
  private val root: View,
  private val host: Host
) : ChatServices.Listener {
  interface Host {
    fun openCall(mode: String, name: String)
    fun openConversation(conversationId: String)
  }

  private val services = ChatServices.get(fragment.requireContext())
  private var filter = "all"
  private var query = ""
  private var calls: List<CallRecord> = emptyList()
  private var conversations: List<Conversation> = emptyList()

  fun start() {
    services.addListener(this)
    root.findViewById<View>(R.id.calls_back).setOnClickListener {
      fragment.parentFragmentManager.popBackStack()
    }
    root.findViewById<View>(R.id.calls_search).setOnClickListener {
      val search = root.findViewById<EditText>(R.id.calls_search_input)
      search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    root.findViewById<View>(R.id.calls_start).setOnClickListener { showStartPicker() }
    root.findViewById<EditText>(R.id.calls_search_input).addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        query = s.toString().trim()
        render()
      }
      override fun afterTextChanged(s: Editable) {}
    })
    val filters = root.findViewById<LinearLayout>(R.id.call_filters)
    for (i in 0 until filters.childCount) {
      val chip = filters.getChildAt(i) as TextView
      chip.setOnClickListener {
        filter = chip.text.toString().lowercase()
        for (j in 0 until filters.childCount) {
          val other = filters.getChildAt(j) as TextView
          val active = other == chip
          other.setBackgroundResource(if (active) R.drawable.chat_category_active else R.drawable.camera_look)
          other.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xDE000000.toInt())
        }
        render()
      }
    }
    refresh()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) { services.removeListener(this@ChatCallsBinder) }
    })
  }

  private fun refresh() {
    services.repository.loadCalls(object : ChatRepository.CallsCallback {
      override fun onLoaded(loaded: List<CallRecord>) {
        fragment.requireActivity().runOnUiThread {
          calls = loaded
          render()
        }
      }
    })
    services.repository.loadInbox(object : ChatRepository.InboxCallback {
      override fun onLoaded(loaded: List<Conversation>, stories: List<app.organicmaps.chats.ChatModels.Story>, live: Boolean) {
        fragment.requireActivity().runOnUiThread {
          conversations = loaded
          renderFavorites()
        }
      }
      override fun onStatus(status: String) {}
    })
  }

  private fun renderFavorites() {
    val row = root.findViewById<LinearLayout>(R.id.calls_favorites) ?: return
    row.removeAllViews()
    val favorites = conversations.filter { it.kind == "direct" || it.kind == "group" }.take(8)
    root.findViewById<View>(R.id.calls_favorites_label).visibility =
      if (favorites.isEmpty()) View.GONE else View.VISIBLE
    val inflater = LayoutInflater.from(fragment.requireContext())
    for (conversation in favorites) {
      val item = inflater.inflate(R.layout.chat_story_item, row, false)
      item.findViewById<TextView>(R.id.chat_story_avatar).text = conversation.initials()
      item.findViewById<TextView>(R.id.chat_story_name).text = conversation.title.substringBefore(' ')
      item.setOnClickListener { host.openCall("outgoing_voice", conversation.title) }
      item.setOnLongClickListener {
        host.openCall("outgoing_video", conversation.title)
        true
      }
      row.addView(item)
    }
  }

  private fun render() {
    val list = root.findViewById<LinearLayout>(R.id.calls_list) ?: return
    list.removeAllViews()
    val inflater = LayoutInflater.from(fragment.requireContext())
    val visible = calls.filter { ChatLogic.matchesCallFilter(it, filter) }
      .filter { query.isEmpty() || it.title.lowercase().contains(query.lowercase()) }
    root.findViewById<View>(R.id.calls_empty).visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
    for (record in visible) {
      val row = inflater.inflate(R.layout.call_row, list as ViewGroup, false)
      row.findViewById<TextView>(R.id.call_row_avatar).text = record.title.take(1).uppercase()
      row.findViewById<TextView>(R.id.call_row_name).text = record.title
      val preview = ChatLogic.callPreview(record)
      val previewView = row.findViewById<TextView>(R.id.call_row_preview)
      previewView.text = preview
      if (record.status == "missed") previewView.setTextColor(0xFFD54242.toInt())
      row.findViewById<TextView>(R.id.call_row_time).text =
        ChatLogic.relativeTime(record.startedAt, System.currentTimeMillis())
      row.findViewById<View>(R.id.call_row_missed).visibility =
        if (record.status == "missed") View.VISIBLE else View.GONE
      val action = row.findViewById<TextView>(R.id.call_row_action)
      action.text = if (record.kind.contains("video")) "▣" else "☎"
      val mode = when {
        record.kind.startsWith("group") && record.kind.contains("video") -> "group_video"
        record.kind.startsWith("group") -> "group_voice"
        record.kind.contains("video") -> "outgoing_video"
        else -> "outgoing_voice"
      }
      action.setOnClickListener { host.openCall(mode, record.title) }
      row.setOnClickListener { host.openCall(mode, record.title) }
      row.setOnLongClickListener {
        host.openCall("incoming_" + if (record.kind.contains("video")) "video" else "voice", record.title)
        true
      }
      list.addView(row)
    }
  }

  private fun showStartPicker() {
    val names = conversations.map { it.title }.ifEmpty { calls.map { it.title }.distinct() }
    if (names.isEmpty()) {
      host.openCall("outgoing_voice", "FOMO Call")
      return
    }
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.call_choose_type)
      .setItems(names.toTypedArray()) { _, which ->
        val name = names[which]
        AlertDialog.Builder(fragment.requireContext())
          .setItems(arrayOf(fragment.getString(R.string.call_voice), fragment.getString(R.string.call_video))) { _, type ->
            val group = conversations.firstOrNull { it.title == name }?.kind == "group"
            val mode = when {
              group && type == 1 -> "group_video"
              group -> "group_voice"
              type == 1 -> "outgoing_video"
              else -> "outgoing_voice"
            }
            host.openCall(mode, name)
          }
          .show()
      }
      .show()
  }

  override fun onInboxHint() { refresh() }
  override fun onConversationHint(conversationId: String) {}
  override fun onTyping(conversationId: String, userId: String, isTyping: Boolean) {}
  override fun onConnectionState(state: app.organicmaps.chat.realtime.ChatConnectionState) {}
}
