package app.organicmaps.chat.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class ChatModelsTest
{
  @Test
  public void parsesInboxRows() throws Exception
  {
    final JSONArray rows = new JSONArray().put(new JSONObject()
        .put("conversation_id", "c1")
        .put("kind", "direct")
        .put("title", "Nomsa")
        .put("peer_username", "nomsa")
        .put("last_message_body", "On my way")
        .put("unread_count", 2)
        .put("pinned", true)
        .put("peer_verified", true)
        .put("last_message_at", "2026-08-20T12:00:00Z"));
    final List<ChatModels.Conversation> inbox = ChatModels.parseInbox(rows);
    assertEquals(1, inbox.size());
    assertEquals("c1", inbox.get(0).id);
    assertEquals("Nomsa", inbox.get(0).title);
    assertEquals("nomsa", inbox.get(0).peerUsername);
    assertEquals(2, inbox.get(0).unread);
    assertTrue(inbox.get(0).pinned);
    assertTrue(inbox.get(0).verified);
    assertEquals("N", inbox.get(0).initials());
  }

  @Test
  public void reversesPaginatedMessagesIntoChronologicalOrder() throws Exception
  {
    final JSONArray rows = new JSONArray()
        .put(new JSONObject().put("id", "m2").put("sender_id", "me").put("body", "later")
                 .put("sent_at", "2026-08-20T12:01:00Z"))
        .put(new JSONObject().put("id", "m1").put("sender_id", "peer").put("body", "first")
                 .put("sent_at", "2026-08-20T12:00:00Z"));
    final List<ChatModels.Message> messages = ChatModels.parseMessages(rows, "me");
    assertEquals("m1", messages.get(0).id);
    assertEquals("m2", messages.get(1).id);
    assertTrue(messages.get(1).mine);
    assertFalse(messages.get(0).mine);
  }

  @Test
  public void parseTimeReadsIsoAndEpoch()
  {
    assertEquals(0L, ChatModels.parseTime(""));
    assertTrue(ChatModels.parseTime("2026-08-20T00:00:00Z") > 0L);
    assertEquals(42L, ChatModels.parseTime("42"));
  }

  @Test
  public void demoInboxKeepsPinnedNightGuardFirst()
  {
    final List<ChatModels.Conversation> inbox = ChatModels.demoInbox();
    assertEquals("demo-nightguard", inbox.get(0).id);
    assertTrue(inbox.get(0).pinned);
    assertEquals(5, inbox.size());
  }

  @Test
  public void sanitizesUsernamesFromEmail()
  {
    assertEquals("alfred.m", FomoAuthRepository.usernameFromEmail("Alfred.M@example.com"));
    assertTrue(FomoAuthRepository.usernameFromEmail("ab@x.com").startsWith("user"));
    assertEquals("Alfred", FomoAuthRepository.displayNameFromEmail("alfred@example.com"));
  }
}
