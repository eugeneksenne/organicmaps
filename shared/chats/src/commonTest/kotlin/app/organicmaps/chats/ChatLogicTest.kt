package app.organicmaps.chats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLogicTest {
  @Test
  fun demoInboxKeepsPinnedNightGuardFirst() {
    val inbox = ChatLogic.demoInbox(1_000L)
    assertEquals("demo-nightguard", inbox[0].id)
    assertTrue(inbox[0].pinned)
    assertEquals(5, inbox.size)
  }

  @Test
  fun mergeQueuedKeepsUnackedLocalMessages() {
    val server = listOf(
      ChatModels.Message("m1", "c", "peer", "A", "text", "hi", sentAt = 1, mine = false)
    )
    val local = listOf(
      ChatModels.Message("op1", "c", "me", "You", "text", "queued", clientOperationId = "op1", sentAt = 2, mine = true, state = "queued")
    )
    val merged = ChatLogic.mergeQueued(server, local)
    assertEquals(2, merged.size)
    assertEquals("op1", merged[1].id)
  }

  @Test
  fun mergeQueuedDropsLocalOnceServerHasOperation() {
    val server = listOf(
      ChatModels.Message("m2", "c", "me", "You", "text", "hi", clientOperationId = "op1", sentAt = 2, mine = true)
    )
    val local = listOf(
      ChatModels.Message("op1", "c", "me", "You", "text", "hi", clientOperationId = "op1", sentAt = 2, mine = true, state = "queued")
    )
    val merged = ChatLogic.mergeQueued(server, local)
    assertEquals(1, merged.size)
    assertEquals("m2", merged[0].id)
  }

  @Test
  fun filtersUnreadAndVenue() {
    val unread = ChatModels.Conversation("1", "direct", "A", unread = 2)
    val venue = ChatModels.Conversation("2", "venue", "Club")
    assertTrue(ChatLogic.matchesCategory(unread, ChatModels.FILTER_UNREAD))
    assertFalse(ChatLogic.matchesCategory(venue, ChatModels.FILTER_UNREAD))
    assertTrue(ChatLogic.matchesCategory(venue, ChatModels.FILTER_VENUE))
    assertFalse(ChatLogic.matchesCategory(unread, ChatModels.FILTER_VENUE))
  }

  @Test
  fun toggleReactionAddsAndRemoves() {
    val added = ChatLogic.toggleReactionSummary("", null, "❤️")
    assertEquals("❤️ 1", added.first)
    assertEquals("❤️", added.second)
    val removed = ChatLogic.toggleReactionSummary(added.first, added.second, "❤️")
    assertEquals("", removed.first)
    assertEquals(null, removed.second)
  }

  @Test
  fun parseTimeReadsIsoAndEpoch() {
    assertEquals(0L, ChatLogic.parseTime(""))
    assertTrue(ChatLogic.parseTime("2026-08-20T00:00:00Z") > 0L)
    assertEquals(42L, ChatLogic.parseTime("42"))
  }

  @Test
  fun sanitizesUsernamesFromEmail() {
    assertEquals("alfred.m", ChatLogic.usernameFromEmail("Alfred.M@example.com"))
    assertTrue(ChatLogic.usernameFromEmail("ab@x.com").startsWith("user"))
    assertEquals("Alfred", ChatLogic.displayNameFromEmail("alfred@example.com"))
  }

  @Test
  fun previewTextForRichCards() {
    val venue = ChatModels.Message("1", "c", "p", "A", "venue", "Cocoon\nOpen", sentAt = 1, mine = false)
    assertEquals("📍 Cocoon", ChatLogic.previewText(venue))
  }

  @Test
  fun callPreviewAndFilters() {
    val missed = ChatModels.CallRecord("1", "c", "Lerato", "video", "incoming", "missed", 1L)
    val voice = ChatModels.CallRecord("2", "c", "Nomsa", "voice", "incoming", "ended", 1L, 240)
    assertEquals("↙ Missed video call", ChatLogic.callPreview(missed))
    assertTrue(ChatLogic.callPreview(voice).contains("Incoming voice"))
    assertTrue(ChatLogic.matchesCallFilter(missed, "missed"))
    assertTrue(ChatLogic.matchesCallFilter(missed, "video"))
    assertFalse(ChatLogic.matchesCallFilter(voice, "video"))
    assertEquals("04:00", ChatLogic.liveTimer(240))
    assertEquals("4 min", ChatLogic.formatDuration(240))
  }

  @Test
  fun contactCardRoundTrip() {
    val card = ChatLogic.contactCard(ChatModels.Contact("1", "Nomsa", "+27821234567", "nomsa@example.com"))
    assertTrue(card.startsWith("👤 Nomsa"))
    val parsed = ChatLogic.parseContact(card)!!
    assertEquals("Nomsa", parsed.name)
    assertEquals("+27821234567", parsed.phone)
    assertEquals("nomsa@example.com", parsed.email)
    assertTrue(ChatLogic.matchesContactQuery(parsed, "2782"))
    assertFalse(ChatLogic.matchesContactQuery(parsed, "alfred"))
  }

  @Test
  fun storiesOwnFirstThenUnviewed() {
    val stories = ChatLogic.demoStories(1_000L)
    val own = ChatLogic.ownStory(stories, "me", "You")
    assertEquals("demo-story-me", own?.id)
    val ordered = ChatLogic.orderedStories(stories, "me", "You")
    assertFalse(ordered.any { it.id == "demo-story-me" })
    assertEquals("demo-story-alfred", ordered.first().id)
    assertTrue(ordered.last().viewed)
  }

  @Test
  fun inboxPreviewSkipsEmptyAndFormatsCards() {
    assertEquals("", ChatLogic.inboxPreview("text", null))
    assertEquals("📍 Cocoon", ChatLogic.inboxPreview("venue", "Cocoon\nOpen"))
    assertEquals("This message was deleted", ChatLogic.inboxPreview("text", "hi", deleted = true))
  }

  @Test
  fun deliveryTicksAndTypingAndMentions() {
    assertEquals("  ○", ChatLogic.deliveryTick("queued", true))
    assertEquals("  ✓", ChatLogic.deliveryTick("sent", true))
    assertEquals("  ✓✓", ChatLogic.deliveryTick("delivered", true))
    assertEquals("  ✓✓", ChatLogic.deliveryTick("read", true))
    assertEquals("", ChatLogic.deliveryTick("sent", false))
    assertEquals("Typing…", ChatLogic.typingLabel(1))
    assertEquals("3 people typing…", ChatLogic.typingLabel(3))
    assertEquals(listOf("sarah", "everyone"), ChatLogic.extractMentions("hey @Sarah and @everyone"))
    assertEquals("Busy", ChatLogic.presenceLabel(false, 0, "x", 1, "busy"))
  }

  @Test
  fun storyAudienceIsFriendsOrOnlyMe() {
    assertEquals("friends", ChatLogic.normalizeStoryAudience(null))
    assertEquals("friends", ChatLogic.normalizeStoryAudience("followers"))
    assertEquals("friends", ChatLogic.normalizeStoryAudience("public"))
    assertEquals("friends", ChatLogic.normalizeStoryAudience("everyone"))
    assertEquals("private", ChatLogic.normalizeStoryAudience("private"))
    assertEquals("private", ChatLogic.normalizeStoryAudience("PRIVATE"))
    assertEquals(listOf("friends", "private"), ChatLogic.storyAudiences)
    assertTrue(ChatLogic.demoStories(1_000L).all {
      it.audience == "friends" || it.audience == "private"
    })
  }
}
