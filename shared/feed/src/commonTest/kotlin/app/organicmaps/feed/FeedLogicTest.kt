package app.organicmaps.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedLogicTest {
  @Test
  fun contextLineUsesTabRules() {
    val now = 1_000_000L
    val moment = FeedModels.Moment(
      id = "1", creatorId = "c", creatorName = "Amanda", creatorUsername = "amanda",
      verified = true, suburb = "Sandton", publishedAt = now - 2 * 3_600_000L, distanceM = 280
    )
    assertTrue(FeedLogic.contextLine(moment, FeedModels.TAB_FOR_YOU, now).contains("Sandton"))
    assertTrue(FeedLogic.contextLine(moment, FeedModels.TAB_NEARBY, now).contains("280m"))
    val live = moment.copy(isLive = true, liveViewers = 2400)
    assertTrue(FeedLogic.contextLine(live, FeedModels.TAB_LIVE, now).contains("LIVE"))
  }

  @Test
  fun invitationStatesStaySeparateFromWhosHere() {
    val now = 10_000L
    val active = FeedModels.Invitation("Cocoon", "active", "Alfred", now + 45_000L)
    assertTrue(FeedLogic.invitationStatus(active, now).contains("is here now"))
    assertTrue(FeedLogic.invitationStatus(active, now).contains("Available"))
    val ended = FeedModels.Invitation("Cocoon", "ended", "Alfred")
    assertTrue(FeedLogic.invitationStatus(ended, now).contains("has left"))
    val closed = FeedModels.Invitation("Cocoon", "venue_closed", "Alfred", opensLabel = "Friday 18:00")
    assertTrue(FeedLogic.invitationStatus(closed, now).contains("Venue Closed"))
  }

  @Test
  fun followHiddenForOwnAndAlreadyFollowing() {
    val own = FeedModels.Moment("1", "me", "You", "me", mine = true)
    val following = FeedModels.Moment("2", "p", "A", "a", following = true)
    val other = FeedModels.Moment("3", "p", "A", "a")
    assertFalse(FeedLogic.showFollow(own))
    assertFalse(FeedLogic.showFollow(following))
    assertTrue(FeedLogic.showFollow(other))
  }

  @Test
  fun momentumLabelIsDisplayOnly() {
    assertEquals("🟡 Heating", FeedLogic.momentumLabel("heating"))
    assertEquals("", FeedLogic.momentumLabel("quiet"))
  }
}
