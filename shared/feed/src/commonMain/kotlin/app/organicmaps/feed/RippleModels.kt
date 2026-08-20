package app.organicmaps.feed

/**
 * Public Ripple records. Momentum scores, trust, and velocity live on the server.
 * These types never include live location, Who's Here, or Moment Invitation fields.
 */
object RippleModels {
  const val BAND_QUIET = "quiet"
  const val BAND_ACTIVE = "active"
  const val BAND_HEATING = "heating"
  const val BAND_HOT = "hot"
  const val BAND_VIRAL = "viral"

  const val SUBJECT_MOMENT = "moment"
  const val SUBJECT_VENUE = "venue"
  const val SUBJECT_EVENT = "event"
  const val SUBJECT_CITY = "city"
  const val SUBJECT_CREATOR = "creator"

  val bands = listOf(BAND_QUIET, BAND_ACTIVE, BAND_HEATING, BAND_HOT, BAND_VIRAL)
  val rippleKinds = listOf("photo", "video", "live", "replay", "template", "sponsored")

  /** Server-owned public snapshot. Clients copy these fields; they do not recompute them. */
  data class Snapshot(
    val subjectKind: String = SUBJECT_MOMENT,
    val subjectId: String = "",
    val band: String = BAND_QUIET,
    val rippleCount: Int = 0,
    val tonightCount: Int = 0,
    val rippleHour: Int? = null,
    val likeCount: Int? = null,
    val saveCount: Int? = null,
    val commentCount: Int? = null,
    val updatedAt: String? = null
  )

  /** Desired Ripple state queued while offline. One row per Moment. */
  data class PendingRipple(
    val momentId: String,
    val wantRippled: Boolean,
    val queuedAt: Long = 0L
  )
}
