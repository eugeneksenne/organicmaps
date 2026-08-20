package app.organicmaps.chat.data;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.chat.realtime.ChatConnectionState;
import java.util.List;

/** Binds the chats inbox to the repository, cache, and optional realtime engine. */
public class ChatInboxBinder implements ChatServices.Listener
{
  public interface Host
  {
    void openConversation(@NonNull String conversationId);
    void openStories();
    void openStory(@NonNull String name);
    void openCalls();
    void openGroups();
  }

  private final Fragment mFragment;
  private final View mRoot;
  private final Host mHost;
  private final ChatServices mServices;
  private final ChatRepository mRepository;
  private final Runnable mRefresh = this::loop;
  private boolean mAttached;

  public ChatInboxBinder(@NonNull Fragment fragment, @NonNull View root, @NonNull Host host)
  {
    mFragment = fragment;
    mRoot = root;
    mHost = host;
    mServices = ChatServices.get(fragment.requireContext());
    mRepository = mServices.repository();
  }

  public void start()
  {
    mAttached = true;
    mServices.addListener(this);
    mServices.onForeground();
    bindChrome();
    loop();
    mRoot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener()
    {
      @Override public void onViewAttachedToWindow(@NonNull View view) {}
      @Override public void onViewDetachedFromWindow(@NonNull View view)
      {
        mAttached = false;
        mServices.removeListener(ChatInboxBinder.this);
        mRoot.removeCallbacks(mRefresh);
      }
    });
  }

  private void bindChrome()
  {
    mRoot.findViewById(R.id.chats_search).setOnClickListener(v -> showPeoplePicker(false));
    mRoot.findViewById(R.id.chats_new).setOnClickListener(v -> showComposerMenu(v));
    mRoot.findViewById(R.id.chats_account).setOnClickListener(v -> showAccountMenu(v));
    final LinearLayout categories = mRoot.findViewById(R.id.chats_categories);
    for (int i = 0; i < categories.getChildCount(); ++i)
    {
      final TextView category = (TextView) categories.getChildAt(i);
      category.setOnClickListener(v -> {
        if (mFragment.getString(R.string.chats_calls).contentEquals(category.getText()))
          mHost.openCalls();
        else if (mFragment.getString(R.string.chats_groups).contentEquals(category.getText()))
          mHost.openGroups();
      });
    }
    mRoot.findViewById(R.id.chats_auth_signin).setOnClickListener(v -> submitAuth(false));
    mRoot.findViewById(R.id.chats_auth_signup).setOnClickListener(v -> submitAuth(true));
    renderAuth();
  }

  private void loop()
  {
    refresh();
    if (mAttached)
      mRoot.postDelayed(mRefresh, 8_000L);
  }

  private void refresh()
  {
    if (!mAttached || !mFragment.isAdded())
      return;
    mRepository.loadInbox(new ChatRepository.InboxCallback()
    {
      @Override
      public void onLoaded(@NonNull List<ChatModels.Conversation> conversations,
                           @NonNull List<ChatModels.Story> stories, boolean live)
      {
        runOnUi(() -> renderInbox(conversations, stories));
      }

      @Override
      public void onStatus(@NonNull String status)
      {
        runOnUi(() -> setStatus(status));
      }
    });
  }

  private void renderInbox(@NonNull List<ChatModels.Conversation> conversations,
                           @NonNull List<ChatModels.Story> stories)
  {
    renderStories(stories);
    final LinearLayout pinned = mRoot.findViewById(R.id.chats_pinned);
    final LinearLayout recent = mRoot.findViewById(R.id.chats_recent);
    pinned.removeAllViews();
    recent.removeAllViews();
    int pinnedCount = 0;
    for (ChatModels.Conversation conversation : conversations)
    {
      if (conversation.pinned)
      {
        pinned.addView(row(conversation));
        pinnedCount++;
      }
      else
        recent.addView(row(conversation));
    }
    mRoot.findViewById(R.id.chats_pinned_label).setVisibility(pinnedCount == 0 ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.chats_recent_label).setVisibility(recent.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.chats_empty).setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
    renderAuth();
  }

  private void renderStories(@NonNull List<ChatModels.Story> stories)
  {
    final LinearLayout row = mRoot.findViewById(R.id.chats_stories);
    row.removeAllViews();
    final LayoutInflater inflater = LayoutInflater.from(mFragment.requireContext());
    final View add = inflater.inflate(R.layout.chat_story_item, row, false);
    ((TextView) add.findViewById(R.id.chat_story_avatar)).setText("F");
    ((TextView) add.findViewById(R.id.chat_story_name)).setText(R.string.chats_my_story);
    add.setOnClickListener(v -> mHost.openStories());
    row.addView(add);
    for (ChatModels.Story story : stories)
    {
      final View item = inflater.inflate(R.layout.chat_story_item, row, false);
      final String name = story.authorName.isEmpty() ? story.authorUsername : story.authorName;
      ((TextView) item.findViewById(R.id.chat_story_avatar)).setText(name.substring(0, 1).toUpperCase());
      ((TextView) item.findViewById(R.id.chat_story_name)).setText(name);
      item.setOnClickListener(v -> mHost.openStory(name));
      row.addView(item);
    }
  }

  @NonNull
  private View row(@NonNull ChatModels.Conversation conversation)
  {
    final View view = LayoutInflater.from(mFragment.requireContext())
                          .inflate(R.layout.chat_row, (ViewGroup) mRoot.findViewById(R.id.chats_recent), false);
    ((TextView) view.findViewById(R.id.chat_row_avatar)).setText(conversation.initials());
    ((TextView) view.findViewById(R.id.chat_row_name)).setText(
        conversation.verified ? conversation.title + "  ✓" : conversation.title);
    ((TextView) view.findViewById(R.id.chat_row_preview)).setText(
        conversation.preview == null || conversation.preview.isEmpty() ? " " : conversation.preview);
    ((TextView) view.findViewById(R.id.chat_row_time)).setText(ChatModels.relativeTime(conversation.lastMessageAt));
    view.findViewById(R.id.chat_row_unread).setVisibility(conversation.unread > 0 ? View.VISIBLE : View.GONE);
    view.setContentDescription(conversation.title);
    view.setOnClickListener(v -> mHost.openConversation(conversation.id));
    view.setOnLongClickListener(v -> {
      mRepository.setPinned(conversation.id, !conversation.pinned);
      Toast.makeText(mFragment.requireContext(),
                     conversation.pinned ? "Unpinned" : "Pinned", Toast.LENGTH_SHORT).show();
      refresh();
      return true;
    });
    return view;
  }

  private void renderAuth()
  {
    final boolean show = mRepository.isConfigured() && !mRepository.isSignedIn();
    mRoot.findViewById(R.id.chats_auth).setVisibility(show ? View.VISIBLE : View.GONE);
    final TextView account = mRoot.findViewById(R.id.chats_account);
    final String name = mServices.session().getDisplayName();
    account.setText(name == null || name.isEmpty() ? "☺" : name.substring(0, 1).toUpperCase());
  }

  private void submitAuth(boolean signUp)
  {
    final String email = ((EditText) mRoot.findViewById(R.id.chats_auth_email)).getText().toString().trim();
    final String password = ((EditText) mRoot.findViewById(R.id.chats_auth_password)).getText().toString();
    if (email.isEmpty() || password.length() < 6)
    {
      Toast.makeText(mFragment.requireContext(), "Enter email and a password of at least 6 characters",
                     Toast.LENGTH_SHORT).show();
      return;
    }
    final FomoAuthRepository.Callback callback = new FomoAuthRepository.Callback()
    {
      @Override public void onSuccess()
      {
        runOnUi(() -> {
          mServices.onSignedIn();
          renderAuth();
          refresh();
        });
      }
      @Override public void onError(@NonNull String message)
      {
        runOnUi(() -> Toast.makeText(mFragment.requireContext(), message, Toast.LENGTH_LONG).show());
      }
    };
    if (signUp)
      mServices.auth().signUp(email, password, callback);
    else
      mServices.auth().signIn(email, password, callback);
  }

  private void showComposerMenu(@NonNull View anchor)
  {
    if (mRepository.isConfigured() && !mRepository.isSignedIn())
    {
      mRoot.findViewById(R.id.chats_auth).setVisibility(View.VISIBLE);
      return;
    }
    final PopupMenu menu = new PopupMenu(mFragment.requireContext(), anchor);
    menu.getMenu().add(mFragment.getString(R.string.chats_search_people));
    menu.getMenu().add(mFragment.getString(R.string.chats_new_group));
    menu.setOnMenuItemClickListener(item -> {
      if (mFragment.getString(R.string.chats_new_group).contentEquals(item.getTitle()))
        showCreateGroup();
      else
        showPeoplePicker(true);
      return true;
    });
    menu.show();
  }

  private void showAccountMenu(@NonNull View anchor)
  {
    final PopupMenu menu = new PopupMenu(mFragment.requireContext(), anchor);
    if (mRepository.isSignedIn())
    {
      final String email = mServices.session().getEmail();
      menu.getMenu().add(email == null ? mFragment.getString(R.string.chats_sign_out)
                                       : email);
      menu.getMenu().add(mFragment.getString(R.string.chats_sign_out));
    }
    else
      menu.getMenu().add(mFragment.getString(R.string.login));
    menu.setOnMenuItemClickListener(item -> {
      if (mFragment.getString(R.string.chats_sign_out).contentEquals(item.getTitle()))
      {
        mServices.auth().signOut();
        mServices.onSignedOut();
        renderAuth();
        refresh();
      }
      else if (!mRepository.isSignedIn())
        mRoot.findViewById(R.id.chats_auth).setVisibility(View.VISIBLE);
      return true;
    });
    menu.show();
  }

  private void showPeoplePicker(boolean startChat)
  {
    final Context context = mFragment.requireContext();
    final EditText input = new EditText(context);
    input.setHint(R.string.chats_search_people);
    input.setInputType(InputType.TYPE_CLASS_TEXT);
    final LinearLayout container = new LinearLayout(context);
    container.setOrientation(LinearLayout.VERTICAL);
    container.addView(input);
    final LinearLayout results = new LinearLayout(context);
    results.setOrientation(LinearLayout.VERTICAL);
    container.addView(results);
    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setTitle(R.string.chats_search_people)
        .setView(container)
        .setNegativeButton(R.string.cancel, null)
        .show();
    input.addTextChangedListener(new SimpleWatcher(query -> mRepository.searchPeople(query, new ChatRepository.ProfilesCallback()
    {
      @Override public void onLoaded(@NonNull List<ChatModels.Profile> profiles)
      {
        runOnUi(() -> {
          results.removeAllViews();
          for (ChatModels.Profile profile : profiles)
          {
            final TextView row = new TextView(context);
            row.setPadding(24, 24, 24, 24);
            row.setText(profile.displayName + "  @" + profile.username);
            row.setTextColor(0xDE000000);
            row.setOnClickListener(v -> {
              dialog.dismiss();
              if (startChat)
                openDirect(profile.id);
              else
                Toast.makeText(context, profile.displayName, Toast.LENGTH_SHORT).show();
            });
            results.addView(row);
          }
        });
      }
      @Override public void onError(@NonNull String message)
      {
        runOnUi(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
      }
    })));
  }

  private void showCreateGroup()
  {
    final EditText input = new EditText(mFragment.requireContext());
    input.setHint(R.string.chats_group_name);
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(R.string.chats_new_group)
        .setView(input)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.create, (d, w) -> {
          final String title = input.getText().toString().trim();
          if (title.isEmpty())
            return;
          mRepository.createGroup(title, new ChatRepository.IdCallback()
          {
            @Override public void onReady(@NonNull String id) { runOnUi(() -> mHost.openConversation(id)); }
            @Override public void onError(@NonNull String message)
            {
              runOnUi(() -> Toast.makeText(mFragment.requireContext(), message, Toast.LENGTH_SHORT).show());
            }
          });
        })
        .show();
  }

  private void openDirect(@NonNull String userId)
  {
    mRepository.openDirect(userId, new ChatRepository.IdCallback()
    {
      @Override public void onReady(@NonNull String id) { runOnUi(() -> mHost.openConversation(id)); }
      @Override public void onError(@NonNull String message)
      {
        runOnUi(() -> Toast.makeText(mFragment.requireContext(), message, Toast.LENGTH_SHORT).show());
      }
    });
  }

  private void setStatus(@NonNull String status)
  {
    final TextView view = mRoot.findViewById(R.id.chats_status);
    if (status.isEmpty())
    {
      if (mServices.connectionState() == ChatConnectionState.offline && mRepository.isSignedIn())
        status = mFragment.getString(R.string.chats_offline);
      else if (mServices.connectionState() == ChatConnectionState.connecting
               || mServices.connectionState() == ChatConnectionState.reconnecting)
        status = mFragment.getString(R.string.chats_connecting);
    }
    view.setText(status);
    view.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void runOnUi(@NonNull Runnable action)
  {
    if (!mFragment.isAdded())
      return;
    mFragment.requireActivity().runOnUiThread(action);
  }

  @Override public void onInboxHint() { refresh(); }
  @Override public void onConversationHint(@NonNull String conversationId) {}
  @Override public void onTyping(@NonNull String conversationId, @NonNull String userId, boolean isTyping) {}
  @Override public void onConnectionState(@NonNull ChatConnectionState state) { runOnUi(() -> setStatus("")); }

  private static final class SimpleWatcher implements android.text.TextWatcher
  {
    interface Listener { void onQuery(@NonNull String query); }
    private final Listener mListener;
    SimpleWatcher(@NonNull Listener listener) { mListener = listener; }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count)
    {
      if (s.length() >= 2)
        mListener.onQuery(s.toString());
    }
    @Override public void afterTextChanged(android.text.Editable s) {}
  }
}
