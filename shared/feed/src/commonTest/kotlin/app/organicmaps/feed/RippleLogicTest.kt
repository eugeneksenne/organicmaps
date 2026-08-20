package app.organicmaps.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RippleLogicTest {
  @Test
  fun meaningIsRecommendationNotLike() {
    assertTrue(RippleLogic.MEANING.contains("experience"))
    assertFalse(RippleLogic.MEANING.contains("Like", ignoreCase = false) && RippleLogic.MEANING.startsWith("I like"))
  }

  @Test
  fun sharedVocabularyCoversEveryLayer() {
    assertEquals(listOf("quiet", "active", "heating", "hot", "viral"), RippleModels.bands)
    assertEquals("🟡 Heating", RippleLogic.bandLabel("heating"))
    assertEquals("🟠 Hot", RippleLogic.bandLabel("HOT"))
    assertEquals("", RippleLogic.bandLabel("quiet"))
    assertEquals("quiet", RippleLogic.normalizeBand("unknown"))
  }

  @Test
  fun applySnapshotNeverPromotesBandFromCounts() {
    val moment = FeedModels.Moment(
      id = "m1", creatorId = "c", creatorName = "Alfred", creatorUsername = "alfred",
      rippleCount = 1, momentumBand = "quiet", rippled = false
    )
    val snapshot = RippleModels.Snapshot(
      subjectId = "m1", band = "quiet", rippleCount = 999, tonightCount = 400
    )
    val next = RippleLogic.applySnapshot(moment, snapshot, true)
    assertEquals("quiet", next.momentumBand)
    assertEquals(999, next.rippleCount)
    assertTrue(next.rippled)
    assertEquals("", RippleLogic.bandLabel(next.momentumBand))
  }

  @Test
  fun optimisticRippleLeavesServerBandAlone() {
    val moment = FeedModels.Moment(
      id = "m1", creatorId = "c", creatorName = "Nomsa", creatorUsername = "nomsa",
      rippleCount = 22, momentumBand = "heating", rippled = false
    )
    val next = RippleLogic.optimisticRipple(moment, true)
    assertTrue(next.rippled)
    assertEquals(23, next.rippleCount)
    assertEquals("heating", next.momentumBand)
    val undone = RippleLogic.optimisticRipple(next, false)
    assertFalse(undone.rippled)
    assertEquals(22, undone.rippleCount)
    assertEquals("heating", undone.momentumBand)
  }

  @Test
  fun queueKeepsOneDesiredStatePerMoment() {
    val first = RippleLogic.mergeQueue(emptyList(), "m1", true, 1L)
    val second = RippleLogic.mergeQueue(first, "m1", false, 2L)
    val third = RippleLogic.mergeQueue(second, "m2", true, 3L)
    assertEquals(2, third.size)
    assertEquals(false, third.first { it.momentId == "m1" }.wantRippled)
    assertTrue(RippleLogic.shouldSend(third.first { it.momentId == "m1" }, true))
    assertFalse(RippleLogic.shouldSend(RippleModels.PendingRipple("m2", true), true))
  }

  @Test
  fun publicPayloadStripsLocationInvitationAndWhosHere() {
    val raw = mapOf(
      "band" to "hot",
      "ripple_count" to "12",
      "trust_score" to "0.9",
      "velocity" to "40",
      "latitude" to "-26.2",
      "longitude" to "28.0",
      "whos_here" to "12",
      "invitation_state" to "active",
      "available_until" to "soon",
      "live_location" to "yes"
    )
    val public = RippleLogic.publicFieldsOf(raw)
    assertEquals("hot", public["band"])
    assertEquals("12", public["ripple_count"])
    assertFalse(public.containsKey("trust_score"))
    assertFalse(public.containsKey("velocity"))
    assertFalse(public.keys.any { RippleLogic.isForbiddenField(it) })
    val snapshot = RippleLogic.snapshotFromFields(raw)
    assertEquals("hot", snapshot.band)
    assertEquals(12, snapshot.rippleCount)
  }

  @Test
  fun photosVideosLiveReplaysCanRipple() {
    assertTrue(RippleLogic.canRipple("photo"))
    assertTrue(RippleLogic.canRipple("live"))
    assertTrue(RippleLogic.canRipple("replay"))
    assertTrue(RippleLogic.canRipple("sponsored"))
    assertFalse(RippleLogic.canRipple("story"))
  }

  @Test
  fun railCaptionUsesServerBandNotLocalMath() {
    assertEquals("Ripple", RippleLogic.railCaption(false, "quiet", 0))
    assertEquals("Rippled", RippleLogic.railCaption(true, "quiet", 999))
    assertEquals("🟡 Heating", RippleLogic.railCaption(false, "heating", 1))
  }

  @Test
  fun venueAndCityLinesUseSharedBands() {
    val venue = RippleLogic.venueHeatLine("Cocoon Nightclub", "heating", 328)
    assertTrue(venue.contains("Cocoon"))
    assertTrue(venue.contains("Heating"))
    assertTrue(venue.contains("328"))
    val city = RippleLogic.cityHeatLine("Johannesburg", "hot", 12_384)
    assertTrue(city.contains("Johannesburg"))
    assertTrue(city.contains("Hot"))
  }
}
