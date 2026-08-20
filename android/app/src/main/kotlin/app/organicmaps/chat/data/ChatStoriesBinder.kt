package app.organicmaps.chat.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.Story

/** Stories list backed by inbox_stories, with composer and empty state. */
class ChatStoriesBinder(
  private val fragment: Fragment,
  private val root: View,
  private val host: Host
) : ChatServices.Listener {
  interface Host {
    fun openStory(storyId: String)
    fun openCamera()
    fun composeStory()
  }

  private val services = ChatServices.get(fragment.requireContext())

  fun start() {
    services.addListener(this)
    root.findViewById<View>(R.id.stories_back).setOnClickListener {
      fragment.parentFragmentManager.popBackStack()
    }
    root.findViewById<View>(R.id.stories_camera).setOnClickListener { host.composeStory() }
    root.findViewById<View>(R.id.story_add).setOnClickListener { host.composeStory() }
    root.findViewById<View>(R.id.story_add).setOnLongClickListener {
      host.openCamera()
      true
    }
    refresh()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) { services.removeListener(this@ChatStoriesBinder) }
    })
  }

  private fun refresh() {
    services.repository.loadInbox(object : ChatRepository.InboxCallback {
      override fun onLoaded(conversations: List<app.organicmaps.chats.ChatModels.Conversation>, stories: List<Story>, live: Boolean) {
        fragment.requireActivity().runOnUiThread { render(stories) }
      }
      override fun onStatus(status: String) {}
    })
  }

  private fun render(stories: List<Story>) {
    val list = root.findViewById<LinearLayout>(R.id.stories_list) ?: return
    list.removeAllViews()
    val selfId = services.session.userId
    val others = ChatLogic.orderedStories(stories, selfId, services.session.displayName)
    val empty = root.findViewById<View>(R.id.stories_empty)
    empty?.visibility = if (others.isEmpty()) View.VISIBLE else View.GONE
    val inflater = LayoutInflater.from(fragment.requireContext())
    for (story in others) {
      val row = inflater.inflate(R.layout.chat_row, list as ViewGroup, false)
      val name = story.authorName.ifEmpty { story.authorUsername }
      row.findViewById<TextView>(R.id.chat_row_avatar).text = name.take(1).uppercase()
      row.findViewById<TextView>(R.id.chat_row_name).text = if (story.verified) "$name  ✓" else name
      row.findViewById<TextView>(R.id.chat_row_preview).text =
        if (story.viewed) fragment.getString(R.string.story_viewed) else story.caption.ifEmpty { fragment.getString(R.string.story_unviewed) }
      row.findViewById<TextView>(R.id.chat_row_time).text =
        ChatLogic.relativeTime(story.createdAt, System.currentTimeMillis())
      row.setOnClickListener { host.openStory(story.id) }
      list.addView(row)
    }
  }

  override fun onInboxHint() { refresh() }
  override fun onConversationHint(conversationId: String) {}
  override fun onTyping(conversationId: String, userId: String, isTyping: Boolean) {}
  override fun onConnectionState(state: app.organicmaps.chat.realtime.ChatConnectionState) {}
}
