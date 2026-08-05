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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView;
import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.discover.data.DiscoverHeroRepository;
import app.organicmaps.discover.data.OpenMeteoWeatherRepository;

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
    if (destination.startsWith("discover_all:"))
      return createDiscoverAllScreen(inflater, container, destination.substring("discover_all:".length()));
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
