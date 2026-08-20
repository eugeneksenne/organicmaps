package app.organicmaps.chat.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.BuildConfig;
import app.organicmaps.chat.realtime.ChatLocalStore;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Offline-first chats repository. Supabase RPCs are the source of truth. */
public class ChatRepository
{
  public interface InboxCallback
  {
    void onLoaded(@NonNull List<ChatModels.Conversation> conversations, @NonNull List<ChatModels.Story> stories,
                  boolean live);
    void onStatus(@NonNull String status);
  }

  public interface MessagesCallback
  {
    void onLoaded(@NonNull List<ChatModels.Message> messages, @Nullable ChatModels.Conversation conversation);
    void onStatus(@NonNull String status);
  }

  public interface ProfilesCallback
  {
    void onLoaded(@NonNull List<ChatModels.Profile> profiles);
    void onError(@NonNull String message);
  }

  public interface IdCallback
  {
    void onReady(@NonNull String id);
    void onError(@NonNull String message);
  }

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient;
  private final FomoAuthSession mSession;
  private final FomoAuthRepository mAuth;
  private final ChatLocalStore mStore;
  @Nullable private final ChatRealtimeEngine mEngine;

  public ChatRepository(@NonNull OkHttpClient client, @NonNull FomoAuthSession session,
                        @NonNull FomoAuthRepository auth, @NonNull ChatLocalStore store,
                        @Nullable ChatRealtimeEngine engine)
  {
    mClient = client;
    mSession = session;
    mAuth = auth;
    mStore = store;
    mEngine = engine;
  }

  public boolean isConfigured() { return mAuth.isConfigured(); }
  public boolean isSignedIn() { return mSession.isSignedIn(); }
  @NonNull public FomoAuthSession session() { return mSession; }
  @NonNull public ChatLocalStore store() { return mStore; }
  @Nullable public ChatRealtimeEngine engine() { return mEngine; }

  public void loadInbox(@NonNull InboxCallback callback)
  {
    final List<ChatModels.Conversation> cached = mStore.loadInbox();
    final List<ChatModels.Story> cachedStories = mStore.loadStories();
    if (!cached.isEmpty() || !cachedStories.isEmpty())
      callback.onLoaded(cached, cachedStories, false);
    if (!isConfigured())
    {
      final List<ChatModels.Conversation> demo = ChatModels.demoInbox();
      final List<ChatModels.Story> stories = ChatModels.demoStories();
      mStore.replaceInbox(demo);
      mStore.replaceStories(stories);
      callback.onLoaded(demo, stories, false);
      callback.onStatus("Demo mode. Add android/fomo.properties to use a live backend.");
      return;
    }
    if (!isSignedIn())
    {
      if (cached.isEmpty())
        callback.onLoaded(new ArrayList<>(), new ArrayList<>(), false);
      callback.onStatus("Sign in to sync your conversations.");
      return;
    }
    new Thread(() -> {
      if (!mAuth.refreshIfNeeded())
      {
        callback.onStatus("Session expired. Sign in again.");
        return;
      }
      try
      {
        final JSONArray inbox = rpcArray("chat_inbox", new JSONObject());
        final JSONArray stories = rpcArray("inbox_stories", new JSONObject());
        final List<ChatModels.Conversation> conversations = ChatModels.parseInbox(inbox);
        final List<ChatModels.Story> storyList = ChatModels.parseStories(stories);
        mStore.replaceInbox(conversations);
        mStore.replaceStories(storyList);
        callback.onLoaded(conversations, storyList, true);
        callback.onStatus("");
        if (mEngine != null)
          mEngine.flushOutbox();
      }
      catch (Exception e)
      {
        if (cached.isEmpty())
          callback.onLoaded(mStore.loadInbox(), mStore.loadStories(), false);
        callback.onStatus("Couldn't refresh chats. Showing saved conversations.");
      }
    }).start();
  }

  public void loadMessages(@NonNull String conversationId, @NonNull MessagesCallback callback)
  {
    final String selfId = mSession.getUserId();
    List<ChatModels.Message> cached = mStore.loadMessages(conversationId, selfId);
    ChatModels.Conversation conversation = mStore.loadConversation(conversationId);
    if (conversation == null)
      conversation = findDemoConversation(conversationId);
    if (conversationId.startsWith("demo-") && cached.isEmpty())
    {
      cached = ChatModels.demoMessages(conversationId, selfId);
      mStore.replaceMessages(conversationId, cached);
    }
    if (!cached.isEmpty())
      callback.onLoaded(cached, conversation);
    if (!isConfigured() || !isSignedIn() || conversationId.startsWith("demo-"))
      return;
    new Thread(() -> {
      try
      {
        if (!mAuth.refreshIfNeeded())
        {
          callback.onStatus("Session expired.");
          return;
        }
        final JSONObject body = new JSONObject().put("target", conversationId).put("page_size", 80);
        final JSONArray rows = rpcArray("chat_messages", body);
        final List<ChatModels.Message> messages = ChatModels.parseMessages(rows, mSession.getUserId());
        mStore.replaceMessages(conversationId, messages);
        callback.onLoaded(messages, mStore.loadConversation(conversationId));
        callback.onStatus("");
        if (!messages.isEmpty())
          markRead(conversationId, messages.get(messages.size() - 1).id);
      }
      catch (Exception e)
      {
        callback.onStatus("Couldn't refresh this conversation.");
      }
    }).start();
  }

  @NonNull
  public ChatModels.Message sendText(@NonNull String conversationId, @NonNull String text)
  {
    final String operationId = UUID.randomUUID().toString();
    final String selfId = mSession.getUserId() == null ? "me" : mSession.getUserId();
    final ChatModels.Message optimistic = new ChatModels.Message(
        operationId, conversationId, selfId, "You", "text", text, operationId,
        System.currentTimeMillis(), true, false, "queued");
    mStore.upsertMessage(optimistic);
    final ChatModels.Conversation existing = mStore.loadConversation(conversationId);
    if (existing != null)
    {
      mStore.upsertConversation(new ChatModels.Conversation(
          existing.id, existing.kind, existing.title, existing.peerId, existing.peerUsername, text,
          optimistic.id, optimistic.sentAt, 0, existing.pinned, existing.verified));
    }
    if (conversationId.startsWith("demo-") || !isConfigured() || !isSignedIn())
      return optimistic;
    try
    {
      final JSONObject payload = new JSONObject()
          .put("conversationId", conversationId)
          .put("kind", "text")
          .put("body", text);
      if (mEngine != null)
        mEngine.queueOperation(operationId, "message", payload);
      else
        dispatchDirect(operationId, "message", payload);
    }
    catch (JSONException ignored) {}
    return optimistic;
  }

  public void searchPeople(@NonNull String query, @NonNull ProfilesCallback callback)
  {
    if (!isConfigured() || !isSignedIn())
    {
      callback.onError("Sign in to find people.");
      return;
    }
    new Thread(() -> {
      try
      {
        mAuth.refreshIfNeeded();
        final JSONArray rows = rpcArray("search_chat_profiles", new JSONObject().put("query", query));
        callback.onLoaded(ChatModels.parseProfiles(rows));
      }
      catch (Exception e)
      {
        callback.onError("Couldn't search people.");
      }
    }).start();
  }

  public void openDirect(@NonNull String userId, @NonNull IdCallback callback)
  {
    mutateId("open_direct_conversation", json -> json.put("other_user", userId), callback);
  }

  public void createGroup(@NonNull String title, @NonNull IdCallback callback)
  {
    mutateId("create_group_conversation", json -> json.put("p_title", title).put("member_ids", new JSONArray()),
             callback);
  }

  public void markRead(@NonNull String conversationId, @NonNull String messageId)
  {
    if (!isConfigured() || !isSignedIn() || conversationId.startsWith("demo-"))
      return;
    new Thread(() -> {
      try
      {
        rpc("mark_chat_read", new JSONObject().put("target", conversationId).put("message_id", messageId));
      }
      catch (Exception ignored) {}
    }).start();
  }

  public void setPinned(@NonNull String conversationId, boolean pinned)
  {
    if (!isConfigured() || !isSignedIn() || conversationId.startsWith("demo-"))
      return;
    new Thread(() -> {
      try
      {
        rpc("set_conversation_pin", new JSONObject().put("target", conversationId).put("pinned", pinned));
      }
      catch (Exception ignored) {}
    }).start();
  }

  @Nullable
  public ChatModels.Conversation conversation(@NonNull String id)
  {
    final ChatModels.Conversation cached = mStore.loadConversation(id);
    return cached != null ? cached : findDemoConversation(id);
  }

  private void mutateId(@NonNull String rpcName, @NonNull JsonBuilder builder, @NonNull IdCallback callback)
  {
    if (!isConfigured() || !isSignedIn())
    {
      callback.onError("Sign in first.");
      return;
    }
    new Thread(() -> {
      try
      {
        mAuth.refreshIfNeeded();
        final String raw = rpc(rpcName, builder.build(new JSONObject()));
        final String id = raw.replace("\"", "").trim();
        if (id.isEmpty())
          callback.onError("Could not open conversation.");
        else
          callback.onReady(id);
      }
      catch (Exception e)
      {
        callback.onError("Could not open conversation.");
      }
    }).start();
  }

  private interface JsonBuilder { JSONObject build(JSONObject object) throws JSONException; }

  private void dispatchDirect(@NonNull String id, @NonNull String type, @NonNull JSONObject payload)
      throws JSONException
  {
    rpc("apply_chat_operation", new JSONObject().put("operation_id", id).put("operation_type", type)
                                    .put("payload", payload));
  }

  @NonNull
  private JSONArray rpcArray(@NonNull String name, @NonNull JSONObject body) throws IOException, JSONException
  {
    final String raw = rpc(name, body);
    final String trimmed = raw.trim();
    if (trimmed.startsWith("["))
      return new JSONArray(trimmed);
    if (trimmed.isEmpty() || "null".equals(trimmed))
      return new JSONArray();
    return new JSONArray().put(new JSONObject(trimmed));
  }

  @NonNull
  private String rpc(@NonNull String name, @NonNull JSONObject body) throws IOException
  {
    final String token = mSession.getAccessToken();
    if (token == null)
      throw new IOException("signed_out");
    final Request request = new Request.Builder()
        .url(BuildConfig.FOMO_SUPABASE_URL.replaceAll("/+$", "") + "/rest/v1/rpc/" + name)
        .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("Authorization", "Bearer " + token)
        .header("Content-Type", "application/json")
        .post(RequestBody.create(body.toString(), JSON))
        .build();
    try (Response response = mClient.newCall(request).execute())
    {
      final String raw = response.body() == null ? "" : response.body().string();
      if (!response.isSuccessful())
        throw new IOException(raw);
      return raw;
    }
  }

  @Nullable
  private static ChatModels.Conversation findDemoConversation(@NonNull String id)
  {
    for (ChatModels.Conversation conversation : ChatModels.demoInbox())
    {
      if (conversation.id.equals(id))
        return conversation;
    }
    return new ChatModels.Conversation(id, "direct", id.startsWith("demo-") ? "Chat" : "Conversation",
                                       null, null, null, null, System.currentTimeMillis(), 0, false, false);
  }
}
