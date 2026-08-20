package app.organicmaps.chat.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Parsed chat records plus demo fallbacks used when Supabase is not configured. */
public final class ChatModels
{
  private ChatModels() {}

  public static class Conversation
  {
    @NonNull public final String id;
    @NonNull public final String kind;
    @NonNull public final String title;
    @Nullable public final String peerId;
    @Nullable public final String peerUsername;
    @Nullable public final String preview;
    @Nullable public final String lastMessageId;
    public final long lastMessageAt;
    public final int unread;
    public final boolean pinned;
    public final boolean verified;

    public Conversation(@NonNull String id, @NonNull String kind, @NonNull String title, @Nullable String peerId,
                        @Nullable String peerUsername, @Nullable String preview, @Nullable String lastMessageId,
                        long lastMessageAt, int unread, boolean pinned, boolean verified)
    {
      this.id = id;
      this.kind = kind;
      this.title = title;
      this.peerId = peerId;
      this.peerUsername = peerUsername;
      this.preview = preview;
      this.lastMessageId = lastMessageId;
      this.lastMessageAt = lastMessageAt;
      this.unread = unread;
      this.pinned = pinned;
      this.verified = verified;
    }

    @NonNull
    public String initials()
    {
      final String source = title.replace("✓", "").trim();
      if (source.isEmpty())
        return "?";
      final String[] parts = source.split("\\s+");
      if (parts.length == 1)
        return parts[0].substring(0, 1).toUpperCase(Locale.US);
      return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }
  }

  public static class Message
  {
    @NonNull public final String id;
    @NonNull public final String conversationId;
    @NonNull public final String senderId;
    @NonNull public final String senderName;
    @NonNull public final String kind;
    @Nullable public final String body;
    @Nullable public final String clientOperationId;
    public final long sentAt;
    public final boolean mine;
    public final boolean deleted;
    @NonNull public final String state;

    public Message(@NonNull String id, @NonNull String conversationId, @NonNull String senderId,
                   @NonNull String senderName, @NonNull String kind, @Nullable String body,
                   @Nullable String clientOperationId, long sentAt, boolean mine, boolean deleted,
                   @NonNull String state)
    {
      this.id = id;
      this.conversationId = conversationId;
      this.senderId = senderId;
      this.senderName = senderName;
      this.kind = kind;
      this.body = body;
      this.clientOperationId = clientOperationId;
      this.sentAt = sentAt;
      this.mine = mine;
      this.deleted = deleted;
      this.state = state;
    }
  }

  public static class Story
  {
    @NonNull public final String id;
    @NonNull public final String authorName;
    @NonNull public final String authorUsername;
    public final boolean viewed;
    public final boolean verified;

    public Story(@NonNull String id, @NonNull String authorName, @NonNull String authorUsername,
                 boolean viewed, boolean verified)
    {
      this.id = id;
      this.authorName = authorName;
      this.authorUsername = authorUsername;
      this.viewed = viewed;
      this.verified = verified;
    }
  }

  public static class Profile
  {
    @NonNull public final String id;
    @NonNull public final String username;
    @NonNull public final String displayName;
    public final boolean verified;

    public Profile(@NonNull String id, @NonNull String username, @NonNull String displayName, boolean verified)
    {
      this.id = id;
      this.username = username;
      this.displayName = displayName;
      this.verified = verified;
    }
  }

  @NonNull
  public static List<Conversation> parseInbox(@NonNull JSONArray rows)
  {
    final List<Conversation> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); ++i)
    {
      final Conversation conversation = parseConversation(rows.optJSONObject(i));
      if (conversation != null)
        result.add(conversation);
    }
    return result;
  }

  @Nullable
  public static Conversation parseConversation(@Nullable JSONObject row)
  {
    if (row == null)
      return null;
    final String id = firstNonEmpty(row, "conversation_id", "id");
    if (id.isEmpty())
      return null;
    return new Conversation(
        id,
        row.optString("kind", "direct"),
        firstNonEmpty(row, "title", "peer_display_name", "Chat"),
        emptyToNull(row.optString("peer_id", "")),
        emptyToNull(row.optString("peer_username", "")),
        emptyToNull(row.optString("last_message_body", row.optString("preview", ""))),
        emptyToNull(row.optString("last_message_id", "")),
        parseTime(firstNonEmpty(row, "last_message_at", "sent_at")),
        row.optInt("unread_count", row.optInt("unread", 0)),
        row.optBoolean("pinned", false),
        row.optBoolean("peer_verified", row.optBoolean("verified", false)));
  }

  @NonNull
  public static List<Message> parseMessages(@NonNull JSONArray rows, @Nullable String selfId)
  {
    final List<Message> result = new ArrayList<>();
    for (int i = rows.length() - 1; i >= 0; --i)
    {
      final Message message = parseMessage(rows.optJSONObject(i), selfId);
      if (message != null)
        result.add(message);
    }
    return result;
  }

  @Nullable
  public static Message parseMessage(@Nullable JSONObject row, @Nullable String selfId)
  {
    if (row == null)
      return null;
    final String id = firstNonEmpty(row, "id", "client_operation_id");
    if (id.isEmpty())
      return null;
    final String senderId = row.optString("sender_id", "");
    final boolean deleted = !row.isNull("deleted_at") && row.optString("deleted_at").length() > 0;
    return new Message(
        id,
        row.optString("conversation_id", ""),
        senderId,
        row.optString("sender_name", ""),
        row.optString("kind", "text"),
        deleted ? null : emptyToNull(row.optString("body", "")),
        emptyToNull(row.optString("client_operation_id", "")),
        parseTime(row.optString("sent_at", "")),
        selfId != null && selfId.equals(senderId),
        deleted,
        row.optString("state", "sent"));
  }

  @NonNull
  public static List<Story> parseStories(@NonNull JSONArray rows)
  {
    final List<Story> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); ++i)
    {
      final JSONObject row = rows.optJSONObject(i);
      if (row == null)
        continue;
      result.add(new Story(row.optString("id"), row.optString("author_name"),
                           row.optString("author_username"), row.optBoolean("viewed"),
                           row.optBoolean("author_verified")));
    }
    return result;
  }

  @NonNull
  public static List<Profile> parseProfiles(@NonNull JSONArray rows)
  {
    final List<Profile> result = new ArrayList<>();
    for (int i = 0; i < rows.length(); ++i)
    {
      final JSONObject row = rows.optJSONObject(i);
      if (row == null)
        continue;
      result.add(new Profile(row.optString("id"), row.optString("username"),
                             row.optString("display_name"), row.optBoolean("is_verified")));
    }
    return result;
  }

  @NonNull
  public static List<Conversation> demoInbox()
  {
    final long now = System.currentTimeMillis();
    final List<Conversation> result = new ArrayList<>();
    result.add(new Conversation("demo-nightguard", "nightguard", "NightGuard", null, null,
                                "Your safety check is ready", null, now, 0, true, true));
    result.add(new Conversation("demo-alfred", "direct", "Alfred M.", "demo-alfred-user", "alfredm",
                                "I’m at Cocoon — pull through 🔥", null, now - 120_000L, 1, false, true));
    result.add(new Conversation("demo-truth", "venue", "Truth Nightclub", null, "truth",
                                "Amapiano Fridays starts at 22:00", null, now - 18 * 60_000L, 0, false, true));
    result.add(new Conversation("demo-group", "group", "Joburg Fridays", null, null,
                                "Nomsa: Who’s joining after dinner?", null, now - 3600_000L, 0, false, false));
    result.add(new Conversation("demo-lerato", "direct", "Lerato", "demo-lerato-user", "lerato",
                                "Shared a Moment", null, now - 3 * 3600_000L, 0, false, false));
    return result;
  }

  @NonNull
  public static List<Message> demoMessages(@NonNull String conversationId, @Nullable String selfId)
  {
    final long now = System.currentTimeMillis();
    final List<Message> result = new ArrayList<>();
    result.add(new Message(conversationId + "-1", conversationId, "peer", "Alfred", "text",
                           "I’m at Cocoon — pull through 🔥", null, now - 180_000L, false, false, "sent"));
    result.add(new Message(conversationId + "-2", conversationId, "peer", "Alfred", "venue",
                           "📍 Cocoon Nightclub  ✓\n0.8 km • Open now • Crowd: High", null,
                           now - 120_000L, false, false, "sent"));
    result.add(new Message(conversationId + "-3", conversationId, selfId == null ? "me" : selfId, "You",
                           "text", "I’ll be there soon", null, now - 60_000L, true, false, "read"));
    return result;
  }

  @NonNull
  public static List<Story> demoStories()
  {
    final List<Story> result = new ArrayList<>();
    result.add(new Story("demo-story-alfred", "Alfred", "alfredm", false, true));
    result.add(new Story("demo-story-nomsa", "Nomsa", "nomsa", false, false));
    result.add(new Story("demo-story-vault", "The Vault", "thevault", false, true));
    return result;
  }

  @NonNull
  public static String relativeTime(long epochMs)
  {
    if (epochMs <= 0)
      return "";
    final long delta = Math.max(0L, System.currentTimeMillis() - epochMs);
    if (delta < 15_000L)
      return "Now";
    if (delta < 60_000L)
      return (delta / 1000L) + "s";
    if (delta < 3_600_000L)
      return (delta / 60_000L) + "m";
    if (delta < 86_400_000L)
      return (delta / 3_600_000L) + "h";
    if (delta < 7 * 86_400_000L)
      return (delta / 86_400_000L) + "d";
    return Instant.ofEpochMilli(epochMs).toString().substring(0, 10);
  }

  public static long parseTime(@Nullable String iso)
  {
    if (iso == null || iso.isEmpty() || "null".equals(iso))
      return 0L;
    try
    {
      return Instant.parse(iso).toEpochMilli();
    }
    catch (RuntimeException ignored)
    {
      try
      {
        return Long.parseLong(iso);
      }
      catch (NumberFormatException e)
      {
        return 0L;
      }
    }
  }

  @NonNull
  static String firstNonEmpty(@NonNull JSONObject row, @NonNull String... keys)
  {
    for (String key : keys)
    {
      final String value = row.optString(key, "");
      if (value != null && !value.isEmpty() && !"null".equals(value))
        return value;
    }
    return "";
  }

  @Nullable
  static String emptyToNull(@Nullable String value)
  {
    return value == null || value.isEmpty() || "null".equals(value) ? null : value;
  }
}
