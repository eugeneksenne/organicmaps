package app.organicmaps.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.widget.HorizontalScrollView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView;
import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.discover.data.CityExploreRepository;
import app.organicmaps.discover.data.DiscoverHeroRepository;
import app.organicmaps.discover.data.FlashDropsRepository;
import app.organicmaps.discover.data.OpenMeteoWeatherRepository;
import app.organicmaps.discover.ui.UniversalVenuePreview;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The non-map destinations in the app's primary navigation. */
public class MainScreenFragment extends Fragment
{
  public static final String ARG_DESTINATION = "destination";
  private static final int CAMERA_PERMISSION_REQUEST = 104;

  @Nullable
  private CameraPreviewEngine mCameraPreviewEngine;

  @NonNull
  public static MainScreenFragment newInstance(@NonNull String destination)
  {
    final MainScreenFragment fragment = new MainScreenFragment();
    final Bundle arguments = new Bundle();
    arguments.putString(ARG_DESTINATION, destination);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    final String destination = requireArguments().getString(ARG_DESTINATION);
    if ("camera".equals(destination))
      return createCameraScreen(inflater, container);
    if ("discover".equals(destination))
      return createDiscoverScreen(inflater, container);
    if ("feed".equals(destination))
      return createFeedScreen(inflater, container);
    if ("chats".equals(destination))
      return createChatsScreen(inflater, container);
    if ("calls".equals(destination))
      return createCallsScreen(inflater, container);
    if ("groups".equals(destination))
      return createGroupsScreen(inflater, container);
    if ("stories".equals(destination))
      return createStoriesScreen(inflater, container);
    if (destination.startsWith("story:"))
      return createStoryViewer(inflater, container, destination.substring("story:".length()));
    if (destination.startsWith("call:"))
      return createCallScreen(inflater, container, destination.substring("call:".length()));
    if (destination.startsWith("profile:"))
      return createProfileScreen(inflater, container, destination.substring("profile:".length()));
    if ("nightguard".equals(destination))
      return createNightGuardScreen(inflater, container);
    if ("buddy_pair".equals(destination))
      return createBuddyPairScreen(inflater, container);
    if ("safety_check".equals(destination))
      return createSafetyCheckScreen(inflater, container);
    if ("walk_home".equals(destination))
      return createWalkHomeScreen(inflater, container);
    if (destination.startsWith("discover_all:"))
    {
      final String title = destination.substring("discover_all:".length());
      if ("Events".equals(title))
        return createEventsScreen(inflater, container);
      if ("Explore the City".equals(title))
        return createExploreCityScreen(inflater, container);
      if ("Flash Drops".equals(title))
        return createFlashDropsScreen(inflater, container);
      if ("My Circle".equals(title))
        return createMyCircleScreen(inflater, container);
      if ("Prep Rooms".equals(title))
        return createPrepRoomsScreen(inflater, container);
      if ("Smart Places".equals(title))
        return createSmartPlacesScreen(inflater, container);
      if ("Tonight".equals(title))
        return createTonightScreen(inflater, container);
      return createDiscoverAllScreen(inflater, container, title);
    }
    if (destination.startsWith("conversation:"))
      return createConversationScreen(inflater, container, destination.substring("conversation:".length()));

    final View view = inflater.inflate(R.layout.main_screen, container, false);
    final TextView title = view.findViewById(R.id.main_screen_title);
    final TextView headline = view.findViewById(R.id.main_screen_headline);
    final TextView body = view.findViewById(R.id.main_screen_body);
    final View action = view.findViewById(R.id.main_screen_action);

    if ("feed".equals(destination))
    {
      title.setText(R.string.main_tab_feed);
      headline.setText(R.string.feed_headline);
      body.setText(R.string.feed_body);
    }
    else
    {
      title.setText(R.string.main_tab_chats);
      headline.setText(R.string.chats_headline);
      body.setText(R.string.chats_body);
    }
    return view;
  }

  @NonNull
  private View createChatsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.chats_screen, container, false);
    view.findViewById(R.id.chats_search).setOnClickListener(v -> Toast.makeText(requireContext(), "Search messages, people, venues, and files", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.chats_new).setOnClickListener(v -> Toast.makeText(requireContext(), "Start a new conversation", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.chats_story_add).setOnClickListener(v -> openStories());
    view.findViewById(R.id.chats_story_alfred).setOnClickListener(v -> openStory("Alfred M."));
    view.findViewById(R.id.chats_story_nomsa).setOnClickListener(v -> openStory("Nomsa"));
    view.findViewById(R.id.chats_story_vault).setOnClickListener(v -> openStory("The Vault"));
    final LinearLayout categories = view.findViewById(R.id.chats_categories);
    for (int i = 0; i < categories.getChildCount(); ++i)
    {
      final TextView category = (TextView) categories.getChildAt(i);
      category.setOnClickListener(v -> {
        if ("Calls".contentEquals(((TextView) v).getText()))
          openCalls();
        else if ("Groups".contentEquals(((TextView) v).getText()))
          openGroups();
        else if ("Stories".contentEquals(((TextView) v).getText()))
          openStories();
        else
          selectChatCategory(categories, (TextView) v);
      });
    }
    view.findViewById(R.id.chat_row_nightguard).setOnClickListener(v -> openConversation("NightGuard"));
    view.findViewById(R.id.chat_row_alfred).setOnClickListener(v -> openConversation("Alfred M."));
    view.findViewById(R.id.chat_row_truth).setOnClickListener(v -> openConversation("Truth Nightclub"));
    view.findViewById(R.id.chat_row_group).setOnClickListener(v -> openConversation("Joburg Fridays"));
    view.findViewById(R.id.chat_row_lerato).setOnClickListener(v -> openConversation("Lerato"));
    return view;
  }

  private void openStories()
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("stories")).addToBackStack("stories").commit();
  }

  @NonNull
  private View createStoriesScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.stories_screen, container, false);
    view.findViewById(R.id.stories_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.stories_camera).setOnClickListener(v -> showConversationNotice("Open FOMO Camera to add a story"));
    view.findViewById(R.id.story_add).setOnClickListener(v -> showConversationNotice("Open FOMO Camera to add a story"));
    view.findViewById(R.id.story_alfred).setOnClickListener(v -> openStory("Alfred M."));
    view.findViewById(R.id.story_nomsa).setOnClickListener(v -> openStory("Nomsa"));
    view.findViewById(R.id.story_vault).setOnClickListener(v -> openStory("The Vault"));
    view.findViewById(R.id.story_lerato).setOnClickListener(v -> openStory("Lerato"));
    return view;
  }

  private void openStory(@NonNull String name)
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("story:" + name)).addToBackStack("story").commit();
  }

  @NonNull
  private View createStoryViewer(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull String name)
  {
    final View view = inflater.inflate(R.layout.story_viewer, container, false);
    ((TextView) view.findViewById(R.id.story_viewer_name)).setText(name);
    view.findViewById(R.id.story_viewer_close).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.story_react).setOnClickListener(v -> ((TextView) v).setText("♥"));
    return view;
  }

  private void openGroups()
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("groups")).addToBackStack("groups").commit();
  }

  @NonNull
  private View createGroupsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.groups_screen, container, false);
    view.findViewById(R.id.groups_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.groups_search).setOnClickListener(v -> showConversationNotice("Search groups, members, plans, and venues"));
    view.findViewById(R.id.groups_create).setOnClickListener(v -> showConversationNotice("Create group: choose members, details, then permissions"));
    view.findViewById(R.id.group_joburg).setOnClickListener(v -> openConversation("Joburg Fridays"));
    view.findViewById(R.id.group_birthday).setOnClickListener(v -> openConversation("Bongi’s Birthday"));
    view.findViewById(R.id.group_roadtrip).setOnClickListener(v -> openConversation("Durban Road Trip"));
    view.findViewById(R.id.group_vault).setOnClickListener(v -> openConversation("The Vault Crew"));
    return view;
  }

  private void openCalls()
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("calls")).addToBackStack("calls").commit();
  }

  @NonNull
  private View createCallsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.calls_screen, container, false);
    view.findViewById(R.id.calls_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.calls_search).setOnClickListener(v -> showConversationNotice("Search call history"));
    view.findViewById(R.id.calls_start).setOnClickListener(v -> showConversationNotice("Start a voice or video call"));
    final LinearLayout filters = view.findViewById(R.id.call_filters);
    for (int i = 0; i < filters.getChildCount(); ++i)
    {
      final TextView filter = (TextView) filters.getChildAt(i);
      filter.setOnClickListener(v -> selectChatCategory(filters, (TextView) v));
    }
    view.findViewById(R.id.call_alfred).setOnClickListener(v -> openCall("outgoing_video", "Alfred M."));
    view.findViewById(R.id.call_nomsa).setOnClickListener(v -> openCall("voice", "Nomsa"));
    view.findViewById(R.id.call_lerato).setOnClickListener(v -> openCall("incoming_video", "Lerato"));
    view.findViewById(R.id.call_group).setOnClickListener(v -> openCall("group_voice", "Joburg Fridays"));
    return view;
  }

  private void selectChatCategory(@NonNull LinearLayout categories, @NonNull TextView selected)
  {
    for (int i = 0; i < categories.getChildCount(); ++i)
    {
      final TextView category = (TextView) categories.getChildAt(i);
      final boolean active = category == selected;
      category.setBackgroundResource(active ? R.drawable.chat_category_active : R.drawable.camera_look);
      category.setTextColor(active ? Color.WHITE : 0xDE000000);
    }
  }

  private void openConversation(@NonNull String name)
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("conversation:" + name)).addToBackStack("conversation").commit();
  }

  @NonNull
  private View createConversationScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull String name)
  {
    final View view = inflater.inflate(R.layout.chat_conversation, container, false);
    ((TextView) view.findViewById(R.id.conversation_name)).setText(name);
    view.findViewById(R.id.conversation_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.conversation_voice).setOnClickListener(v -> openCall("outgoing_voice", name));
    view.findViewById(R.id.conversation_video).setOnClickListener(v -> openCall("outgoing_video", name));
    view.findViewById(R.id.conversation_attach).setOnClickListener(v -> showConversationNotice("Camera • Gallery • Document • Venue • Event • Location"));
    view.findViewById(R.id.conversation_cancel_reply).setOnClickListener(v -> view.findViewById(R.id.conversation_reply).setVisibility(View.GONE));
    view.findViewById(R.id.conversation_menu).setOnClickListener(v -> showConversationMenu(v));
    final EditText input = view.findViewById(R.id.conversation_input);
    final TextView sent = view.findViewById(R.id.conversation_sent);
    final TextView send = view.findViewById(R.id.conversation_send);
    input.addTextChangedListener(new TextWatcher()
    {
      @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence text, int start, int before, int count)
      {
        send.setText(text.length() == 0 ? "🎤" : "↑");
      }
      @Override public void afterTextChanged(Editable text) {}
    });
    send.setOnClickListener(v -> {
      if (input.getText().length() > 0)
      {
        sent.setText(input.getText());
        input.setText("");
      }
      else
        showConversationNotice("Hold to record a voice note");
    });
    return view;
  }

  private void openProfile(@NonNull String username)
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container,
        newInstance("profile:" + username)).addToBackStack("profile").commit();
  }

  @NonNull
  private View createProfileScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull String username)
  {
    final View view = inflater.inflate(R.layout.profile_screen, container, false);
    final String displayName = "alfredm".equals(username) ? "Alfred M.  ✓" : username;
    ((TextView) view.findViewById(R.id.profile_username_top)).setText("@" + username);
    ((TextView) view.findViewById(R.id.profile_name)).setText(displayName);
    ((TextView) view.findViewById(R.id.profile_avatar)).setText(username.substring(0, 1).toUpperCase());
    view.findViewById(R.id.profile_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.profile_follow).setOnClickListener(v -> { ((TextView) v).setText("Following"); v.setClickable(false); });
    view.findViewById(R.id.profile_message).setOnClickListener(v -> openConversation(displayName));
    view.findViewById(R.id.profile_more).setOnClickListener(v -> showConversationNotice("Share, block, or report profile"));
    return view;
  }

  private void openCall(@NonNull String mode, @NonNull String name)
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container,
        newInstance("call:" + mode + ":" + name)).addToBackStack("call").commit();
  }

  @NonNull
  private View createCallScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull String payload)
  {
    final String[] parts = payload.split(":", 2);
    final String mode = parts[0];
    final String name = parts.length > 1 ? parts[1] : "FOMO Call";
    final View view = inflater.inflate(R.layout.call_screen, container, false);
    ((TextView) view.findViewById(R.id.call_screen_name)).setText(name);
    ((TextView) view.findViewById(R.id.call_screen_avatar)).setText(name.substring(0, 1).toUpperCase());
    final boolean incoming = mode.startsWith("incoming");
    final boolean video = mode.contains("video");
    final boolean group = mode.startsWith("group");
    ((TextView) view.findViewById(R.id.call_screen_status)).setText(incoming ? "Incoming " + (video ? "video" : "voice") + " call" : mode.startsWith("outgoing") ? "Calling…" : "00:12");
    view.findViewById(R.id.call_screen_answer).setVisibility(incoming ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.call_screen_local_preview).setVisibility(video ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.call_screen_camera).setVisibility(video ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.call_screen_participant_button).setVisibility(group ? View.VISIBLE : View.GONE);
    if (group)
    {
      final TextView participants = view.findViewById(R.id.call_screen_participants);
      participants.setVisibility(View.VISIBLE);
      participants.setText("8 participants • 3 speaking");
    }
    view.findViewById(R.id.call_screen_answer).setOnClickListener(v -> {
      ((TextView) view.findViewById(R.id.call_screen_status)).setText("00:00");
      v.setVisibility(View.GONE);
    });
    view.findViewById(R.id.call_screen_decline).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.call_screen_mute).setOnClickListener(v -> ((TextView) v).setText("♩\nMuted"));
    view.findViewById(R.id.call_screen_speaker).setOnClickListener(v -> ((TextView) v).setText("◖\nSpeaker on"));
    view.findViewById(R.id.call_screen_camera).setOnClickListener(v -> ((TextView) v).setText("▣\nCamera off"));
    view.findViewById(R.id.call_screen_participant_button).setOnClickListener(v -> showConversationNotice("Participants and moderator controls"));
    return view;
  }

  private void showConversationNotice(@NonNull String message)
  {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
  }

  private void showConversationMenu(@NonNull View anchor)
  {
    final PopupMenu menu = new PopupMenu(requireContext(), anchor);
    for (String item : new String[] {"View Profile", "Search Conversation", "Shared Media", "Shared Files",
                                     "Shared Venues", "Mute Notifications", "Block User", "Report User"})
      menu.getMenu().add(item);
    menu.setOnMenuItemClickListener(item -> { showConversationNotice(item.getTitle().toString()); return true; });
    menu.show();
  }

  @NonNull
  private View createFeedScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.feed_screen, container, false);
    final TextView context = view.findViewById(R.id.feed_context);
    final TextView[] tabs = {view.findViewById(R.id.feed_for_you), view.findViewById(R.id.feed_following),
                             view.findViewById(R.id.feed_nearby), view.findViewById(R.id.feed_live)};
    final String[] details = {"2h ago • Sandton", "42m ago • Following", "5m ago • 280m away", "● LIVE • 2.4K watching"};
    for (int i = 0; i < tabs.length; ++i)
    {
      final int index = i;
      tabs[i].setOnClickListener(v -> selectFeedTab(tabs, index, context, details[index]));
    }
    view.findViewById(R.id.feed_search).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Search creators, venues, events, sounds, and hashtags", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.feed_profile).setOnClickListener(v -> openProfile("alfredm"));
    view.findViewById(R.id.feed_follow).setOnClickListener(v -> {
      ((TextView) v).setText("✓\nFollowing");
      v.setClickable(false);
    });
    view.findViewById(R.id.feed_like).setOnClickListener(v -> ((TextView) v).setText("♥\n2.8K"));
    view.findViewById(R.id.feed_ripple).setOnClickListener(v -> ((TextView) v).setText("≋\nRippled"));
    view.findViewById(R.id.feed_save).setOnClickListener(v -> ((TextView) v).setText("⌑\nSaved"));
    view.findViewById(R.id.feed_comment).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Comments", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.feed_share).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Share this Moment", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.feed_lobby).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Opening Cocoon Nightclub lobby", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.feed_route).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Opening route to Cocoon Nightclub", Toast.LENGTH_SHORT).show());
    return view;
  }

  private void selectFeedTab(@NonNull TextView[] tabs, int selected, @NonNull TextView context, @NonNull String details)
  {
    for (int i = 0; i < tabs.length; ++i)
    {
      final boolean active = i == selected;
      tabs[i].setSelected(active);
      tabs[i].setBackgroundResource(active ? R.drawable.feed_glass_pill : 0);
      tabs[i].setTextColor(active ? Color.WHITE : 0xCCFFFFFF);
      tabs[i].setTypeface(null, active ? 1 : 0);
    }
    context.setText(details);
  }

  @NonNull
  private View createDiscoverScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.discover_screen, container, false);
    bindDiscoverHero(view);
    refreshDiscoverWeather(view);
    view.findViewById(R.id.discover_hero_action).setOnClickListener(v -> openDiscoverAll("Tonight"));
    final LinearLayout sections = view.findViewById(R.id.discover_sections);
    addDiscoverSection(sections, "Closing Soon", new String[] {"The Living Room • 48 min", "The Royale • 1h 12m", "Rosebank Social • 1h 35m"}, 0xFFF9E2D1);
    addDiscoverSection(sections, "Flash Drops", new String[] {"Free welcome drink • 12 left", "R100 ride credit • 8 left", "Guest-list upgrade • 5 left"}, 0xFFF8D8E8);
    addDiscoverSection(sections, "My Circle", new String[] {"Nomsa • 3 mutual friends", "Kagiso • Same events", "Lerato • 1.2 km away"}, 0xFFDCE8FA);
    addDiscoverSection(sections, "Live Moments", new String[] {"Amapiano at Truth", "Rooftop sunset", "Friends at Braam"}, 0xFFE6D9F9);
    addDiscoverSection(sections, "Smart Places", new String[] {"The Living Room • 0.8 km", "Artivist • 1.1 km", "Marabi Club • 1.6 km"}, 0xFFD5EDE3);
    addDiscoverSection(sections, "Trending Now", new String[] {"Braamfontein after dark", "Amapiano Fridays", "Rosebank rooftops"}, 0xFFFFE8B9);
    addDiscoverSection(sections, "Events", new String[] {"Amapiano Fridays • Tonight", "Jazz on the Square • 20:00", "Night Market • Tomorrow"}, 0xFFE3E3E3);
    addExploreCitySection(sections);
    addDiscoverSection(sections, "Channels", new String[] {"Music", "Campus", "Food", "Photography"}, 0xFFD8E9F1);
    addDiscoverSection(sections, "Prep Rooms", new String[] {"Tonight's outfits", "Fresh cut near you", "Beauty deals"}, 0xFFF5DCE4);
    addDiscoverSection(sections, "Tonight", new String[] {"Build your timeline", "Dinner at 19:30", "Ride home ready"}, 0xFFDDE5D4);
    return view;
  }

  private void refreshDiscoverWeather(@NonNull View view)
  {
    new OpenMeteoWeatherRepository().loadJohannesburg(new OpenMeteoWeatherRepository.Callback()
    {
      @Override public void onLoaded(@NonNull String summary)
      {
        if (!isAdded())
          return;
        requireActivity().runOnUiThread(() -> {
          ((TextView) view.findViewById(R.id.discover_weather)).setText(summary);
          // Refresh while Discover remains open. The Supabase hero remains the offline fallback.
          view.postDelayed(() -> { if (isAdded()) refreshDiscoverWeather(view); }, 10 * 60 * 1000L);
        });
      }
      @Override public void onUnavailable() {}
    });
  }

  private void bindDiscoverHero(@NonNull View view)
  {
    new DiscoverHeroRepository().load("johannesburg", new DiscoverHeroRepository.Callback()
    {
      @Override public void onLoaded(@NonNull DiscoverHeroRepository.Hero hero)
      {
        if (!isAdded())
          return;
        requireActivity().runOnUiThread(() -> {
          ((TextView) view.findViewById(R.id.discover_city)).setText(hero.city);
          ((TextView) view.findViewById(R.id.discover_weather)).setText(hero.weather);
          ((TextView) view.findViewById(R.id.discover_headline)).setText(hero.headline);
          ((TextView) view.findViewById(R.id.discover_recommendation)).setText(hero.recommendation);
          ((TextView) view.findViewById(R.id.discover_energy)).setText(hero.energy + "% ENERGY");
          ((TextView) view.findViewById(R.id.discover_live_count)).setText(hero.liveVenues + " LIVE");
          ((TextView) view.findViewById(R.id.discover_drop_count)).setText(hero.flashDrops + " DROPS");
        });
      }
      @Override public void onUnavailable() {}
    });
  }

  private void addDiscoverSection(@NonNull LinearLayout parent, @NonNull String title, @NonNull String[] cards, int accent)
  {
    final LinearLayout section = new LinearLayout(requireContext());
    section.setOrientation(LinearLayout.VERTICAL);
    section.setPadding(dp(16), dp(24), 0, 0);
    final LinearLayout header = new LinearLayout(requireContext());
    header.setGravity(android.view.Gravity.CENTER_VERTICAL);
    final TextView label = new TextView(requireContext());
    label.setText(title); label.setTextColor(Color.rgb(25, 21, 29)); label.setTextSize(20); label.setTypeface(null, 1);
    header.addView(label, new LinearLayout.LayoutParams(0, dp(34), 1));
    final TextView all = new TextView(requireContext());
    all.setText("See All  →"); all.setTextColor(Color.rgb(43, 111, 82)); all.setTextSize(13); all.setGravity(android.view.Gravity.CENTER_VERTICAL);
    all.setOnClickListener(v -> openDiscoverAll(title));
    header.addView(all, new LinearLayout.LayoutParams(dp(88), dp(34)));
    section.addView(header);
    final HorizontalScrollView scroll = new HorizontalScrollView(requireContext());
    scroll.setHorizontalScrollBarEnabled(false);
    final LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, dp(8), dp(16), 0);
    for (String card : cards) row.addView(discoverCard(card, accent));
    scroll.addView(row);
    section.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(126)));
    parent.addView(section);
  }

  private void addExploreCitySection(@NonNull LinearLayout parent)
  {
    final LinearLayout section = new LinearLayout(requireContext()); section.setOrientation(LinearLayout.VERTICAL); section.setPadding(dp(16), dp(24), 0, 0);
    final TextView heading = new TextView(requireContext()); heading.setText("Explore the City                                      See All  →"); heading.setTextSize(20); heading.setTextColor(Color.rgb(25, 21, 29)); heading.setTypeface(null, 1); heading.setOnClickListener(v -> openDiscoverAll("Explore the City")); section.addView(heading, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)));
    final HorizontalScrollView chips = new HorizontalScrollView(requireContext()); chips.setHorizontalScrollBarEnabled(false); final LinearLayout chipRow = new LinearLayout(requireContext()); chipRow.setOrientation(LinearLayout.HORIZONTAL);
    for (String category : new String[] {"All", "Nightlife", "Food", "Prep", "Travel", "24/7"}) { TextView chip = new TextView(requireContext()); chip.setText(category); chip.setGravity(android.view.Gravity.CENTER); chip.setTextSize(13); chip.setTextColor(Color.rgb(25, 21, 29)); chip.setBackground(roundBackground(category.equals("All") ? 0xFFE6A05B : 0xFFFFFFFF, 18)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(78), dp(34)); params.setMargins(0, 0, dp(8), 0); chipRow.addView(chip, params); }
    chips.addView(chipRow); section.addView(chips);
    final HorizontalScrollView venues = new HorizontalScrollView(requireContext()); venues.setHorizontalScrollBarEnabled(false); final LinearLayout venueRow = new LinearLayout(requireContext()); venueRow.setOrientation(LinearLayout.HORIZONTAL); venueRow.setPadding(0, dp(10), dp(16), 0);
    for (String venue : new String[] {"The Living Room • 0.8 km", "Artivist • 1.1 km", "Marabi Club • 1.6 km"}) venueRow.addView(discoverCard(venue, 0xFFFFE5C7)); venues.addView(venueRow); section.addView(venues, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(134))); parent.addView(section);
  }

  @NonNull
  private View discoverCard(@NonNull String text, int accent)
  {
    final TextView card = new TextView(requireContext()); card.setText(text); card.setTextColor(Color.rgb(25, 21, 29)); card.setTextSize(15); card.setTypeface(null, 1); card.setGravity(android.view.Gravity.BOTTOM); card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(roundBackground(accent, 18));
    final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(180), dp(112)); params.setMargins(0, 0, dp(10), 0); card.setLayoutParams(params); return card;
  }

  @NonNull
  private GradientDrawable roundBackground(int color, int radius)
  {
    final GradientDrawable background = new GradientDrawable(); background.setColor(color); background.setCornerRadius(dp(radius)); return background;
  }

  private int dp(int value) { return Math.round(value * requireContext().getResources().getDisplayMetrics().density); }

  private void openDiscoverAll(@NonNull String title)
  {
    getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("discover_all:" + title)).addToBackStack("discover_all").commit();
  }

  @NonNull
  private View createWalkHomeScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.walk_home_screen, container, false);
    view.findViewById(R.id.walk_back).setOnClickListener(v -> requireActivity().onBackPressed());
    final LinearLayout active = view.findViewById(R.id.walk_active); eventLine(active, "Walking Home  →  Home", 24, Color.WHITE, 1); eventLine(active, "● Protected · ETA 12 min · 1.1 km remaining", 14, 0xFF8EF6B7, 1); eventLine(active, "Sarah and James can see your temporary progress.\nSharing ends immediately when you arrive.", 13, 0xDFFFFFFF, 0); TextView actions = eventText("Pause     Call     Emergency     End", 13, 0xFF74AEFF, 1); actions.setPadding(0, dp(14), 0, 0); active.addView(actions);
    final LinearLayout recent = view.findViewById(R.id.walk_recent); eventLine(recent, "RECENT JOURNEYS", 12, 0xAFFFFFFF, 1); eventLine(recent, "✓ Home · 1.8 km · 18 min · arrived safely", 14, 0xFF8EF6B7, 0); eventLine(recent, "No route is retained unless you explicitly save a summary.", 12, 0xBFFFFFFF, 0);
    view.findViewById(R.id.walk_start).setOnClickListener(v -> new android.app.AlertDialog.Builder(requireContext()).setTitle("Where are you going?").setItems(new String[] {"Home", "Hotel", "Parking", "My car", "Saved place", "Custom destination"}, (dialog, which) -> new android.app.AlertDialog.Builder(requireContext()).setTitle("How are you travelling?").setItems(new String[] {"Walking", "Ride-share", "Driving", "Cycling", "Public transport"}, (d, mode) -> Toast.makeText(requireContext(), "Review journey before sharing starts", Toast.LENGTH_SHORT).show()).show()).show());
    return view;
  }

  @NonNull
  private View createSafetyCheckScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.safety_check_screen, container, false);
    view.findViewById(R.id.check_back).setOnClickListener(v -> requireActivity().onBackPressed());
    final LinearLayout scheduled = view.findViewById(R.id.check_scheduled); eventLine(scheduled, "SCHEDULED CHECK", 12, 0xAFFFFFFF, 1); eventLine(scheduled, "21:30 Tonight", 22, Color.WHITE, 1); eventLine(scheduled, "Sarah · James will be notified only if you miss it.", 13, 0xDFFFFFFF, 0);
    final LinearLayout recent = view.findViewById(R.id.check_recent); eventLine(recent, "RECENT CHECKS", 12, 0xAFFFFFFF, 1); eventLine(recent, "✓ Walk home · Safe confirmed · deleted automatically", 14, 0xFF8EF6B7, 0); eventLine(recent, "✓ First date · Silent wellness completion", 14, 0xFF8EF6B7, 0);
    view.findViewById(R.id.check_start).setOnClickListener(v -> new android.app.AlertDialog.Builder(requireContext()).setTitle("When should we check on you?").setItems(new String[] {"15 min", "30 min", "1 hour", "2 hours", "After event ends", "After leaving venue", "On arrival"}, (dialog, which) -> new android.app.AlertDialog.Builder(requireContext()).setTitle("Share with").setMessage("Trusted Circle · Recent Buddies\n\nLocation stays private unless you choose emergency sharing.").setNegativeButton("Cancel", null).setPositiveButton("Start", (d, w) -> Toast.makeText(requireContext(), "Safety Check scheduled", Toast.LENGTH_SHORT).show()).show()).show());
    return view;
  }

  @NonNull
  private View createBuddyPairScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.buddy_pair_screen, container, false);
    view.findViewById(R.id.buddy_back).setOnClickListener(v -> requireActivity().onBackPressed());
    final LinearLayout active = view.findViewById(R.id.buddy_active); eventLine(active, "🤝  Festival Crew", 24, Color.WHITE, 1); eventLine(active, "● Active  ·  2h 14m remaining", 14, 0xFF8EF6B7, 1); eventLine(active, "Sarah · 18 m away     James · 120 m away\nPeter · 450 m away", 13, 0xEFFFFFFF, 0); TextView openMap = eventText("Open Live Map  →", 14, 0xFF74AEFF, 1); openMap.setPadding(0, dp(12), 0, 0); active.addView(openMap); openMap.setOnClickListener(v -> Toast.makeText(requireContext(), "Live map opens with temporary buddy presence", Toast.LENGTH_SHORT).show());
    final LinearLayout actions = view.findViewById(R.id.buddy_actions); addBuddyAction(actions, "＋ Pair with Friend", "Invite expires in 5 minutes"); addBuddyAction(actions, "👥 Start Group Pair", "2–20 people · Festival Crew, Birthday Group"); addBuddyAction(actions, "📍 Meet Here", "Temporary rendezvous · expires in 30 min"); addBuddyAction(actions, "⌖ Find Friend", "Navigate to a buddy without exposing a permanent trail");
    final LinearLayout privacy = view.findViewById(R.id.buddy_privacy); eventLine(privacy, "PRIVACY CONTROLS", 12, 0xAFFFFFFF, 1); eventLine(privacy, "Live location · Battery · ETA · Arrival", 15, Color.WHITE, 1); TextView controls = eventText("Manage what this session shares  →", 13, 0xFF74AEFF, 1); controls.setPadding(0, dp(7), 0, 0); privacy.addView(controls); controls.setOnClickListener(v -> Toast.makeText(requireContext(), "Changes apply immediately and automatically expire", Toast.LENGTH_LONG).show());
    return view;
  }

  private void addBuddyAction(@NonNull LinearLayout parent, @NonNull String title, @NonNull String subtitle)
  {
    final LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(15), dp(12), dp(15), dp(12)); row.setBackground(roundBackground(0xFF191620, 17)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)); params.setMargins(0, 0, 0, dp(9)); parent.addView(row, params); eventLine(row, title, 16, Color.WHITE, 1); eventLine(row, subtitle, 12, 0xBFFFFFFF, 0); row.setOnClickListener(v -> Toast.makeText(requireContext(), title, Toast.LENGTH_SHORT).show());
  }

  @NonNull
  private View createNightGuardScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.nightguard_screen, container, false);
    view.findViewById(R.id.ng_back).setOnClickListener(v -> requireActivity().onBackPressed());
    final LinearLayout status = view.findViewById(R.id.ng_status); eventLine(status, "◈  NightGuard  ✓", 25, Color.WHITE, 1); eventLine(status, "● Protected · monitoring until 4:00 AM", 14, 0xFF8EF6B7, 1); eventLine(status, "Temporary sharing is active only for your chosen sessions.", 12, 0xDFFFFFFF, 0);
    final LinearLayout actions = view.findViewById(R.id.ng_actions); addNightGuardAction(actions, "🤝  Buddy Pair", "Pair temporarily with someone you trust"); addNightGuardAction(actions, "🚶  Walk Me Home", "Share progress until you arrive"); addNightGuardAction(actions, "◈  Safety Check", "Schedule a private check-in"); addNightGuardAction(actions, "🚗  Ride Companion", "Track a ride with your trusted circle");
    final LinearLayout circle = view.findViewById(R.id.ng_circle); eventLine(circle, "TRUSTED CIRCLE  ·  6 MEMBERS", 12, 0xAFFFFFFF, 1); eventLine(circle, "Family · Close Friends · Festival Crew", 16, Color.WHITE, 1); TextView manage = eventText("Manage permissions and expiry  →", 13, 0xFF74AEFF, 1); manage.setPadding(0, dp(8), 0, 0); circle.addView(manage); manage.setOnClickListener(v -> Toast.makeText(requireContext(), "Privacy controls: sharing is temporary and revocable", Toast.LENGTH_LONG).show());
    final View sos = view.findViewById(R.id.ng_sos); sos.setOnLongClickListener(v -> { Toast.makeText(requireContext(), "SOS confirmation required. Contact local emergency services if you are in immediate danger.", Toast.LENGTH_LONG).show(); return true; }); sos.setOnClickListener(v -> Toast.makeText(requireContext(), "Hold SOS to start an emergency session", Toast.LENGTH_SHORT).show());
    return view;
  }

  private void addNightGuardAction(@NonNull LinearLayout parent, @NonNull String title, @NonNull String subtitle)
  {
    final LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(15), dp(12), dp(15), dp(12)); row.setBackground(roundBackground(0xFF191620, 17)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)); params.setMargins(0, 0, 0, dp(9)); parent.addView(row, params); eventLine(row, title, 16, Color.WHITE, 1); eventLine(row, subtitle, 12, 0xBFFFFFFF, 0); row.setOnClickListener(v -> { if (title.contains("Buddy Pair")) getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("buddy_pair")).addToBackStack("buddy_pair").commit(); else if (title.contains("Safety Check")) getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("safety_check")).addToBackStack("safety_check").commit(); else if (title.contains("Walk Me Home")) getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("walk_home")).addToBackStack("walk_home").commit(); else Toast.makeText(requireContext(), title + " session setup", Toast.LENGTH_SHORT).show(); });
  }

  @NonNull
  private View createTonightScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.tonight_screen, container, false);
    view.findViewById(R.id.tonight_notifications).setOnClickListener(v -> Toast.makeText(requireContext(), "Tonight notifications", Toast.LENGTH_SHORT).show());
    final LinearLayout overview = view.findViewById(R.id.tonight_overview); eventLine(overview, "TONIGHT  ✓  PROTECTED", 12, 0xFF66EBA5, 1); eventLine(overview, "2 Active Plans  ·  8 Friends", 23, Color.WHITE, 1); eventLine(overview, "Sandton  ·  Next stop Saint at 8:30 PM", 14, 0xDFFFFFFF, 0); TextView continuePlan = eventText("Continue tonight  →", 14, 0xFF6DAAFF, 1); continuePlan.setPadding(0, dp(15), 0, 0); overview.addView(continuePlan); continuePlan.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening tonight timeline", Toast.LENGTH_SHORT).show());
    final LinearLayout guard = view.findViewById(R.id.tonight_guard); eventLine(guard, "◈  NightGuard  ✓", 24, Color.WHITE, 1); eventLine(guard, "You’re protected tonight.", 14, 0xDFFFFFFF, 0); TextView monitoring = eventText("●  Monitoring until 4:00 AM", 14, 0xFF8EF6B7, 1); monitoring.setPadding(0, dp(9), 0, dp(9)); guard.addView(monitoring); eventLine(guard, "◉ Trusted Contacts     ! Emergency SOS     ⌖ Live Location\n◇ Safe Arrival          ⌁ Route Awareness     ♧ Group Alerts", 12, 0xEFFFFFFF, 0); TextView manageGuard = eventText("Manage NightGuard  →", 14, 0xFF6DAAFF, 1); manageGuard.setGravity(android.view.Gravity.CENTER); manageGuard.setBackgroundResource(R.drawable.event_chip_background); LinearLayout.LayoutParams guardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(39)); guardParams.setMargins(0, dp(10), 0, 0); guard.addView(manageGuard, guardParams); manageGuard.setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("nightguard")).addToBackStack("nightguard").commit());
    final LinearLayout plans = view.findViewById(R.id.tonight_plans); final LinearLayout planHeader = new LinearLayout(requireContext()); planHeader.setGravity(android.view.Gravity.CENTER_VERTICAL); plans.addView(planHeader); TextView planTitle = eventText("✦  My Plans", 22, Color.WHITE, 1); planHeader.addView(planTitle, new LinearLayout.LayoutParams(0, dp(43), 1)); TextView newPlan = eventText("＋ New Plan", 13, 0xFFFFABF3, 1); newPlan.setGravity(android.view.Gravity.CENTER); newPlan.setBackground(roundBackground(0xFF572158, 14)); planHeader.addView(newPlan, new LinearLayout.LayoutParams(dp(105), dp(39))); newPlan.setOnClickListener(v -> showPlanCreator(v)); addTonightPlan(plans, "Birthday Night", "7:30 PM  Marble  →  10:00 PM  Saint  →  12:30 AM  LIV", "5/8 arrived", 0xFFB958EC); addTonightPlan(plans, "Girls Night Out", "8:00 PM  Tasha’s Sandton  →  11:30 PM  Taboo", "4/4 arrived", 0xFFF05BA9);
    final LinearLayout create = view.findViewById(R.id.tonight_create); TextView plus = eventText("＋", 42, 0xFFFF81C7, 0); plus.setGravity(android.view.Gravity.CENTER); plus.setBackground(roundBackground(0xFF51213E, 22)); create.addView(plus, new LinearLayout.LayoutParams(dp(70), dp(70))); LinearLayout createDetails = new LinearLayout(requireContext()); createDetails.setOrientation(LinearLayout.VERTICAL); createDetails.setPadding(dp(15), 0, 0, 0); create.addView(createDetails, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); eventLine(createDetails, "Create Tonight’s Plan", 18, Color.WHITE, 1); eventLine(createDetails, "Solo  ·  Duo  ·  Group", 13, 0xDFFFFFFF, 0); create.setOnClickListener(v -> showPlanCreator(v));
    final LinearLayout suggestion = view.findViewById(R.id.tonight_suggestion); eventLine(suggestion, "✦  SMART SUGGESTION", 11, 0xFFD87EFF, 1); eventLine(suggestion, "Leave now. Queue increasing at Saint.", 15, Color.WHITE, 1); eventLine(suggestion, "Ride prices are dropping — save R35.", 12, 0xCFFFFFFF, 0);
    return view;
  }

  private void addTonightPlan(@NonNull LinearLayout parent, @NonNull String title, @NonNull String timeline, @NonNull String arrival, int accent)
  {
    final LinearLayout plan = new LinearLayout(requireContext()); plan.setOrientation(LinearLayout.VERTICAL); plan.setPadding(dp(14), dp(12), dp(14), dp(12)); plan.setBackground(roundBackground(0xFF211B2A, 17)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(112)); params.setMargins(0, 0, 0, dp(10)); parent.addView(plan, params); eventLine(plan, title + "   ACTIVE", 16, Color.WHITE, 1); eventLine(plan, timeline, 12, 0xDFFFFFFF, 0); TextView arrivalView = eventText(arrival + "   ›", 12, accent, 1); arrivalView.setPadding(0, dp(8), 0, 0); plan.addView(arrivalView); plan.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening " + title, Toast.LENGTH_SHORT).show());
  }

  private void showPlanCreator(@NonNull View anchor)
  {
    new android.app.AlertDialog.Builder(requireContext())
        .setTitle("Choose your night")
        .setItems(new String[] {"Solo — just me", "Duo — you + 1", "Group — squad up"},
            (dialog, which) -> showPlanStartMethod(which == 0 ? "Solo" : which == 1 ? "Duo" : "Group"))
        .show();
  }

  private void showPlanStartMethod(@NonNull String mode)
  {
    new android.app.AlertDialog.Builder(requireContext())
        .setTitle(mode + " plan")
        .setItems(new String[] {"AI Planner", "Start Empty", "Use a Template", "Remix a Public Plan"},
            (dialog, which) -> showPlanReview(mode, new String[] {"AI Planner", "Start Empty", "Template", "Public Plan"}[which]))
        .show();
  }

  private void showPlanReview(@NonNull String mode, @NonNull String startMethod)
  {
    new android.app.AlertDialog.Builder(requireContext())
        .setTitle("Ready to create?")
        .setMessage("Friday Night\n" + mode + " Plan · " + startMethod + "\nStarts 19:00 · Budget R1 200\n\nYou can add venues, friends and rides in the Plan Lobby.")
        .setNegativeButton("Back", null)
        .setPositiveButton("Create Plan", (dialog, which) -> Toast.makeText(requireContext(), "Plan created — opening your Plan Lobby", Toast.LENGTH_SHORT).show())
        .show();
  }

  @NonNull
  private View createSmartPlacesScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.smart_places_screen, container, false);
    view.findViewById(R.id.smart_notifications).setOnClickListener(v -> Toast.makeText(requireContext(), "No new concierge updates", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.smart_profile).setOnClickListener(v -> Toast.makeText(requireContext(), "Your saved plans", Toast.LENGTH_SHORT).show());
    final LinearLayout hero = view.findViewById(R.id.smart_hero);
    eventLine(hero, "★  TONIGHT RECOMMENDATION", 11, 0xFF55E6D0, 1); eventLine(hero, "Your Night Starts Here", 17, 0xFF55E6D0, 0); eventLine(hero, "The Monarch Rooftop", 29, Color.WHITE, 1); eventLine(hero, "★★★★★   4.8 (312 reviews)", 14, 0xFFFFC75E, 1); eventLine(hero, "● Open until 02:00\n♫ Live DJ from 21:00\n♥ Perfect for Date Night\n⚡ Less than 10 min away", 14, 0xEFFFFFFF, 0);
    final LinearLayout heroActions = new LinearLayout(requireContext()); heroActions.setGravity(android.view.Gravity.CENTER_VERTICAL); heroActions.setPadding(0, dp(13), 0, 0); hero.addView(heroActions); TextView route = eventText("➤  Route", 14, 0xFF1B1520, 1); route.setGravity(android.view.Gravity.CENTER); route.setBackground(roundBackground(0xFFFF8079, 15)); heroActions.addView(route, new LinearLayout.LayoutParams(dp(130), dp(45))); TextView venue = eventText("View Venue", 14, Color.WHITE, 1); venue.setGravity(android.view.Gravity.CENTER); venue.setBackgroundResource(R.drawable.event_chip_background); LinearLayout.LayoutParams venueParams = new LinearLayout.LayoutParams(dp(125), dp(45)); venueParams.setMargins(dp(10), 0, 0, 0); heroActions.addView(venue, venueParams); route.setOnClickListener(v -> Toast.makeText(requireContext(), "Routing to The Monarch Rooftop", Toast.LENGTH_SHORT).show()); venue.setOnClickListener(v -> showUniversalVenuePreview(hero, "The Monarch Rooftop", "Sandton", "rooftops", "4.8 (312 reviews)", "Open now · Closes 02:00", "4.2 km", "Rooftop · Cocktails · Live DJ"));
    final LinearLayout moods = view.findViewById(R.id.smart_moods); for (String mood : new String[] {"🔥 Tonight", "♥ Date Night", "🎉 Party", "🍸 Cocktails", "♫ Live Music", "🌇 Rooftops", "☾ Late Night"}) { TextView chip = eventText(mood, 12, 0xEFFFFFFF, 0); chip.setGravity(android.view.Gravity.CENTER); chip.setBackgroundResource(R.drawable.event_chip_background); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(108), dp(36)); p.setMargins(0, 0, dp(8), 0); moods.addView(chip, p); chip.setOnClickListener(v -> { chip.setTextColor(0xFF16121B); chip.setBackgroundResource(R.drawable.event_chip_selected); Toast.makeText(requireContext(), "Recommendations updated for " + mood, Toast.LENGTH_SHORT).show(); }); }
    final LinearLayout guides = view.findViewById(R.id.smart_guides); for (String guide : new String[] {"Tonight's\nBest Picks\n12 venues", "Hidden\nRooftops\n8 venues", "Perfect\nFirst Date\n10 venues", "After\nMidnight\n24 venues", "Live Music\nTonight\n12 venues"}) { TextView card = eventText(guide, 17, Color.WHITE, 1); card.setGravity(android.view.Gravity.BOTTOM); card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(roundBackground(0xFF242035, 18)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(142), dp(158)); p.setMargins(0, 0, dp(10), 0); guides.addView(card, p); card.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening guide", Toast.LENGTH_SHORT).show()); }
    final LinearLayout timeline = view.findViewById(R.id.smart_timeline); eventLine(timeline, "✧  Your Tonight Timeline", 20, Color.WHITE, 1); eventLine(timeline, "Ideas to inspire your night", 12, 0xAFFFFFFF, 0); eventLine(timeline, "8:00 PM  Dinner   ·   9:30 PM  Cocktails   ·   11:00 PM  Live DJ   ·   1:00 AM  Late Night Lounge", 13, 0xFFFFC477, 1);
    final LinearLayout pulse = view.findViewById(R.id.smart_pulse); for (String metric : new String[] {"145\nEvents Tonight", "38\nRooftops Open", "21\nLive Shows", "Safe\nRoutes"}) { TextView item = eventText(metric, 13, Color.WHITE, 1); item.setGravity(android.view.Gravity.CENTER); item.setBackground(roundBackground(0xFF1A1823, 16)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(90), 1); p.setMargins(0, 0, dp(7), 0); pulse.addView(item, p); }
    return view;
  }

  @NonNull
  private View createPrepRoomsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.prep_rooms_screen, container, false);
    view.findViewById(R.id.prep_back).setOnClickListener(v -> requireActivity().onBackPressed());
    view.findViewById(R.id.prep_search).setOnClickListener(v -> Toast.makeText(requireContext(), "Search looks, tutorials and creators", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.prep_fab).setOnClickListener(v -> { PopupMenu menu = new PopupMenu(requireContext(), v); menu.getMenu().add("Create outfit poll"); menu.getMenu().add("Post a look"); menu.getMenu().add("Create preparation plan"); menu.setOnMenuItemClickListener(item -> { Toast.makeText(requireContext(), item.getTitle(), Toast.LENGTH_SHORT).show(); return true; }); menu.show(); });
    final String[] tabNames = {"Following", "Trending", "Nearby", "Events"}; final TextView[] tabs = new TextView[tabNames.length]; final LinearLayout tabRow = view.findViewById(R.id.prep_tabs);
    for (int i = 0; i < tabNames.length; ++i) { TextView tab = eventText(tabNames[i], 14, i == 1 ? Color.WHITE : 0x9FFFFFFF, i == 1 ? 1 : 0); tab.setGravity(android.view.Gravity.CENTER); tabRow.addView(tab, new LinearLayout.LayoutParams(dp(92), dp(40))); tabs[i] = tab; final String selected = tabNames[i]; tab.setOnClickListener(v -> selectPrepTab(view, tabs, tab, selected)); }
    final LinearLayout filters = view.findViewById(R.id.prep_filters); for (String filter : new String[] {"👗 Looks", "💄 Makeup", "💇 Hair", "💅 Nails", "👞 Shoes", "🎥 Tutorials", "✨ Tips"}) { TextView chip = eventText(filter, 12, 0xDFFFFFFF, 0); chip.setGravity(android.view.Gravity.CENTER); chip.setBackgroundResource(R.drawable.event_chip_background); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(100), dp(32)); params.setMargins(0, 0, dp(8), 0); filters.addView(chip, params); chip.setOnClickListener(v -> { chip.setSelected(!chip.isSelected()); chip.setTextColor(chip.isSelected() ? 0xFFFFC477 : 0xDFFFFFFF); }); }
    final LinearLayout plan = view.findViewById(R.id.prep_plan); eventLine(plan, "TONIGHT", 11, 0xDFFFFFFF, 1); eventLine(plan, "AfroHaus Rooftop", 24, Color.WHITE, 1); eventLine(plan, "Starts in 03:12:18  ·  6 friends going", 13, 0xE6FFFFFF, 0); TextView continueButton = eventText("Continue Preparing  →", 13, 0xFF1E1623, 1); continueButton.setGravity(android.view.Gravity.CENTER); continueButton.setBackground(roundBackground(0xFFFFC477, 14)); LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(dp(185), dp(38)); continueParams.setMargins(0, dp(13), 0, 0); plan.addView(continueButton, continueParams); continueButton.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening AfroHaus preparation session", Toast.LENGTH_SHORT).show());
    final LinearLayout circle = view.findViewById(R.id.prep_circle); for (String person : new String[] {"Sarah\nOutfit poll", "Jessica\nTutorial", "Mike\nPreparing", "Sip Squad\n4 active"}) { TextView item = eventText(person, 11, Color.WHITE, 1); item.setGravity(android.view.Gravity.CENTER); item.setBackground(roundBackground(0xFF292232, 16)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(92), dp(56)); params.setMargins(0, 0, dp(9), 0); circle.addView(item, params); }
    selectPrepTab(view, tabs, tabs[1], "Trending"); return view;
  }

  private void selectPrepTab(@NonNull View root, @NonNull TextView[] tabs, @NonNull TextView selected, @NonNull String name)
  {
    for (TextView tab : tabs) { boolean active = tab == selected; tab.setTextColor(active ? Color.WHITE : 0x9FFFFFFF); tab.setTypeface(null, active ? 1 : 0); tab.setBackgroundColor(active ? 0x26B75DFF : Color.TRANSPARENT); }
    final LinearLayout feed = root.findViewById(R.id.prep_feed); feed.removeAllViews(); ((TextView) root.findViewById(R.id.prep_feed_heading)).setText(("Events".equals(name) ? "VENUE GUIDES" : name + " LOOKS").toUpperCase());
    if ("Nearby".equals(name)) { addPrepCard(feed, "Need Nails?", "Glow Nails · 1.4 km", "Navigate  →", 0xFF744D73); addPrepCard(feed, "Need Hair?", "Elite Barber · 800 m", "Navigate  →", 0xFF436C80); return; }
    if ("Events".equals(name)) { addPrepCard(feed, "Luxury Friday", "Dress code: Smart Casual · Popular colours: Black, Gold, White", "View venue guide  →", 0xFF75563B); addPrepCard(feed, "AfroHaus Rooftop", "28 preparing · Ready meter 84%", "Join preparation session  →", 0xFF624275); return; }
    addPrepCard(feed, "LOOK A  VS  LOOK B", "Sarah posted an outfit poll for AfroHaus Rooftop", "Vote  ·  Comment  ·  Share", 0xFF7B405C); addPrepCard(feed, "5-MINUTE GLOW MAKEUP", "Jessica's tutorial · Save for Friday", "Watch tutorial  →", 0xFF9A5D59); addPrepCard(feed, "PREP TIP", "Cold tonight — bring a jacket. Parking is limited; use a ride.", "Save tip  →", 0xFF405D70);
  }

  private void addPrepCard(@NonNull LinearLayout parent, @NonNull String title, @NonNull String detail, @NonNull String action, int accent)
  {
    final LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(16), dp(16), dp(14)); card.setBackground(roundBackground(0xFF191620, 20)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(143)); params.setMargins(0, 0, 0, dp(12)); parent.addView(card, params); TextView titleView = eventText(title, 18, Color.WHITE, 1); titleView.setBackground(roundBackground(accent, 12)); titleView.setGravity(android.view.Gravity.CENTER_VERTICAL); titleView.setPadding(dp(12), 0, dp(12), 0); card.addView(titleView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40))); eventLine(card, detail, 12, 0xCFFFFFFF, 0); TextView actionView = eventText(action, 12, 0xFFFFC477, 1); actionView.setPadding(0, dp(9), 0, 0); card.addView(actionView); actionView.setOnClickListener(v -> Toast.makeText(requireContext(), action, Toast.LENGTH_SHORT).show());
  }

  @NonNull
  private View createMyCircleScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.my_circle_screen, container, false);
    view.findViewById(R.id.circle_search_action).setOnClickListener(v -> Toast.makeText(requireContext(), "Search your circle", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.circle_add_action).setOnClickListener(v -> showCircleActions(v));
    view.findViewById(R.id.circle_search).setOnClickListener(v -> Toast.makeText(requireContext(), "Search people, venues, events and music", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.circle_fab).setOnClickListener(this::showCircleActions);
    final LinearLayout stories = view.findViewById(R.id.circle_stories);
    addCircleStory(stories, "＋", "Your Story", 0xFF55505D, "");
    addCircleStory(stories, "A", "Amanda", 0xFF8A367D, "●"); addCircleStory(stories, "J", "Jason", 0xFFD24C5E, "LIVE");
    addCircleStory(stories, "S", "Sarah", 0xFFB88642, "EVENT"); addCircleStory(stories, "T", "Tyler", 0xFF4277A2, "⌖"); addCircleStory(stories, "J", "Jessica", 0xFF487E69, "★");
    final LinearLayout metrics = view.findViewById(R.id.circle_metrics);
    for (String metric : new String[] {"● 18 Online", "▣ 9 Stories", "● 4 Live", "✦ 12 Events", "⌖ 7 Nearby"})
    {
      final TextView item = eventText(metric, 12, 0xDFFFFFFF, 1); item.setGravity(android.view.Gravity.CENTER); item.setBackground(roundBackground(0xFF211C29, 16)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(105), dp(32)); params.setMargins(0, 0, dp(8), 0); metrics.addView(item, params); item.setOnClickListener(v -> Toast.makeText(requireContext(), metric, Toast.LENGTH_SHORT).show());
    }
    final String[] names = {"Activity", "Friends", "Discover", "Nearby", "Requests"}; final TextView[] tabs = new TextView[names.length]; final LinearLayout tabRow = view.findViewById(R.id.circle_tabs);
    for (int i = 0; i < names.length; ++i)
    {
      final TextView tab = eventText(names[i], 14, i == 0 ? Color.WHITE : 0x9FFFFFFF, i == 0 ? 1 : 0); tab.setGravity(android.view.Gravity.CENTER); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(88), dp(40)); tabRow.addView(tab, params); tabs[i] = tab; final String name = names[i]; tab.setOnClickListener(v -> selectCircleTab(view, tabs, tab, name));
    }
    selectCircleTab(view, tabs, tabs[0], "Activity");
    return view;
  }

  private void addCircleStory(@NonNull LinearLayout row, @NonNull String initial, @NonNull String name, int color, @NonNull String badge)
  {
    final LinearLayout holder = new LinearLayout(requireContext()); holder.setGravity(android.view.Gravity.CENTER_HORIZONTAL); holder.setOrientation(LinearLayout.VERTICAL);
    final TextView avatar = eventText(initial, 27, Color.WHITE, 1); avatar.setGravity(android.view.Gravity.CENTER); avatar.setBackground(circleBackground(color)); holder.addView(avatar, new LinearLayout.LayoutParams(dp(69), dp(69)));
    final TextView label = eventText(name + (badge.isEmpty() ? "" : " " + badge), 10, 0xDFFFFFFF, 0); label.setGravity(android.view.Gravity.CENTER); holder.addView(label, new LinearLayout.LayoutParams(dp(82), dp(28))); avatar.setOnClickListener(v -> Toast.makeText(requireContext(), "Viewing " + name + "’s story", Toast.LENGTH_SHORT).show());
    row.addView(holder, new LinearLayout.LayoutParams(dp(82), dp(102)));
  }

  @NonNull
  private GradientDrawable circleBackground(int color)
  {
    final GradientDrawable drawable = new GradientDrawable(); drawable.setShape(GradientDrawable.OVAL); drawable.setColor(color); drawable.setStroke(dp(3), 0xFFBB5CFF); return drawable;
  }

  private void showCircleActions(@NonNull View anchor)
  {
    final PopupMenu menu = new PopupMenu(requireContext(), anchor); menu.getMenu().add("Add Friend"); menu.getMenu().add("Create Story"); menu.getMenu().add("Scan QR"); menu.getMenu().add("My QR Code"); menu.getMenu().add("Find Nearby People"); menu.setOnMenuItemClickListener(item -> { Toast.makeText(requireContext(), item.getTitle(), Toast.LENGTH_SHORT).show(); return true; }); menu.show();
  }

  private void selectCircleTab(@NonNull View root, @NonNull TextView[] tabs, @NonNull TextView selected, @NonNull String tabName)
  {
    for (TextView tab : tabs) { boolean active = tab == selected; tab.setTextColor(active ? Color.WHITE : 0x9FFFFFFF); tab.setTypeface(null, active ? 1 : 0); tab.setBackgroundColor(active ? 0x26B75DFF : Color.TRANSPARENT); }
    final LinearLayout feed = root.findViewById(R.id.circle_feed); feed.removeAllViews();
    if ("Activity".equals(tabName)) { addCircleCard(feed, "A", "Amanda", "Sharing a Moment", "Altitude Club · 3 min ago", "View   ·   Route", 0xFF8A367D); addCircleCard(feed, "J", "Jason", "LIVE at Madison Lounge", "182 watching · now", "Watch   ·   Route", 0xFFD24C5E); addCircleCard(feed, "S", "Sarah", "Amapiano Fridays", "Tonight · Truth Nightclub", "View Event   ·   Invite", 0xFFB88642); return; }
    if ("Friends".equals(tabName)) { addCircleCard(feed, "N", "Nomsa", "3 mutual friends", "Getting ready for tonight", "Message   ·   Invite Out", 0xFF467A9D); addCircleCard(feed, "K", "Kagiso", "Same events", "At The Living Room", "Message   ·   View Profile", 0xFF73527F); return; }
    if ("Discover".equals(tabName)) { addCircleCard(feed, "L", "Lerato", "People you may know", "12 mutual friends · Loves Amapiano", "Add Friend   ·   Follow", 0xFF596B98); addCircleCard(feed, "M", "Mpho", "Met at Rockets Lounge", "Going to the same event", "Add Friend   ·   View", 0xFF9A6747); return; }
    if ("Nearby".equals(tabName)) { addCircleCard(feed, "M", "Mike", "650 m away", "Rockets · available now", "Route   ·   Invite Out", 0xFF3E897C); return; }
    addCircleCard(feed, "P", "Palesa", "Friend request", "8 mutual friends", "Accept   ·   Decline", 0xFF88614A);
  }

  private void addCircleCard(@NonNull LinearLayout parent, @NonNull String initial, @NonNull String name, @NonNull String activity, @NonNull String context, @NonNull String actions, int color)
  {
    final LinearLayout card = new LinearLayout(requireContext()); card.setPadding(dp(13), dp(13), dp(13), dp(13)); card.setGravity(android.view.Gravity.CENTER_VERTICAL); card.setBackground(roundBackground(0xFF191620, 20)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(112)); params.setMargins(0, 0, 0, dp(12)); parent.addView(card, params);
    final TextView avatar = eventText(initial, 22, Color.WHITE, 1); avatar.setGravity(android.view.Gravity.CENTER); avatar.setBackground(circleBackground(color)); card.addView(avatar, new LinearLayout.LayoutParams(dp(58), dp(58)));
    final LinearLayout details = new LinearLayout(requireContext()); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(12), 0, 0, 0); card.addView(details, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)); eventLine(details, name + "  ✓", 16, Color.WHITE, 1); eventLine(details, activity, 13, 0xDFFFFFFF, 0); eventLine(details, context, 11, 0x9FFFFFFF, 0); final TextView action = eventText(actions, 11, 0xFFFFC477, 1); action.setPadding(0, dp(5), 0, 0); details.addView(action); action.setOnClickListener(v -> Toast.makeText(requireContext(), actions, Toast.LENGTH_SHORT).show());
  }

  @NonNull
  private View createFlashDropsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.flash_drops_screen, container, false);
    view.findViewById(R.id.drops_back).setOnClickListener(v -> requireActivity().onBackPressed());
    view.findViewById(R.id.drops_filter).setOnClickListener(v -> Toast.makeText(requireContext(), "Flash Drop filters and sorting", Toast.LENGTH_SHORT).show());
    final String[] categories = {"all", "venue", "event", "brand"};
    final TextView[] chips = new TextView[categories.length];
    final LinearLayout filterRow = view.findViewById(R.id.drops_filters);
    for (int i = 0; i < categories.length; ++i)
    {
      final TextView chip = eventText(categories[i].toUpperCase(), 13, i == 0 ? 0xFF16121B : 0xFFFFFFFF, i == 0 ? 1 : 0);
      chip.setGravity(android.view.Gravity.CENTER); chip.setPadding(dp(18), 0, dp(18), 0); chip.setBackgroundResource(i == 0 ? R.drawable.event_chip_selected : R.drawable.event_chip_background);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(38)); params.setMargins(0, 0, dp(10), 0); filterRow.addView(chip, params); chips[i] = chip;
      final String category = categories[i]; chip.setOnClickListener(v -> selectDropCategory(view, chips, chip, category));
    }
    selectDropCategory(view, chips, chips[0], "all");
    return view;
  }

  private void selectDropCategory(@NonNull View root, @NonNull TextView[] chips, @NonNull TextView selected, @NonNull String category)
  {
    for (TextView chip : chips)
    {
      final boolean active = chip == selected; chip.setTextColor(active ? 0xFF16121B : Color.WHITE); chip.setTypeface(null, active ? 1 : 0);
      chip.setBackgroundResource(active ? R.drawable.event_chip_selected : R.drawable.event_chip_background);
    }
    loadFlashDrops(root, category);
  }

  private void loadFlashDrops(@NonNull View root, @NonNull String category)
  {
    final LinearLayout list = root.findViewById(R.id.drops_list); list.removeAllViews();
    new FlashDropsRepository().load(category, new FlashDropsRepository.Callback()
    {
      @Override public void onLoaded(@NonNull JSONArray drops)
      {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> bindFlashDrops(root, drops));
      }
      @Override public void onUnavailable()
      {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> bindFlashDropFallback(root, category));
      }
    });
  }

  private void bindFlashDrops(@NonNull View root, @NonNull JSONArray drops)
  {
    final LinearLayout list = root.findViewById(R.id.drops_list); list.removeAllViews();
    ((TextView) root.findViewById(R.id.drops_live_count)).setText(drops.length() + " LIVE");
    if (drops.length() == 0) { addFlashEmptyState(list); return; }
    bindFlashHero(root, drops.optJSONObject(0));
    for (int i = 0; i < drops.length(); ++i) addFlashCard(list, drops.optJSONObject(i));
    updateDropTimers(root);
  }

  private void bindFlashDropFallback(@NonNull View root, @NonNull String category)
  {
    final JSONArray drops = new JSONArray();
    try
    {
      if ("all".equals(category) || "venue".equals(category)) drops.put(flashJson("FREE COCKTAIL", "LIV Sandton", "venue", "First 50 guests receive one complimentary cocktail.", "17 Sandton Drive, Sandton, 2196", "COMPLIMENTARY COCKTAIL", "JUST DROPPED", 1122, "0.3", 24));
      if ("all".equals(category) || "venue".equals(category)) drops.put(flashJson("FREE ENTRY BEFORE 11PM", "The Den Randburg", "venue", "Skip the line and get in free before 11PM tonight.", "30 Hill Street, Randburg, 2194", "FREE ENTRY", "TRENDING", 1937, "1.2", 34));
      if ("all".equals(category) || "event".equals(category)) drops.put(flashJson("SURPRISE DJ SET", "Rooftop Sundays", "event", "International DJ. Secret set. You have to be there.", "20 Anderson Street, Maboneng, 2094", "LIVE DJ SET", "ENDS SOON", 454, "1.8", 18));
      if ("all".equals(category) || "brand".equals(category)) drops.put(flashJson("FREE SAMPLES", "Red Bull", "brand", "Energy up. Good times. Limited to first 100 people.", "1 Melrose Boulevard, Melrose, 2196", "FREE SAMPLES", "LIMITED", 1333, "2.6", 11));
    }
    catch (JSONException ignored) { }
    bindFlashDrops(root, drops);
  }

  @NonNull
  private JSONObject flashJson(@NonNull String title, @NonNull String source, @NonNull String category, @NonNull String description, @NonNull String address, @NonNull String offer, @NonNull String status, long seconds, @NonNull String distance, int interests) throws JSONException
  {
    return new JSONObject().put("title", title).put("source_name", source).put("category", category).put("description", description).put("address", address).put("offer_text", offer).put("status", status).put("seconds_remaining", seconds).put("distance_km", distance).put("interested_count", interests).put("source_verified", true);
  }

  private void bindFlashHero(@NonNull View root, @Nullable JSONObject drop)
  {
    if (drop == null) return;
    ((TextView) root.findViewById(R.id.drops_hero_status)).setText("⚡  " + drop.optString("status", "JUST DROPPED"));
    ((TextView) root.findViewById(R.id.drops_hero_title)).setText(drop.optString("title"));
    ((TextView) root.findViewById(R.id.drops_hero_source)).setText(drop.optString("source_name") + (drop.optBoolean("source_verified") ? "  ✓" : ""));
    ((TextView) root.findViewById(R.id.drops_hero_description)).setText(drop.optString("description"));
    ((TextView) root.findViewById(R.id.drops_hero_meta)).setText("⌖  " + drop.optString("address") + "  ·  " + drop.optString("distance_km") + " km away  ·  " + drop.optString("offer_text"));
    final TextView timer = root.findViewById(R.id.drops_hero_timer); timer.setTag(System.currentTimeMillis() + drop.optLong("seconds_remaining", 0) * 1000L); setDropTimer(timer);
  }

  private void addFlashCard(@NonNull LinearLayout list, @Nullable JSONObject drop)
  {
    if (drop == null) return;
    final LinearLayout card = new LinearLayout(requireContext()); card.setGravity(android.view.Gravity.CENTER_VERTICAL); card.setPadding(dp(10), dp(10), dp(11), dp(10)); card.setBackground(roundBackground(0xFF17141E, 18));
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(164)); cardParams.setMargins(0, 0, 0, dp(11)); list.addView(card, cardParams);
    final TextView art = eventText(drop.optString("status", "LIVE"), 11, Color.WHITE, 1); art.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL); art.setPadding(dp(8), dp(12), dp(8), 0); art.setBackground(roundBackground(drop.optString("category").hashCode() | 0xFF000000, 13)); card.addView(art, new LinearLayout.LayoutParams(dp(112), LinearLayout.LayoutParams.MATCH_PARENT));
    final LinearLayout content = new LinearLayout(requireContext()); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(12), 0, 0, 0); card.addView(content, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
    final LinearLayout titleRow = new LinearLayout(requireContext()); titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL); content.addView(titleRow);
    final TextView title = eventText(drop.optString("title"), 16, Color.WHITE, 1); title.setMaxLines(1); titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(28), 1));
    final TextView like = eventText("♡", 22, 0xFFFF7BD5, 0); like.setGravity(android.view.Gravity.CENTER); titleRow.addView(like, new LinearLayout.LayoutParams(dp(32), dp(30))); like.setOnClickListener(v -> { like.setText("♥"); });
    eventLine(content, drop.optString("source_name") + (drop.optBoolean("source_verified") ? "  ✓" : ""), 12, 0xCFFFFFFF, 1);
    final TextView description = eventText(drop.optString("description"), 11, 0xD9FFFFFF, 0); description.setMaxLines(2); content.addView(description, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(31)));
    eventLine(content, drop.optString("schedule", "Tonight") + "  ·  " + drop.optString("offer_text"), 10, 0xFF92E3D1, 1);
    final TextView timer = eventText("", 17, 0xFFFFB948, 1); timer.setGravity(android.view.Gravity.CENTER_VERTICAL); timer.setTag(System.currentTimeMillis() + drop.optLong("seconds_remaining", 0) * 1000L); content.addView(timer, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(26))); setDropTimer(timer);
    eventLine(content, "⌖ " + drop.optString("address") + " · " + drop.optString("distance_km") + " km  ·  ♥ " + drop.optLong("interested_count") + " interested  ·  " + drop.optString("category").toUpperCase(), 10, 0xAFFFFFFF, 0);
    card.setOnClickListener(v -> Toast.makeText(requireContext(), "Opening Flash Drop: " + drop.optString("title"), Toast.LENGTH_SHORT).show());
  }

  private void updateDropTimers(@NonNull View root)
  {
    final TextView heroTimer = root.findViewById(R.id.drops_hero_timer); setDropTimer(heroTimer);
    final LinearLayout list = root.findViewById(R.id.drops_list);
    for (int i = 0; i < list.getChildCount(); ++i) updateTimersInView(list.getChildAt(i));
    root.postDelayed(() -> { if (isAdded() && root.isShown()) updateDropTimers(root); }, 1000L);
  }

  private void updateTimersInView(@NonNull View view)
  {
    if (view instanceof TextView && view.getTag() instanceof Long) setDropTimer((TextView) view);
    if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); ++i) updateTimersInView(((ViewGroup) view).getChildAt(i));
  }

  private void setDropTimer(@NonNull TextView timer)
  {
    if (!(timer.getTag() instanceof Long)) return;
    final long seconds = Math.max(0, ((Long) timer.getTag() - System.currentTimeMillis()) / 1000L);
    timer.setText(seconds == 0 ? "ENDED" : "ENDS IN\n" + String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60));
  }

  private void addFlashEmptyState(@NonNull LinearLayout list)
  {
    final TextView empty = eventText("⚡\n\nNO FLASH DROPS RIGHT NOW\nNothing is happening nearby yet.\nWe’ll show you when something drops.\n\nEXPLORE NEARBY", 15, Color.WHITE, 1); empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(dp(22), dp(36), dp(22), dp(36)); empty.setBackground(roundBackground(0xFF17141E, 20)); list.addView(empty, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240)));
  }

  @NonNull
  private View createExploreCityScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.explore_city_screen, container, false);
    view.findViewById(R.id.explore_back).setOnClickListener(v -> requireActivity().onBackPressed());
    view.findViewById(R.id.explore_search).setOnClickListener(v -> Toast.makeText(requireContext(), "Search places, experiences and suburbs", Toast.LENGTH_SHORT).show());
    bindExploreHero(view);
    final String[] worlds = exploreWorldOrder();
    final LinearLayout worldRow = view.findViewById(R.id.explore_worlds);
    final TextView[] chips = new TextView[worlds.length];
    for (int i = 0; i < worlds.length; ++i)
    {
      final TextView chip = new TextView(requireContext());
      chip.setText(exploreWorldLabel(worlds[i])); chip.setTextSize(13); chip.setGravity(android.view.Gravity.CENTER);
      chip.setPadding(dp(15), 0, dp(15), 0); chip.setTextColor(i == 0 ? 0xFF16121B : 0xCCFFFFFF);
      chip.setTypeface(null, i == 0 ? 1 : 0); chip.setBackgroundResource(i == 0 ? R.drawable.event_chip_selected : R.drawable.event_chip_background);
      final LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(37)); chipParams.setMargins(0, 0, dp(9), 0); worldRow.addView(chip, chipParams);
      chips[i] = chip;
      final String world = worlds[i];
      chip.setOnClickListener(v -> selectExploreWorld(view, chips, chip, world));
    }
    selectExploreWorld(view, chips, chips[0], worlds[0]);
    view.findViewById(R.id.explore_see_all).setOnClickListener(v -> Toast.makeText(requireContext(), "Showing all " + v.getTag(), Toast.LENGTH_SHORT).show());
    return view;
  }

  private void bindExploreHero(@NonNull View view)
  {
    final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    final TextView eyebrow = view.findViewById(R.id.explore_hero_eyebrow);
    final TextView title = view.findViewById(R.id.explore_hero_title);
    final TextView body = view.findViewById(R.id.explore_hero_body);
    if (hour < 5) { eyebrow.setText("☾  LATE NIGHT"); title.setText("Johannesburg Never Sleeps"); body.setText("Late food, coffee and places still open around you."); }
    else if (hour < 11) { eyebrow.setText("☀  GOOD MORNING"); title.setText("Good morning, Johannesburg"); body.setText("Start your day or plan tonight."); }
    else if (hour < 17) { eyebrow.setText("✦  EXPLORE JOHANNESBURG"); title.setText("Discover what’s happening later today"); body.setText("Great food, shopping and places to discover nearby."); }
    else if (hour < 21) { eyebrow.setText("✦  GOOD EVENING"); title.setText("The City Is Coming Alive"); body.setText("Nightlife, rooftops and unforgettable experiences await."); }
    else { eyebrow.setText("✦  PEAK HOURS"); title.setText("Tonight is in motion"); body.setText("Find the city’s best places still open right now."); }
  }

  @NonNull
  private String[] exploreWorldOrder()
  {
    final Calendar now = Calendar.getInstance();
    final int hour = now.get(Calendar.HOUR_OF_DAY);
    final int day = now.get(Calendar.DAY_OF_WEEK);
    final boolean weekend = day == Calendar.FRIDAY || day == Calendar.SATURDAY;
    final boolean holiday = isSouthAfricanPublicHoliday(now);
    if (hour >= 5 && hour < 11) return holiday ? new String[] {"food", "travel", "wellness", "prep", "always_open", "nightlife"} : new String[] {"prep", "wellness", "food", "travel", "always_open", "nightlife"};
    if (hour < 17) return (weekend || holiday) ? new String[] {"food", "travel", "prep", "wellness", "always_open", "nightlife"} : new String[] {"food", "prep", "travel", "wellness", "always_open", "nightlife"};
    if (hour < 21) return new String[] {"nightlife", "food", "rooftops", "casinos", "prep", "always_open", "wellness"};
    if (hour >= 2 && hour < 5) return new String[] {"always_open", "food", "casinos", "wellness", "nightlife"};
    return new String[] {"nightlife", "casinos", "food", "always_open", "rooftops", "wellness", "travel"};
  }

  private boolean isSouthAfricanPublicHoliday(@NonNull Calendar date)
  {
    final int month = date.get(Calendar.MONTH);
    final int day = date.get(Calendar.DAY_OF_MONTH);
    // Fixed-date national holidays. The server table remains the authoritative source for movable holidays and observances.
    return (month == Calendar.JANUARY && day == 1) || (month == Calendar.MARCH && day == 21)
        || (month == Calendar.APRIL && (day == 27 || day == 28)) || (month == Calendar.MAY && day == 1)
        || (month == Calendar.JUNE && day == 16) || (month == Calendar.AUGUST && day == 9)
        || (month == Calendar.SEPTEMBER && day == 24) || (month == Calendar.DECEMBER && (day == 16 || day == 25 || day == 26));
  }

  @NonNull
  private String exploreWorldLabel(@NonNull String world)
  {
    if ("nightlife".equals(world)) return "🌙  Nightlife";
    if ("food".equals(world)) return "🍴  Food";
    if ("prep".equals(world)) return "✦  Prep";
    if ("wellness".equals(world)) return "◌  Wellness";
    if ("travel".equals(world)) return "✈  Travel";
    if ("rooftops".equals(world)) return "🌇  Rooftops";
    if ("casinos".equals(world)) return "🎰  Casinos";
    return "◷  24/7";
  }

  private void selectExploreWorld(@NonNull View root, @NonNull TextView[] chips, @NonNull TextView selected, @NonNull String world)
  {
    for (TextView chip : chips)
    {
      final boolean active = chip == selected; chip.setTextColor(active ? 0xFF16121B : 0xCCFFFFFF); chip.setTypeface(null, active ? 1 : 0);
      chip.setBackgroundResource(active ? R.drawable.event_chip_selected : R.drawable.event_chip_background);
    }
    final String[] copy = exploreCopy(world);
    ((TextView) root.findViewById(R.id.explore_section_title)).setText(copy[0]);
    ((TextView) root.findViewById(R.id.explore_section_subtitle)).setText(copy[1]);
    root.findViewById(R.id.explore_see_all).setTag(selected.getText());
    loadExploreVenues(root, world);
  }

  @NonNull
  private String[] exploreCopy(@NonNull String world)
  {
    if ("food".equals(world)) return new String[] {"Places to Eat", "Restaurants, cafés and dining experiences nearby."};
    if ("prep".equals(world)) return new String[] {"Get Ready", "Everything you need before heading out."};
    if ("wellness".equals(world)) return new String[] {"Time to Recharge", "Wellness experiences nearby."};
    if ("travel".equals(world)) return new String[] {"Explore Local Gems", "Discover iconic places and memorable experiences."};
    if ("rooftops".equals(world)) return new String[] {"Above the City", "Golden-hour rooftops and skyline views nearby."};
    if ("casinos".equals(world)) return new String[] {"The Night Is Still Young", "Casinos and late-night entertainment nearby."};
    if ("always_open".equals(world)) return new String[] {"Always Open", "Places you can visit any time of day or night."};
    return new String[] {"Tonight's Hotspots", "Discover the city's best nightlife venues."};
  }

  private void loadExploreVenues(@NonNull View root, @NonNull String world)
  {
    final LinearLayout venues = root.findViewById(R.id.explore_venues); venues.removeAllViews();
    new CityExploreRepository().load(world, new CityExploreRepository.Callback()
    {
      @Override public void onLoaded(@NonNull JSONArray data)
      {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
          venues.removeAllViews();
          for (int i = 0; i < data.length(); ++i) addExploreVenue(venues, data.optJSONObject(i), world);
          if (data.length() == 0) addExploreFallbacks(venues, world);
        });
      }
      @Override public void onUnavailable()
      {
        if (isAdded()) requireActivity().runOnUiThread(() -> { venues.removeAllViews(); addExploreFallbacks(venues, world); });
      }
    });
  }

  private void addExploreFallbacks(@NonNull LinearLayout venues, @NonNull String world)
  {
    if ("food".equals(world)) { addExploreVenue(venues, "Proud Mary", "Rosebank", "Modern dining", "4.7", "Open · Closes 23:00", "1.1 km", "Brunch · Cocktails", 0xFF8A4D32); addExploreVenue(venues, "Marble", "Rosebank", "Fine dining", "4.8", "Open · Closes 22:30", "1.3 km", "Fire cooking · Views", 0xFF724743); return; }
    if ("prep".equals(world)) { addExploreVenue(venues, "Legends Barbershop", "Braamfontein", "Barber", "4.6", "Open · Closes 19:00", "0.8 km", "Walk-ins · Fresh cuts", 0xFF3E6572); addExploreVenue(venues, "The Sneaker Laundry", "Rosebank", "Sneaker care", "4.7", "Open · Closes 18:00", "1.7 km", "Cleaning · Customisation", 0xFF685B81); return; }
    if ("wellness".equals(world)) { addExploreVenue(venues, "Moya Yoga", "Parkhurst", "Yoga studio", "4.8", "Open · Closes 20:00", "2.2 km", "Yoga · Breathwork", 0xFF3A786D); addExploreVenue(venues, "Amani Spa", "Houghton", "Day spa", "4.7", "Open · Closes 19:00", "3.1 km", "Massage · Recovery", 0xFF517A7C); return; }
    if ("travel".equals(world)) { addExploreVenue(venues, "The Four Seasons", "Westcliff", "Hotel", "4.8", "Open now", "4.5 km", "City views · Dining", 0xFF79653C); addExploreVenue(venues, "Constitution Hill", "Braamfontein", "Local landmark", "4.6", "Open · Closes 17:00", "1.9 km", "History · Tours", 0xFF6F5E48); return; }
    if ("always_open".equals(world)) { addExploreVenue(venues, "McDonald's", "Braamfontein", "24-hour restaurant", "4.2", "Open 24 hours", "0.6 km", "Food · Drive-through", 0xFF895338); addExploreVenue(venues, "Engen 1 Plus", "Parktown", "24-hour convenience", "4.3", "Open 24 hours", "1.2 km", "Coffee · Convenience", 0xFF3C647B); return; }
    if ("rooftops".equals(world)) { addExploreVenue(venues, "The Living Room", "Maboneng", "Rooftop lounge", "4.7", "Open · Closes 02:00", "1.6 km", "Skyline · Cocktails", 0xFF8E5733); addExploreVenue(venues, "The Rand Club", "Johannesburg CBD", "Rooftop bar", "4.6", "Open · Closes 23:00", "2.3 km", "Sunset · Dining", 0xFF7A4764); return; }
    if ("casinos".equals(world)) { addExploreVenue(venues, "Gold Reef City", "Ormonde", "Casino", "4.5", "Open late", "7.8 km", "Gaming · Entertainment", 0xFF675437); addExploreVenue(venues, "Montecasino", "Fourways", "Casino", "4.6", "Open late", "17 km", "Gaming · Dining", 0xFF4B507A); return; }
    addExploreVenue(venues, "Konka", "Rosebank", "Cocktail bar", "4.8", "Open · Closes 04:00", "2.4 km", "Live DJ · Dance floor", 0xFF742456); addExploreVenue(venues, "The Living Room", "Maboneng", "Rooftop lounge", "4.7", "Open · Closes 02:00", "1.6 km", "Rooftop · Cocktails", 0xFF8E5733);
  }

  private void addExploreVenue(@NonNull LinearLayout parent, @Nullable JSONObject venue, @NonNull String world)
  {
    if (venue == null) return;
    final String closes = venue.optBoolean("is_24_hours") ? "Open 24 hours" : venue.optBoolean("open_now") ? "Open · Closes " + venue.optString("closes_at", "later") : "Currently closed";
    final String rhythm = venue.optString("rhythm_message", closes);
    final JSONArray tags = venue.optJSONArray("experience_tags");
    final String eventTag = venue.optBoolean("local_event_active") ? " · Tonight’s event" : "";
    addExploreVenue(parent, venue.optString("name"), venue.optString("suburb"), venue.optString("venue_kind"), venue.optString("rating", "—"), rhythm + "\n" + closes, venue.optString("distance_km", "—") + " km", (tags != null ? tags.toString().replace("[", "").replace("]", "").replace("\"", "").replace(",", " ·") : "Curated venue") + eventTag, world.hashCode());
  }

  private void addExploreVenue(@NonNull LinearLayout parent, @NonNull String name, @NonNull String suburb, @NonNull String category, @NonNull String rating, @NonNull String status, @NonNull String distance, @NonNull String tags, int color)
  {
    final LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(14), dp(14), dp(14)); card.setBackground(roundBackground(0xFF18151F, 22));
    final LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(312)); cardParams.setMargins(0, 0, 0, dp(14)); parent.addView(card, cardParams);
    final TextView image = eventText(name.toUpperCase() + "\n" + suburb.toUpperCase(), 18, Color.WHITE, 1); image.setGravity(android.view.Gravity.CENTER); image.setBackground(roundBackground(0xFF000000 | (color & 0x00FFFFFF), 16)); card.addView(image, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(118)));
    final LinearLayout headline = new LinearLayout(requireContext()); headline.setGravity(android.view.Gravity.CENTER_VERTICAL); headline.setPadding(0, dp(10), 0, 0); card.addView(headline);
    final TextView nameText = eventText(name, 19, Color.WHITE, 1); headline.addView(nameText, new LinearLayout.LayoutParams(0, dp(28), 1));
    final TextView save = eventText("♡  Save", 12, 0xFFFFC477, 1); save.setGravity(android.view.Gravity.CENTER); save.setBackground(roundBackground(0xFF2B2533, 13)); headline.addView(save, new LinearLayout.LayoutParams(dp(70), dp(28))); save.setOnClickListener(v -> { save.setText("♥  Saved"); });
    eventLine(card, category + "  ·  " + suburb + "  ·  ★ " + rating, 13, 0xCFFFFFFF, 0);
    eventLine(card, "●  " + status + "   ·   " + distance + " away", 12, 0xFF78D9BB, 0);
    eventLine(card, tags, 12, 0x99FFFFFF, 0);
    final TextView view = eventText("View venue  →", 13, 0xFF18121E, 1); view.setGravity(android.view.Gravity.CENTER); view.setBackground(roundBackground(0xFFFFC477, 14)); LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)); viewParams.setMargins(0, dp(9), 0, 0); card.addView(view, viewParams); view.setOnClickListener(v -> Toast.makeText(requireContext(), "Directions to " + name + " are ready", Toast.LENGTH_SHORT).show());
    card.setOnClickListener(v -> showUniversalVenuePreview(card, name, suburb, category, rating, status, distance, tags));
  }

  private void showUniversalVenuePreview(@NonNull View source, @NonNull String name, @NonNull String suburb, @NonNull String category,
                                           @NonNull String rating, @NonNull String status, @NonNull String distance, @NonNull String tags)
  {
    View root = source;
    while (root.getParent() instanceof View)
      root = (View) root.getParent();
    if (root instanceof ViewGroup)
      UniversalVenuePreview.show((ViewGroup) root, new UniversalVenuePreview.Venue(name, suburb, category, rating,
          suburb + ", Johannesburg", status, distance, "Today · check venue for times", tags, true));
  }

  @NonNull
  private View createEventsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.events_screen, container, false);
    view.findViewById(R.id.events_back).setOnClickListener(v -> requireActivity().onBackPressed());
    view.findViewById(R.id.events_calendar).setOnClickListener(v -> Toast.makeText(requireContext(), "Your event calendar", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.events_settings).setOnClickListener(v -> Toast.makeText(requireContext(), "Event preferences", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.events_search).setOnClickListener(v -> Toast.makeText(requireContext(), "Search events, artists, venues, and genres", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.events_hero).setOnClickListener(v -> showEventPreview(view, "Amapiano Fridays", "Truth Nightclub", "Tonight · 22:00", "R120 from", "0.9 km · 6 min drive", "🔥 Heating up"));

    final TextView[] chips = {view.findViewById(R.id.events_chip_all), view.findViewById(R.id.events_chip_tonight), view.findViewById(R.id.events_chip_week)};
    for (TextView chip : chips)
      chip.setOnClickListener(v -> selectEventChip(chips, (TextView) v, view));

    final LinearLayout feed = view.findViewById(R.id.events_feed);
    addEventCard(feed, view, "Amapiano Fridays", "Truth Nightclub  ✓", "TONIGHT · 22:00", "0.9 km", "🔥 Heating up", "R120 from", 0xFF7B1D62);
    addEventCard(feed, view, "Sundown Sessions", "The Living Room  ✓", "TODAY · 18:30", "1.4 km", "🌊 Peak night", "R80 from", 0xFFBD572A);
    addEventCard(feed, view, "Jazz on the Square", "The Market Theatre", "TOMORROW · 20:00", "2.1 km", "⚡ Trending nearby", "R150 from", 0xFF315F87);
    addEventCard(feed, view, "After Hours: KAYTRANADA", "And Club", "SAT · 23:00", "3.6 km", "🍾 VIP filling fast", "R250 from", 0xFF563E97);
    return view;
  }

  private void selectEventChip(@NonNull TextView[] chips, @NonNull TextView selected, @NonNull View root)
  {
    for (TextView chip : chips)
    {
      final boolean active = chip == selected;
      chip.setBackgroundResource(active ? R.drawable.event_chip_selected : R.drawable.event_chip_background);
      chip.setTextColor(active ? 0xFF16121B : 0xCCFFFFFF);
      chip.setTypeface(null, active ? 1 : 0);
    }
    ((TextView) root.findViewById(R.id.events_feed_title)).setText(selected == chips[0] ? "Nearby & heating up" : selected == chips[1] ? "Tonight near you" : "This week in Joburg");
  }

  private void addEventCard(@NonNull LinearLayout parent, @NonNull View root, @NonNull String name, @NonNull String venue,
                            @NonNull String time, @NonNull String distance, @NonNull String heat, @NonNull String price, int posterColor)
  {
    final LinearLayout card = new LinearLayout(requireContext());
    card.setGravity(android.view.Gravity.CENTER_VERTICAL);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setPadding(dp(10), dp(10), dp(14), dp(10));
    card.setBackground(roundBackground(0xFF17141E, 20));
    final LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(142));
    cardParams.setMargins(0, 0, 0, dp(12));
    parent.addView(card, cardParams);

    final TextView poster = new TextView(requireContext());
    poster.setText(name.equals("Amapiano Fridays") ? "AF\n\nFRI\n22:00" : name.equals("Sundown Sessions") ? "SUN\nDOWN\n\nSESSIONS" : name.equals("Jazz on the Square") ? "JAZZ\nON THE\nSQUARE" : "AFTER\nHOURS");
    poster.setTextColor(Color.WHITE); poster.setTextSize(14); poster.setTypeface(null, 1); poster.setGravity(android.view.Gravity.CENTER);
    poster.setBackground(roundBackground(posterColor, 13));
    card.addView(poster, new LinearLayout.LayoutParams(dp(90), LinearLayout.LayoutParams.MATCH_PARENT));

    final LinearLayout details = new LinearLayout(requireContext()); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(14), dp(1), 0, 0);
    card.addView(details, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    eventLine(details, name, 17, Color.WHITE, 1);
    eventLine(details, venue, 13, 0xCFFFFFFF, 0);
    eventLine(details, time + "  ·  " + distance, 11, 0x91FFFFFF, 1);
    final TextView heatLine = eventText(heat + "   " + price, 12, 0xFFFFC77A, 1); heatLine.setPadding(0, dp(7), 0, 0); details.addView(heatLine);
    card.setOnClickListener(v -> showEventPreview(root, name, venue.replace("  ✓", ""), time, price, distance + " · 6 min drive", heat));
  }

  private void eventLine(@NonNull LinearLayout parent, @NonNull String text, int size, int color, int style)
  {
    final TextView line = eventText(text, size, color, style); line.setMaxLines(1); parent.addView(line);
  }

  @NonNull
  private TextView eventText(@NonNull String text, int size, int color, int style)
  {
    final TextView textView = new TextView(requireContext()); textView.setText(text); textView.setTextSize(size); textView.setTextColor(color); textView.setTypeface(null, style); return textView;
  }

  private void showEventPreview(@NonNull View root, @NonNull String name, @NonNull String venue, @NonNull String time,
                                @NonNull String price, @NonNull String travel, @NonNull String heat)
  {
    final FrameLayout layer = root.findViewById(R.id.events_preview_layer);
    layer.removeAllViews(); layer.setVisibility(View.VISIBLE); layer.setBackgroundColor(0xD9000000);
    layer.setOnClickListener(v -> layer.setVisibility(View.GONE));
    final LinearLayout sheet = new LinearLayout(requireContext()); sheet.setOrientation(LinearLayout.VERTICAL); sheet.setPadding(dp(20), dp(20), dp(20), dp(18)); sheet.setBackground(roundBackground(0xFF201B29, 28));
    final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER);
    params.setMargins(dp(18), dp(24), dp(18), dp(24)); layer.addView(sheet, params);
    sheet.setOnClickListener(v -> { });
    final TextView poster = eventText(name.toUpperCase() + "\n\n" + venue.toUpperCase(), 25, Color.WHITE, 1); poster.setGravity(android.view.Gravity.CENTER); poster.setBackground(roundBackground(0xFF5E256C, 20)); sheet.addView(poster, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(152)));
    final TextView title = eventText(name, 23, Color.WHITE, 1); title.setPadding(0, dp(17), 0, 0); sheet.addView(title);
    final TextView venueLine = eventText("⌖  " + venue + "  ✓", 14, 0xDFFFFFFF, 0); venueLine.setPadding(0, dp(5), 0, 0); sheet.addView(venueLine);
    final TextView facts = eventText(time + "\n" + travel + "   ·   " + price + "\n" + heat, 13, 0xCFFFFFFF, 0); facts.setLineSpacing(dp(5), 1); facts.setPadding(0, dp(14), 0, dp(14)); sheet.addView(facts);
    final LinearLayout actions = new LinearLayout(requireContext()); actions.setGravity(android.view.Gravity.CENTER_VERTICAL); actions.setOrientation(LinearLayout.HORIZONTAL); sheet.addView(actions);
    addPreviewAction(actions, "♡\nInterested", 0xFF302A39, false); addPreviewAction(actions, "◷\nPlan", 0xFF302A39, false); addPreviewAction(actions, "↗\nShare", 0xFF302A39, false);
    final TextView tickets = eventText("Tickets  →", 15, 0xFF18121E, 1); tickets.setGravity(android.view.Gravity.CENTER); tickets.setBackground(roundBackground(0xFFFFC36D, 16)); LinearLayout.LayoutParams ticketParams = new LinearLayout.LayoutParams(0, dp(48), 1); ticketParams.setMargins(dp(10), 0, 0, 0); actions.addView(tickets, ticketParams); tickets.setOnClickListener(v -> Toast.makeText(requireContext(), "Tickets for " + name, Toast.LENGTH_SHORT).show());
  }

  private void addPreviewAction(@NonNull LinearLayout parent, @NonNull String text, int color, boolean active)
  {
    final TextView action = eventText(text, 10, Color.WHITE, 0); action.setGravity(android.view.Gravity.CENTER); action.setBackground(roundBackground(color, 16)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(54), dp(48)); params.setMargins(0, 0, dp(7), 0); parent.addView(action, params); action.setOnClickListener(v -> { action.setText("✓\nSaved"); });
  }

  @NonNull
  private View createDiscoverAllScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @NonNull String title)
  {
    final View view = inflater.inflate(R.layout.main_screen, container, false);
    ((TextView) view.findViewById(R.id.main_screen_title)).setText(title);
    ((TextView) view.findViewById(R.id.main_screen_headline)).setText("Browse everything nearby");
    ((TextView) view.findViewById(R.id.main_screen_body)).setText("Search, filters, and the complete " + title + " experience are ready here.");
    return view;
  }

  @NonNull
  private View createCameraScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.camera_screen, container, false);
    view.findViewById(R.id.camera_close).setOnClickListener(v -> requireActivity().onBackPressed());
    view.findViewById(R.id.camera_studio).setOnClickListener(v -> toggleStudio(view));
    view.findViewById(R.id.camera_venue).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Truth Nightclub • venue details", Toast.LENGTH_SHORT).show());
    view.findViewById(R.id.camera_gallery).setOnClickListener(v -> openGallery());
    view.findViewById(R.id.camera_capture).setOnClickListener(v -> capturePhoto());
    view.findViewById(R.id.camera_dual_shot).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Dual Shot is ready on supported cameras", Toast.LENGTH_SHORT).show());

    mCameraPreviewEngine = new CameraPreviewEngine(requireActivity(), view.findViewById(R.id.camera_preview),
                                                    new CameraPreviewEngine.Listener()
    {
      @Override public void onPhotoCaptured(@NonNull File file)
      {
        Toast.makeText(requireContext(), "Moment captured and saved locally", Toast.LENGTH_SHORT).show();
      }

      @Override public void onCameraError(@NonNull String message)
      {
        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
      }
    });
    requestCameraAccess();

    final TextView[] modes = {view.findViewById(R.id.camera_photo), view.findViewById(R.id.camera_video),
                              view.findViewById(R.id.camera_live)};
    selectMode(modes, modes[0]);
    for (TextView mode : modes)
      mode.setOnClickListener(v -> selectMode(modes, (TextView) v));

    final LinearLayout looks = view.findViewById(R.id.camera_looks);
    for (int i = 0; i < looks.getChildCount(); ++i)
    {
      final View look = looks.getChildAt(i);
      look.setOnClickListener(v -> selectLook(looks, v));
      look.setOnLongClickListener(v -> {
        Toast.makeText(requireContext(), "Look intensity adjustment", Toast.LENGTH_SHORT).show();
        return true;
      });
    }
    return view;
  }

  private void toggleStudio(@NonNull View view)
  {
    final View studio = view.findViewById(R.id.camera_studio_sheet);
    studio.setVisibility(studio.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
  }

  private void selectMode(@NonNull TextView[] modes, @NonNull TextView selected)
  {
    for (TextView mode : modes)
      mode.setSelected(mode == selected);
  }

  private void selectLook(@NonNull LinearLayout looks, @NonNull View selected)
  {
    for (int i = 0; i < looks.getChildCount(); ++i)
    {
      final TextView look = (TextView) looks.getChildAt(i);
      final boolean isSelected = look == selected;
      look.setSelected(isSelected);
      look.setBackgroundResource(isSelected ? R.drawable.camera_look_selected : R.drawable.camera_look);
      look.setTextColor(isSelected ? 0xFF191423 : 0xFFFFFFFF);
    }
  }

  private void requestCameraAccess()
  {
    if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
      mCameraPreviewEngine.start();
    else
      requestPermissions(new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
  }

  private void capturePhoto()
  {
    if (requireContext().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
    {
      requestCameraAccess();
      return;
    }
    mCameraPreviewEngine.capture();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == CAMERA_PERMISSION_REQUEST && grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
      mCameraPreviewEngine.start();
    else if (requestCode == CAMERA_PERMISSION_REQUEST)
      Toast.makeText(requireContext(), "Camera permission is needed to capture moments", Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPause()
  {
    if (mCameraPreviewEngine != null)
      mCameraPreviewEngine.stop();
    super.onPause();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    if (mCameraPreviewEngine != null && requireContext().checkSelfPermission(Manifest.permission.CAMERA)
                                      == PackageManager.PERMISSION_GRANTED)
      mCameraPreviewEngine.start();
  }

  private void openGallery()
  {
    final Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("image/*");
    if (intent.resolveActivity(requireActivity().getPackageManager()) != null)
      startActivity(intent);
  }

  private void openCamera()
  {
    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (intent.resolveActivity(requireActivity().getPackageManager()) != null)
      startActivity(intent);
  }
}
