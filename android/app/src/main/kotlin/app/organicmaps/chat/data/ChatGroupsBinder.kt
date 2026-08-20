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
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels
import app.organicmaps.chats.ChatModels.Conversation

/** Groups list backed by the same inbox repository as Chats. */
class ChatGroupsBinder(
  private val fragment: Fragment,
  private val root: View,
  private val host: Host
) : ChatServices.Listener {
  fun interface Host {
    fun openConversation(conversationId: String)
  }

  private val services = ChatServices.get(fragment.requireContext())
  private val repository = services.repository
  private var groups: List<Conversation> = emptyList()
  private var query = ""

  fun start() {
    services.addListener(this)
    root.findViewById<View>(R.id.groups_back).setOnClickListener {
      fragment.parentFragmentManager.popBackStack()
    }
    root.findViewById<View>(R.id.groups_search).setOnClickListener {
      val search = root.findViewById<EditText>(R.id.groups_search_input)
      search.visibility = if (search.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    root.findViewById<View>(R.id.groups_create).setOnClickListener { showCreateGroup() }
    root.findViewById<EditText>(R.id.groups_search_input).addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        query = s.toString().trim()
        render(groups)
      }
      override fun afterTextChanged(s: Editable) {}
    })
    refresh()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) { services.removeListener(this@ChatGroupsBinder) }
    })
  }

  private fun refresh() {
    repository.loadInbox(object : ChatRepository.InboxCallback {
      override fun onLoaded(conversations: List<Conversation>, stories: List<ChatModels.Story>, live: Boolean) {
        fragment.requireActivity().runOnUiThread {
          groups = conversations.filter { ChatLogic.matchesCategory(it, ChatModels.FILTER_GROUP) }
          render(groups)
        }
      }
      override fun onStatus(status: String) {}
    })
  }

  private fun render(all: List<Conversation>) {
    val pinned = root.findViewById<LinearLayout>(R.id.groups_pinned)
    val list = root.findViewById<LinearLayout>(R.id.groups_list)
    pinned.removeAllViews()
    list.removeAllViews()
    var pinnedCount = 0
    var visible = 0
    for (conversation in all) {
      if (!ChatLogic.matchesQuery(conversation, query)) continue
      visible++
      if (conversation.pinned) {
        pinned.addView(row(conversation))
        pinnedCount++
      } else list.addView(row(conversation))
    }
    root.findViewById<View>(R.id.groups_pinned_label).visibility = if (pinnedCount == 0) View.GONE else View.VISIBLE
    root.findViewById<View>(R.id.groups_empty).visibility = if (visible == 0) View.VISIBLE else View.GONE
  }

  private fun row(conversation: Conversation): View {
    val view = LayoutInflater.from(fragment.requireContext())
      .inflate(R.layout.chat_row, root.findViewById<ViewGroup>(R.id.groups_list), false)
    view.findViewById<TextView>(R.id.chat_row_avatar).text = conversation.initials()
    view.findViewById<TextView>(R.id.chat_row_name).text = conversation.title
    view.findViewById<TextView>(R.id.chat_row_preview).text = conversation.preview ?: " "
    view.findViewById<TextView>(R.id.chat_row_time).text =
      ChatLogic.relativeTime(conversation.lastMessageAt, System.currentTimeMillis())
    view.setOnClickListener { host.openConversation(conversation.id) }
    return view
  }

  private fun showCreateGroup() {
    val input = EditText(fragment.requireContext())
    input.setHint(R.string.chats_group_name)
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.chats_new_group)
      .setView(input)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.create) { _, _ ->
        val title = input.text.toString().trim()
        if (title.isEmpty()) return@setPositiveButton
        repository.createGroup(title, object : ChatRepository.IdCallback {
          override fun onReady(id: String) {
            fragment.requireActivity().runOnUiThread { host.openConversation(id) }
          }
          override fun onError(message: String) {
            fragment.requireActivity().runOnUiThread {
              Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
            }
          }
        })
      }
      .show()
  }

  override fun onInboxHint() { refresh() }
  override fun onConversationHint(conversationId: String) { refresh() }
  override fun onTyping(conversationId: String, userId: String, isTyping: Boolean) {}
  override fun onConnectionState(state: app.organicmaps.chat.realtime.ChatConnectionState) {}
}
