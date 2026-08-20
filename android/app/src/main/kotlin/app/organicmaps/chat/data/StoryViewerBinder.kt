package app.organicmaps.chat.data

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels.Story

/** Full-screen story viewer: progress, tap next/prev, hold to pause, reply, react, mute. */
class StoryViewerBinder(
  private val fragment: Fragment,
  private val root: View,
  private val initialId: String,
  private val host: Host
) {
  interface Host {
    fun close()
    fun openChat(username: String)
    fun showNotice(message: String)
  }

  private val handler = Handler(Looper.getMainLooper())
  private val repository = ChatServices.get(fragment.requireContext()).repository
  private var stories: List<Story> = emptyList()
  private var index = 0
  private var paused = false
  private var progress = 0
  private val tick = object : Runnable {
    override fun run() {
      if (paused || stories.isEmpty()) return
      progress += 4
      if (progress >= 100) {
        next()
        return
      }
      paintProgress()
      handler.postDelayed(this, 120L)
    }
  }

  fun start() {
    val all = repository.store.loadStories().ifEmpty { ChatLogic.demoStories(System.currentTimeMillis()) }
    val selfId = repository.session().userId
    stories = ChatLogic.orderedStories(all, selfId, repository.session().displayName) +
      listOfNotNull(ChatLogic.ownStory(all, selfId, repository.session().displayName))
    if (stories.isEmpty()) stories = all
    index = stories.indexOfFirst { it.id == initialId || it.authorName == initialId || it.authorUsername == initialId }
      .coerceAtLeast(0)
    root.findViewById<View>(R.id.story_viewer_close).setOnClickListener { host.close() }
    root.findViewById<View>(R.id.story_tap_right).setOnClickListener { next() }
    root.findViewById<View>(R.id.story_tap_left).setOnClickListener { previous() }
    root.findViewById<View>(R.id.story_react).setOnClickListener { react() }
    root.findViewById<View>(R.id.story_more).setOnClickListener { more(it) }
    val reply = root.findViewById<EditText>(R.id.story_reply)
    reply.setOnEditorActionListener { _, _, _ ->
      sendReply(reply.text.toString())
      true
    }
    val holdTarget = root.findViewById<View>(R.id.story_caption)
    holdTarget.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          paused = true
          true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          paused = false
          handler.removeCallbacks(tick)
          handler.post(tick)
          true
        }
        else -> false
      }
    }
    showCurrent()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) { handler.removeCallbacks(tick) }
    })
  }

  private fun showCurrent() {
    if (stories.isEmpty()) {
      host.close()
      return
    }
    val story = stories[index]
    root.findViewById<TextView>(R.id.story_viewer_name).text =
      if (story.verified) story.authorName + "  ✓" else story.authorName.ifEmpty { story.authorUsername }
    root.findViewById<TextView>(R.id.story_viewer_avatar).text =
      (story.authorName.ifEmpty { story.authorUsername }).take(1).uppercase()
    root.findViewById<TextView>(R.id.story_viewer_time).text =
      ChatLogic.relativeTime(story.createdAt, System.currentTimeMillis())
    root.findViewById<TextView>(R.id.story_caption).text =
      story.caption.ifEmpty { "Story" }
    root.findViewById<TextView>(R.id.story_react).text = "♡"
    repository.markStoryViewed(story.id)
    progress = 0
    paintProgress()
    handler.removeCallbacks(tick)
    handler.post(tick)
  }

  private fun paintProgress() {
    val row = root.findViewById<LinearLayout>(R.id.story_progress_row)
    if (row.childCount != stories.size) {
      row.removeAllViews()
      repeat(stories.size) {
        val segment = View(fragment.requireContext())
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        params.marginEnd = 4
        segment.layoutParams = params
        row.addView(segment)
      }
    }
    for (i in 0 until row.childCount) {
      val fill = when {
        i < index -> 100
        i == index -> progress
        else -> 0
      }
      val drawable = GradientDrawable()
      drawable.cornerRadius = 4f
      drawable.setColor(if (fill > 0) 0xFFFFFFFF.toInt() else 0x55FFFFFF)
      row.getChildAt(i).background = drawable
      row.getChildAt(i).alpha = if (i == index) 0.4f + fill / 160f else if (i < index) 1f else 0.35f
    }
  }

  private fun next() {
    if (index >= stories.size - 1) {
      host.close()
      return
    }
    index += 1
    showCurrent()
  }

  private fun previous() {
    if (index <= 0) return
    index -= 1
    showCurrent()
  }

  private fun react() {
    val story = stories.getOrNull(index) ?: return
    repository.reactToStory(story.id)
    root.findViewById<TextView>(R.id.story_react).text = "♥"
    Toast.makeText(fragment.requireContext(), R.string.chats_react, Toast.LENGTH_SHORT).show()
  }

  private fun sendReply(text: String) {
    val story = stories.getOrNull(index) ?: return
    val body = text.trim()
    if (body.isEmpty()) return
    root.findViewById<EditText>(R.id.story_reply).setText("")
    val username = story.authorUsername.ifEmpty { story.authorName }
    repository.openDirect(story.authorId, object : ChatRepository.IdCallback {
      override fun onReady(id: String) {
        repository.sendText(id, "↩ ${story.caption}\n$body")
        fragment.requireActivity().runOnUiThread { host.openChat(username) }
      }
      override fun onError(message: String) {
        fragment.requireActivity().runOnUiThread {
          host.openChat(username)
          host.showNotice(body)
        }
      }
    })
  }

  private fun more(anchor: View) {
    val story = stories.getOrNull(index) ?: return
    val menu = PopupMenu(fragment.requireContext(), anchor)
    menu.menu.add(fragment.getString(R.string.story_mute))
    if (story.mine) menu.menu.add(fragment.getString(R.string.story_delete))
    menu.menu.add(fragment.getString(R.string.chats_report))
    menu.setOnMenuItemClickListener { item ->
      when (item.title) {
        fragment.getString(R.string.story_mute) -> {
          repository.muteStoryAuthor(story.authorId)
          host.close()
        }
        fragment.getString(R.string.story_delete) -> {
          repository.deleteStory(story.id)
          host.close()
        }
        else -> host.showNotice(fragment.getString(R.string.chats_report))
      }
      true
    }
    menu.show()
  }
}
