package app.organicmaps.chat.data;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.chat.realtime.ChatConnectionState;
import app.organicmaps.chat.realtime.ChatRealtimeEngine;
import java.util.List;

/** Binds a conversation thread: cache-first open, optimistic send, realtime hints. */
public class ChatConversationBinder implements ChatServices.Listener
{
  public interface Host
  {
    void openCall(@NonNull String mode, @NonNull String name);
    void openProfile(@NonNull String username);
    void showNotice(@NonNull String message);
  }

  private final Fragment mFragment;
  private final View mRoot;
  private final String mConversationId;
  private final Host mHost;
  private final ChatServices mServices;
  private final ChatRepository mRepository;
  private final Runnable mPoll = this::refresh;
  private boolean mTypingSent;

  public ChatConversationBinder(@NonNull Fragment fragment, @NonNull View root, @NonNull String conversationId,
                                @NonNull Host host)
  {
    mFragment = fragment;
    mRoot = root;
    mConversationId = conversationId;
    mHost = host;
    mServices = ChatServices.get(fragment.requireContext());
    mRepository = mServices.repository();
  }

  public void start()
  {
    mServices.addListener(this);
    bindHeader();
    bindComposer();
    loop();
    final ChatRealtimeEngine engine = mServices.engine();
    if (engine != null && !mConversationId.startsWith("demo-"))
      engine.joinConversation(mConversationId);
    mRoot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener()
    {
      @Override public void onViewAttachedToWindow(@NonNull View view) {}
      @Override public void onViewDetachedFromWindow(@NonNull View view)
      {
        mServices.removeListener(ChatConversationBinder.this);
        mRoot.removeCallbacks(mPoll);
        final ChatRealtimeEngine live = mServices.engine();
        if (live != null)
        {
          live.setTyping(mConversationId, false);
          live.leaveConversation(mConversationId);
        }
        final EditText input = mRoot.findViewById(R.id.conversation_input);
        mRepository.store().saveDraft(mConversationId, input.getText().toString());
      }
    });
  }

  private void bindHeader()
  {
    final ChatModels.Conversation conversation = mRepository.conversation(mConversationId);
    final String title = conversation == null ? mConversationId : conversation.title;
    ((TextView) mRoot.findViewById(R.id.conversation_name)).setText(title);
    ((TextView) mRoot.findViewById(R.id.conversation_avatar)).setText(
        conversation == null ? "?" : conversation.initials());
    final String username = conversation == null || conversation.peerUsername == null
        ? "" : "@" + conversation.peerUsername;
    ((TextView) mRoot.findViewById(R.id.conversation_presence)).setText(
        username.isEmpty() ? mFragment.getString(R.string.chats_connecting) : username);
    mRoot.findViewById(R.id.conversation_back).setOnClickListener(
        v -> mFragment.getParentFragmentManager().popBackStack());
    mRoot.findViewById(R.id.conversation_voice).setOnClickListener(v -> mHost.openCall("outgoing_voice", title));
    mRoot.findViewById(R.id.conversation_video).setOnClickListener(v -> mHost.openCall("outgoing_video", title));
    mRoot.findViewById(R.id.conversation_header).setOnClickListener(v -> {
      if (conversation != null && conversation.peerUsername != null)
        mHost.openProfile(conversation.peerUsername);
    });
    mRoot.findViewById(R.id.conversation_menu).setOnClickListener(this::showMenu);
    mRoot.findViewById(R.id.conversation_attach).setOnClickListener(
        v -> mHost.showNotice("Camera • Gallery • Document • Venue • Event • Location"));
    mRoot.findViewById(R.id.conversation_cancel_reply).setOnClickListener(
        v -> mRoot.findViewById(R.id.conversation_reply).setVisibility(View.GONE));
  }

  private void bindComposer()
  {
    final EditText input = mRoot.findViewById(R.id.conversation_input);
    final TextView send = mRoot.findViewById(R.id.conversation_send);
    input.setText(mRepository.store().loadDraft(mConversationId));
    send.setText(input.getText().length() == 0 ? "🎤" : "↑");
    input.addTextChangedListener(new TextWatcher()
    {
      @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence text, int start, int before, int count)
      {
        send.setText(text.length() == 0 ? "🎤" : "↑");
        final ChatRealtimeEngine engine = mServices.engine();
        if (engine == null)
          return;
        if (text.length() > 0 && !mTypingSent)
        {
          engine.setTyping(mConversationId, true);
          mTypingSent = true;
        }
        else if (text.length() == 0 && mTypingSent)
        {
          engine.setTyping(mConversationId, false);
          mTypingSent = false;
        }
      }
      @Override public void afterTextChanged(Editable text) {}
    });
    send.setOnClickListener(v -> {
      final String text = input.getText().toString().trim();
      if (text.isEmpty())
      {
        mHost.showNotice("Hold to record a voice note");
        return;
      }
      input.setText("");
      mRepository.store().saveDraft(mConversationId, "");
      mRepository.sendText(mConversationId, text);
      refresh();
    });
  }

  private void loop()
  {
    refresh();
    mRoot.postDelayed(mPoll, 3_000L);
  }

  private void refresh()
  {
    if (!mFragment.isAdded())
      return;
    mRepository.loadMessages(mConversationId, new ChatRepository.MessagesCallback()
    {
      @Override
      public void onLoaded(@NonNull List<ChatModels.Message> messages, @Nullable ChatModels.Conversation conversation)
      {
        runOnUi(() -> renderMessages(messages, conversation));
      }
      @Override
      public void onStatus(@NonNull String status)
      {
        runOnUi(() -> {
          final TextView view = mRoot.findViewById(R.id.conversation_status);
          view.setText(status);
          view.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        });
      }
    });
  }

  private void renderMessages(@NonNull List<ChatModels.Message> messages,
                              @Nullable ChatModels.Conversation conversation)
  {
    if (conversation != null)
    {
      ((TextView) mRoot.findViewById(R.id.conversation_name)).setText(conversation.title);
      ((TextView) mRoot.findViewById(R.id.conversation_avatar)).setText(conversation.initials());
    }
    final LinearLayout list = mRoot.findViewById(R.id.conversation_messages);
    list.removeAllViews();
    if (messages.isEmpty())
    {
      final TextView empty = new TextView(mFragment.requireContext());
      empty.setText("Say hello");
      empty.setTextColor(0x99000000);
      empty.setGravity(Gravity.CENTER);
      list.addView(empty);
      return;
    }
    for (ChatModels.Message message : messages)
      list.addView(bubble(message));
    final ScrollView scroll = mRoot.findViewById(R.id.conversation_scroll);
    scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
  }

  @NonNull
  private View bubble(@NonNull ChatModels.Message message)
  {
    final View view = LayoutInflater.from(mFragment.requireContext())
                          .inflate(R.layout.chat_bubble, mRoot.findViewById(R.id.conversation_messages), false);
    final TextView text = view.findViewById(R.id.chat_bubble_text);
    final TextView meta = view.findViewById(R.id.chat_bubble_meta);
    final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) text.getLayoutParams();
    if (message.mine)
    {
      params.gravity = Gravity.END;
      text.setBackgroundResource(R.drawable.chat_category_active);
      text.setTextColor(Color.WHITE);
      ((LinearLayout.LayoutParams) meta.getLayoutParams()).gravity = Gravity.END;
    }
    else
    {
      params.gravity = Gravity.START;
      text.setBackgroundResource(R.drawable.chat_avatar);
      text.setTextColor(0xDE000000);
    }
    text.setLayoutParams(params);
    if (message.deleted || message.body == null)
      text.setText(R.string.chats_deleted);
    else
      text.setText(message.body);
    final String ticks = message.mine
        ? ("queued".equals(message.state) ? "  ○" : "read".equals(message.state) ? "  ✓✓" : "  ✓")
        : "";
    meta.setText(ChatModels.relativeTime(message.sentAt) + ticks);
    view.setOnLongClickListener(v -> {
      mHost.showNotice("React • Reply • Copy • Delete");
      return true;
    });
    return view;
  }

  private void showMenu(@NonNull View anchor)
  {
    final PopupMenu menu = new PopupMenu(mFragment.requireContext(), anchor);
    for (String item : new String[] {"View Profile", "Search Conversation", "Shared Media", "Shared Files",
                                     "Shared Venues", "Mute Notifications", "Pin Conversation", "Block User",
                                     "Report User"})
      menu.getMenu().add(item);
    menu.setOnMenuItemClickListener(item -> {
      if ("Pin Conversation".contentEquals(item.getTitle()))
        mRepository.setPinned(mConversationId, true);
      else if ("View Profile".contentEquals(item.getTitle()))
      {
        final ChatModels.Conversation conversation = mRepository.conversation(mConversationId);
        if (conversation != null && conversation.peerUsername != null)
          mHost.openProfile(conversation.peerUsername);
      }
      else
        mHost.showNotice(String.valueOf(item.getTitle()));
      return true;
    });
    menu.show();
  }

  private void runOnUi(@NonNull Runnable action)
  {
    if (mFragment.isAdded())
      mFragment.requireActivity().runOnUiThread(action);
  }

  @Override public void onInboxHint() {}
  @Override public void onConversationHint(@NonNull String conversationId)
  {
    if (mConversationId.equals(conversationId))
      refresh();
  }
  @Override public void onTyping(@NonNull String conversationId, @NonNull String userId, boolean isTyping)
  {
    if (!mConversationId.equals(conversationId) || !mFragment.isAdded())
      return;
    runOnUi(() -> {
      final TextView presence = mRoot.findViewById(R.id.conversation_presence);
      if (isTyping)
        presence.setText(R.string.chats_typing);
      else
        bindHeader();
    });
  }
  @Override public void onConnectionState(@NonNull ChatConnectionState state) {}
}
