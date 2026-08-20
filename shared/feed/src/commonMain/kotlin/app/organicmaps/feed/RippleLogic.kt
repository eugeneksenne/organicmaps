package app.organicmaps.feed

import app.organicmaps.feed.FeedModels.Moment
import app.organicmaps.feed.RippleModels.PendingRipple
import app.organicmaps.feed.RippleModels.Snapshot

/**
 * Presentation and offline-queue rules for Ripple.
 *
 * A Ripple means "more people should experience this." It is not a Like, save, or rating.
 * This file must not compute momentum, velocity, decay, trust, or feed rank.
 */
object RippleLogic {
  const val MEANING = "More people should experience this."

  val publicFields = setOf(
    "kind", "id", "subject_kind", "subject_id", "band",
    "ripple_count", "tonight_count", "ripple_hour",
    "like_count", "save_count", "comment_count",
    "updated_at", "moment_id", "venue_id", "event_id", "city_id", "creator_id"
  )

  private val forbidden = setOf(
    "latitude", "longitude", "lat", "lng", "live_location", "location",
    "geofence", "accuracy", "heading",
    "whos_here", "who's here", "who_is_here", "whoshere", "presence",
    "invitation", "invitation_state", "invitation_until", "available_until",
    "who_is_present", "venue_presence"
  )

  fun normalizeBand(band: String?): String {
    val value = band?.lowercase()?.trim().orEmpty()
    return if (value in RippleModels.bands) value else RippleModels.BAND_QUIET
  }

  fun bandLabel(band: String?): String = when (normalizeBand(band)) {
    RippleModels.BAND_VIRAL -> "🔴 Viral"
    RippleModels.BAND_HOT -> "🟠 Hot"
    RippleModels.BAND_HEATING -> "🟡 Heating"
    RippleModels.BAND_ACTIVE -> "🟢 Active"
    else -> ""
  }

  fun canRipple(kind: String?): Boolean {
    val value = kind?.lowercase()?.trim().orEmpty()
    return value.isEmpty() || value in RippleModels.rippleKinds
  }

  fun isForbiddenField(name: String?): Boolean {
    if (name.isNullOrEmpty()) return false
    return name.lowercase().trim() in forbidden
  }

  /** Drops trust/velocity internals and any privacy-sensitive keys. Never infers a band. */
  fun publicFieldsOf(raw: Map<String, String?>): Map<String, String> {
    val out = linkedMapOf<String, String>()
    for ((key, value) in raw) {
      val name = key.lowercase().trim()
      if (isForbiddenField(name) || name !in publicFields) continue
      if (value.isNullOrEmpty() || value == "null") continue
      out[name] = value
    }
    return out
  }

  fun snapshotFromFields(fields: Map<String, String>): Snapshot {
    val public = publicFieldsOf(fields)
    return Snapshot(
      subjectKind = public["kind"] ?: public["subject_kind"] ?: RippleModels.SUBJECT_MOMENT,
      subjectId = public["id"] ?: public["subject_id"] ?: public["moment_id"].orEmpty(),
      band = normalizeBand(public["band"]),
      rippleCount = public["ripple_count"]?.toIntOrNull() ?: 0,
      tonightCount = public["tonight_count"]?.toIntOrNull() ?: 0,
      rippleHour = public["ripple_hour"]?.toIntOrNull(),
      likeCount = public["like_count"]?.toIntOrNull(),
      saveCount = public["save_count"]?.toIntOrNull(),
      commentCount = public["comment_count"]?.toIntOrNull(),
      updatedAt = public["updated_at"]
    )
  }

  /**
   * Copy server counts and band onto the Moment. Counts never promote the band.
   */
  fun applySnapshot(moment: Moment, snapshot: Snapshot, rippled: Boolean): Moment {
    return moment.copy(
      rippleCount = maxOf(0, snapshot.rippleCount),
      likeCount = snapshot.likeCount?.let { maxOf(0, it) } ?: moment.likeCount,
      saveCount = snapshot.saveCount?.let { maxOf(0, it) } ?: moment.saveCount,
      commentCount = snapshot.commentCount?.let { maxOf(0, it) } ?: moment.commentCount,
      momentumBand = normalizeBand(snapshot.band),
      rippled = rippled
    )
  }

  /** Local tap feedback only. Band stays whatever the server last sent. */
  fun optimisticRipple(moment: Moment, wantRippled: Boolean): Moment {
    val delta = when {
      wantRippled && !moment.rippled -> 1
      !wantRippled && moment.rippled -> -1
      else -> 0
    }
    return moment.copy(
      rippled = wantRippled,
      rippleCount = maxOf(0, moment.rippleCount + delta)
    )
  }

  fun mergeQueue(pending: List<PendingRipple>, momentId: String, wantRippled: Boolean, nowMs: Long): List<PendingRipple> {
    if (momentId.isEmpty()) return pending
    val others = pending.filter { it.momentId != momentId }
    return others + PendingRipple(momentId, wantRippled, nowMs)
  }

  fun shouldSend(pending: PendingRipple, serverRippled: Boolean): Boolean =
    pending.wantRippled != serverRippled

  fun railCaption(rippled: Boolean, band: String?, count: Int): String {
    val label = bandLabel(band)
    return when {
      rippled && label.isNotEmpty() -> label
      rippled -> "Rippled"
      label.isNotEmpty() -> label
      count > 0 -> FeedLogic.formatCount(count)
      else -> "Ripple"
    }
  }

  fun venueHeatLine(venueName: String, band: String, tonightCount: Int): String {
    val name = venueName.trim()
    if (name.isEmpty()) return ""
    val label = bandLabel(band).ifEmpty { "🌑 Quiet" }
    val count = if (tonightCount > 0) "\n${FeedLogic.formatCount(tonightCount)} Ripples Tonight" else ""
    return "📍 $name\n$label$count"
  }

  fun cityHeatLine(cityName: String, band: String, tonightCount: Int): String {
    val name = cityName.trim()
    if (name.isEmpty()) return ""
    val label = bandLabel(band).ifEmpty { "🌑 Quiet" }
    val count = if (tonightCount > 0) "\n${FeedLogic.formatCount(tonightCount)} Ripples Tonight" else ""
    return "$name\n$label$count"
  }
}
