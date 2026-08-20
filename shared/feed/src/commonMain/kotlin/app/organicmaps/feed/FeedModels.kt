package app.organicmaps.feed

/** Platform-agnostic Moment records. Clients never compute momentum scores. */
object FeedModels {
  const val TAB_FOR_YOU = "for_you"
  const val TAB_FOLLOWING = "following"
  const val TAB_NEARBY = "nearby"
  const val TAB_LIVE = "live"

  data class Moment(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val creatorUsername: String,
    val verified: Boolean = false,
    val kind: String = "photo",
    val caption: String = "",
    val suburb: String = "",
    val venueName: String = "",
    val publishedAt: Long = 0L,
    val distanceM: Int = 0,
    val liveViewers: Int = 0,
    val isLive: Boolean = false,
    val isReplay: Boolean = false,
    val likeCount: Int = 0,
    val rippleCount: Int = 0,
    val commentCount: Int = 0,
    val saveCount: Int = 0,
    val liked: Boolean = false,
    val rippled: Boolean = false,
    val saved: Boolean = false,
    val following: Boolean = false,
    val mine: Boolean = false,
    val momentumBand: String = "quiet",
    val friendRipples: Int = 0,
    val invitation: Invitation? = null
  )

  data class Invitation(
    val venueName: String,
    val state: String,
    val creatorName: String,
    val availableUntil: Long = 0L,
    val venueClosed: Boolean = false,
    val opensLabel: String = ""
  )

  data class Comment(
    val id: String,
    val authorName: String,
    val body: String,
    val createdAt: Long
  )
}
