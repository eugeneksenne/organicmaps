package app.organicmaps.feed

import app.organicmaps.chats.ChatLogic
import app.organicmaps.feed.FeedModels.Comment
import app.organicmaps.feed.FeedModels.Invitation
import app.organicmaps.feed.FeedModels.Moment
import app.organicmaps.feed.RippleModels.Snapshot
import org.json.JSONArray
import org.json.JSONObject

object FeedJson {
  fun parseMoments(rows: JSONArray): List<Moment> =
    (0 until rows.length()).mapNotNull { parseMoment(rows.optJSONObject(it)) }

  fun parseMoment(row: JSONObject?): Moment? {
    if (row == null) return null
    val id = row.optString("id")
    if (id.isEmpty()) return null
    val inviteState = row.optString("invitation_state")
    val invitation = if (inviteState.isEmpty() || inviteState == "null") null
    else Invitation(
      venueName = row.optString("venue_name"),
      state = inviteState,
      creatorName = row.optString("creator_name"),
      availableUntil = ChatLogic.parseTime(row.optString("invitation_until"))
    )
    return Moment(
      id = id,
      creatorId = row.optString("creator_id"),
      creatorName = row.optString("creator_name"),
      creatorUsername = row.optString("creator_username"),
      verified = row.optBoolean("creator_verified"),
      kind = row.optString("kind", "photo"),
      caption = row.optString("caption"),
      suburb = row.optString("suburb"),
      venueName = row.optString("venue_name"),
      publishedAt = ChatLogic.parseTime(row.optString("published_at")),
      liveViewers = row.optInt("live_viewers"),
      isLive = row.optBoolean("is_live"),
      isReplay = row.optBoolean("is_replay"),
      likeCount = row.optInt("like_count"),
      rippleCount = row.optInt("ripple_count"),
      commentCount = row.optInt("comment_count"),
      saveCount = row.optInt("save_count"),
      liked = row.optBoolean("liked"),
      rippled = row.optBoolean("rippled"),
      saved = row.optBoolean("saved"),
      following = row.optBoolean("following"),
      mine = row.optBoolean("mine"),
      momentumBand = RippleLogic.normalizeBand(row.optString("momentum_band", "quiet")),
      friendRipples = row.optInt("friend_ripples"),
      invitation = invitation
    )
  }

  fun parseComments(rows: JSONArray): List<Comment> =
    (0 until rows.length()).mapNotNull { index ->
      val row = rows.optJSONObject(index) ?: return@mapNotNull null
      val id = row.optString("id")
      if (id.isEmpty()) null
      else Comment(id, row.optString("author_name"), row.optString("body"), ChatLogic.parseTime(row.optString("created_at")))
    }

  fun parseReact(raw: String): ReactResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed == "null") return ReactResult(removed = false, snapshot = null)
    val row = JSONObject(trimmed)
    return ReactResult(
      removed = row.optBoolean("removed"),
      snapshot = parseSnapshot(row.optJSONObject("momentum") ?: row)
    )
  }

  fun parseSnapshot(row: JSONObject?): Snapshot? {
    if (row == null) return null
    val fields = linkedMapOf<String, String?>()
    val keys = row.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      fields[key] = if (row.isNull(key)) null else row.opt(key)?.toString()
    }
    val snapshot = RippleLogic.snapshotFromFields(fields)
    return if (snapshot.subjectId.isEmpty() && snapshot.rippleCount == 0 && snapshot.band == RippleModels.BAND_QUIET) {
      if (fields.containsKey("band") || fields.containsKey("ripple_count")) snapshot else null
    } else snapshot
  }

  data class ReactResult(val removed: Boolean, val snapshot: Snapshot?)
}
