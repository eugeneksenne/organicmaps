package app.organicmaps.feed

import android.app.AlertDialog
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chat.data.ChatRepository
import app.organicmaps.chat.data.ChatServices
import app.organicmaps.chat.data.OkHttpChatRpcClient
import app.organicmaps.feed.FeedModels.Moment
import okhttp3.OkHttpClient

/** Binds the full-screen Moment player to feed_page + Ripple RPCs. */
class FeedBinder(
  private val fragment: Fragment,
  private val root: View,
  private val host: Host
) {
  interface Host {
    fun openProfile(username: String)
    fun showNotice(message: String)
    fun openFeature(destination: String)
  }

  private val services = ChatServices.get(fragment.requireContext())
  private val repository = FeedRepository(
    OkHttpChatRpcClient.fromSession(OkHttpClient(), services.session),
    services.session,
    services.auth,
    FeedCache(fragment.requireContext())
  )
  private var tab = FeedModels.TAB_FOR_YOU
  private var moments: List<Moment> = emptyList()
  private var index = 0
  private val tick = object : Runnable {
    override fun run() {
      bindCurrent()
      if (fragment.isAdded) root.postDelayed(this, 1000L)
    }
  }

  fun start() {
    bindTabs()
    root.findViewById<View>(R.id.feed_search).setOnClickListener {
      host.showNotice("Search creators, venues, events, sounds, and hashtags")
    }
    root.findViewById<View>(R.id.feed_tap_next)?.setOnClickListener { next() }
    root.findViewById<View>(R.id.feed_tap_prev)?.setOnClickListener { previous() }
    load()
    root.post(tick)
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {}
      override fun onViewDetachedFromWindow(v: View) { root.removeCallbacks(tick) }
    })
  }

  private fun bindTabs() {
    val ids = intArrayOf(R.id.feed_for_you, R.id.feed_following, R.id.feed_nearby, R.id.feed_live)
    val kinds = arrayOf(FeedModels.TAB_FOR_YOU, FeedModels.TAB_FOLLOWING, FeedModels.TAB_NEARBY, FeedModels.TAB_LIVE)
    ids.forEachIndexed { i, id ->
      root.findViewById<TextView>(id)?.setOnClickListener {
        tab = kinds[i]
        highlightTabs(ids, i)
        load()
      }
    }
    highlightTabs(ids, 0)
  }

  private fun highlightTabs(ids: IntArray, selected: Int) {
    ids.forEachIndexed { i, id ->
      val tabView = root.findViewById<TextView>(id) ?: return@forEachIndexed
      val active = i == selected
      tabView.setBackgroundResource(if (active) R.drawable.feed_glass_pill else 0)
      tabView.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xCCFFFFFF.toInt())
    }
  }

  private fun load() {
    repository.load(tab, object : FeedRepository.PageCallback {
      override fun onLoaded(moments: List<Moment>, live: Boolean) {
        runOnUi {
          this@FeedBinder.moments = moments
          index = 0
          bindCurrent()
        }
      }
      override fun onStatus(status: String) {
        runOnUi {
          val view = root.findViewById<TextView>(R.id.feed_status) ?: return@runOnUi
          view.text = status
          view.visibility = if (status.isEmpty()) View.GONE else View.VISIBLE
        }
      }
    })
  }

  private fun bindCurrent() {
    val empty = root.findViewById<TextView>(R.id.feed_empty)
    val moment = moments.getOrNull(index)
    if (moment == null) {
      empty?.visibility = View.VISIBLE
      return
    }
    empty?.visibility = View.GONE
    val now = System.currentTimeMillis()
    root.findViewById<TextView>(R.id.feed_username)?.text = FeedLogic.creatorLabel(moment)
    root.findViewById<TextView>(R.id.feed_avatar)?.text = moment.creatorName.take(1).uppercase()
    root.findViewById<TextView>(R.id.feed_context)?.text = FeedLogic.contextLine(moment, tab, now)
    root.findViewById<TextView>(R.id.feed_caption)?.text = moment.caption
    val intel = FeedLogic.friendIntelligence(moment.friendRipples)
    val intelView = root.findViewById<TextView>(R.id.feed_intelligence)
    intelView?.text = intel
    intelView?.visibility = if (intel.isEmpty()) View.GONE else View.VISIBLE
    root.findViewById<View>(R.id.feed_profile)?.setOnClickListener { host.openProfile(moment.creatorUsername) }
    bindRail(moment)
    bindInvitation(moment, now)
  }

  private fun bindRail(moment: Moment) {
    val follow = root.findViewById<TextView>(R.id.feed_follow)
    follow?.visibility = if (FeedLogic.showFollow(moment)) View.VISIBLE else View.GONE
    follow?.text = "+\nFollow"
    follow?.setOnClickListener {
      repository.follow(moment.creatorId, true)
      replace(moment.copy(following = true))
    }
    root.findViewById<TextView>(R.id.feed_like)?.apply {
      text = if (moment.liked) "♥\n${FeedLogic.formatCount(moment.likeCount)}" else "♡\n${FeedLogic.formatCount(moment.likeCount)}"
      setOnClickListener { react(moment, "like") }
    }
    root.findViewById<TextView>(R.id.feed_ripple)?.apply {
      text = "≋\n${RippleLogic.railCaption(moment.rippled, moment.momentumBand, moment.rippleCount)}"
      setOnClickListener { react(moment, "ripple") }
    }
    root.findViewById<TextView>(R.id.feed_comment)?.apply {
      text = "◌\n${FeedLogic.formatCount(moment.commentCount)}"
      setOnClickListener { showComments(moment) }
    }
    root.findViewById<TextView>(R.id.feed_save)?.apply {
      text = if (moment.saved) "⌑\nSaved" else "⌑\nSave"
      setOnClickListener { react(moment, "save") }
    }
    root.findViewById<View>(R.id.feed_share)?.setOnClickListener {
      host.showNotice("Share this Moment")
    }
  }

  private fun bindInvitation(moment: Moment, now: Long) {
    val card = root.findViewById<LinearLayout>(R.id.feed_invitation) ?: return
    val invitation = moment.invitation
    if (invitation == null) {
      card.visibility = View.GONE
      return
    }
    card.visibility = View.VISIBLE
    root.findViewById<TextView>(R.id.feed_invitation_venue)?.text = "📍 ${invitation.venueName}  ✓"
    root.findViewById<TextView>(R.id.feed_invitation_status)?.text = FeedLogic.invitationStatus(invitation, now)
    root.findViewById<View>(R.id.feed_lobby)?.setOnClickListener {
      host.showNotice("Club Lobby stays on the venue, not Who's Here")
    }
    root.findViewById<View>(R.id.feed_route)?.setOnClickListener {
      host.openFeature("map")
    }
  }

  private fun react(moment: Moment, kind: String) {
    if (kind == "ripple" && !RippleLogic.canRipple(moment.kind)) {
      host.showNotice("This Moment can't be rippled")
      return
    }
    val want = when (kind) {
      "like" -> !moment.liked
      "ripple" -> !moment.rippled
      else -> !moment.saved
    }
    if (kind == "ripple") {
      root.findViewById<View>(R.id.feed_ripple)?.let { pulseRipple(it) }
    }
    val optimistic = when (kind) {
      "ripple" -> RippleLogic.optimisticRipple(moment, want)
      "like" -> moment.copy(liked = want, likeCount = maxOf(0, moment.likeCount + if (want) 1 else -1))
      else -> moment.copy(saved = want, saveCount = maxOf(0, moment.saveCount + if (want) 1 else -1))
    }
    replace(optimistic)
    repository.toggle(moment.id, kind, want, object : FeedRepository.ReactCallback {
      override fun onReady(result: FeedJson.ReactResult) {
        runOnUi {
          val current = moments.firstOrNull { it.id == moment.id } ?: optimistic
          val next = if (result.snapshot != null) {
            val applied = RippleLogic.applySnapshot(current, result.snapshot, if (kind == "ripple") want else current.rippled)
            when (kind) {
              "like" -> applied.copy(liked = want)
              "save" -> applied.copy(saved = want)
              else -> applied
            }
          } else optimistic
          replace(next)
        }
      }
      override fun onError(message: String) {
        runOnUi {
          replace(moment)
          Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
        }
      }
    })
  }

  private fun pulseRipple(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    view.animate().cancel()
    view.animate().scaleX(1.16f).scaleY(1.16f).setDuration(80)
      .withEndAction {
        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
      }
      .start()
  }

  private fun showComments(moment: Moment) {
    val context = fragment.requireContext()
    val input = EditText(context)
    input.hint = "Comment"
    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    AlertDialog.Builder(context)
      .setTitle("Comments")
      .setView(input)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.chats_send) { _, _ ->
        val body = input.text.toString().trim()
        if (body.isEmpty()) return@setPositiveButton
        repository.comment(moment.id, body, object : ChatRepository.IdCallback {
          override fun onReady(id: String) {
            runOnUi { replace(moment.copy(commentCount = moment.commentCount + 1)) }
          }
          override fun onError(message: String) {
            runOnUi { host.showNotice(message) }
          }
        })
      }
      .show()
    repository.comments(moment.id, object : FeedRepository.CommentsCallback {
      override fun onLoaded(comments: List<FeedModels.Comment>) {
        if (comments.isEmpty()) return
        runOnUi {
          host.showNotice(comments.takeLast(1).joinToString { "${it.authorName}: ${it.body}" })
        }
      }
      override fun onError(message: String) {}
    })
  }

  private fun replace(moment: Moment) {
    moments = moments.map { if (it.id == moment.id) moment else it }
    bindCurrent()
  }

  private fun next() {
    if (index < moments.size - 1) {
      index += 1
      bindCurrent()
    }
  }

  private fun previous() {
    if (index > 0) {
      index -= 1
      bindCurrent()
    }
  }

  private fun runOnUi(action: () -> Unit) {
    if (fragment.isAdded) fragment.requireActivity().runOnUiThread(action)
  }
}
