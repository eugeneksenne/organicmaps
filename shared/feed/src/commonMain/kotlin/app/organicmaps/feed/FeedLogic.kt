package app.organicmaps.feed

import app.organicmaps.feed.FeedModels.Invitation
import app.organicmaps.feed.FeedModels.Moment

/**
 * Presentation rules for Feed. Momentum bands and scores come from the server Ripple engine.
 * This file must not compute ranking or velocity.
 */
object FeedLogic {
  val tabs = listOf(FeedModels.TAB_FOR_YOU, FeedModels.TAB_FOLLOWING, FeedModels.TAB_NEARBY, FeedModels.TAB_LIVE)

  fun relativeTime(epochMs: Long, nowMs: Long): String {
    if (epochMs <= 0L) return ""
    val delta = maxOf(0L, nowMs - epochMs)
    return when {
      delta < 60_000L -> "Now"
      delta < 3_600_000L -> "${delta / 60_000L}m ago"
      delta < 86_400_000L -> "${delta / 3_600_000L}h ago"
      else -> "${delta / 86_400_000L}d ago"
    }
  }

  fun contextLine(moment: Moment, tab: String, nowMs: Long): String {
    if (moment.isLive) return "● LIVE • ${formatCount(moment.liveViewers)} watching"
    if (moment.isReplay) return "Replay • Ended ${relativeTime(moment.publishedAt, nowMs)}"
    val whenText = relativeTime(moment.publishedAt, nowMs)
    return when (tab) {
      FeedModels.TAB_NEARBY -> {
        val distance = when {
          moment.distanceM in 1..999 -> "${moment.distanceM}m away"
          moment.distanceM >= 1000 -> "${moment.distanceM / 1000}km away"
          else -> moment.suburb
        }
        "$whenText • $distance"
      }
      FeedModels.TAB_FOLLOWING -> "$whenText • Following"
      else -> if (moment.suburb.isEmpty()) whenText else "$whenText • ${moment.suburb}"
    }
  }

  fun invitationStatus(invitation: Invitation, nowMs: Long): String {
    return when (invitation.state) {
      "active" -> {
        val left = invitation.availableUntil - nowMs
        val clock = if (left > 0) formatCountdown(left) else "00:00"
        "● ${invitation.creatorName} is here now   •   Available $clock"
      }
      "venue_closed" -> {
        val opens = invitation.opensLabel
        if (opens.isEmpty()) "● Venue Closed" else "● Venue Closed\nOpens $opens"
      }
      else -> "○ ${invitation.creatorName} has left"
    }
  }

  fun momentumLabel(band: String): String = RippleLogic.bandLabel(band)

  fun friendIntelligence(count: Int): String =
    if (count > 0) "👥 $count friends rippled this" else ""

  fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1000 -> "${value / 1000}K"
    else -> value.toString()
  }

  fun formatCountdown(remainingMs: Long): String {
    val total = maxOf(0L, remainingMs / 1000L)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
      "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
      "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
  }

  fun creatorLabel(moment: Moment): String =
    (if (moment.creatorUsername.isNotEmpty()) moment.creatorUsername else moment.creatorName) +
      if (moment.verified) "  ✓" else ""

  fun showFollow(moment: Moment): Boolean = !moment.mine && !moment.following

  fun demoFeed(tab: String, now: Long): List<Moment> {
    val invite = Invitation("Cocoon Nightclub", "active", "Alfred", now + 4 * 60_000L + 45_000L)
    val base = Moment(
      id = "demo-moment-alfred",
      creatorId = "demo-alfred-user",
      creatorName = "Alfred",
      creatorUsername = "alfred.m",
      verified = true,
      kind = if (tab == FeedModels.TAB_LIVE) "live" else "video",
      caption = "The room changed when this track dropped. Johannesburg, you had to be there. 🔥",
      suburb = "Sandton",
      venueName = "Cocoon Nightclub",
      publishedAt = now - 2 * 3_600_000L,
      distanceM = 280,
      liveViewers = 2400,
      isLive = tab == FeedModels.TAB_LIVE,
      likeCount = 2800,
      rippleCount = 184,
      commentCount = 184,
      saveCount = 90,
      momentumBand = "heating",
      friendRipples = 8,
      invitation = invite
    )
    val second = base.copy(
      id = "demo-moment-nomsa",
      creatorId = "demo-nomsa-user",
      creatorName = "Nomsa",
      creatorUsername = "nomsa",
      verified = false,
      caption = "Getting ready. Pull through if you're in Braam.",
      suburb = "Braamfontein",
      venueName = "The Living Room",
      publishedAt = now - 40 * 60_000L,
      distanceM = 1400,
      isLive = false,
      likeCount = 120,
      rippleCount = 22,
      commentCount = 9,
      friendRipples = 3,
      invitation = Invitation("The Living Room", "ended", "Nomsa")
    )
    return if (tab == FeedModels.TAB_LIVE) listOf(base) else listOf(base, second)
  }
}
