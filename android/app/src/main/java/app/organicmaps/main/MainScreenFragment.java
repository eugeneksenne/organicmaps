package app.organicmaps.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.widget.HorizontalScrollView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.camera.data.CurrentVenue;
import app.organicmaps.camera.data.CurrentVenueDetector;
import app.organicmaps.camera.data.MomentPublisher;
import app.organicmaps.discover.data.DiscoverHeroRepository;
import app.organicmaps.discover.data.OpenMeteoWeatherRepository;
import app.organicmaps.fomo.data.FomoCatalogue;
import app.organicmaps.web.VenueWebFragment;
import app.organicmaps.web.data.WebVenue;

/** The non-map destinations in the app's primary navigation. */
public class MainScreenFragment extends Fragment
{
  public static final String ARG_DESTINATION = "destination";
  private static final int CAMERA_PERMISSION_REQUEST = 104;

  /**
   * Latest moment captured from the FOMO Camera. Stored statically so that once a photo is taken
   * it is immediately visible on the Feed screen (the app's first-class destination for captured
   * moments) even before any backend upload pipeline is in place.
   */
  @Nullable
  private static File sLatestCapturedMoment;
  @Nullable
  private static String sLatestMomentCaption;
  @Nullable
  private static CurrentVenue sLatestMomentVenue;

  @Nullable
  private CameraPreviewEngine mCameraPreviewEngine;
  @Nullable
  private EditText mCaptionEditor;
  @Nullable
  private View mCapturedPreviewCard;
  @Nullable
  private View mCameraCaptureView;
  @Nullable
  private TextView mCameraVenueLabel;
  @Nullable
  private View mCameraVenuePill;
  @Nullable
  private CurrentVenue mCurrentVenue;
  private final CurrentVenueDetector mVenueDetector = new CurrentVenueDetector();
  private final MomentPublisher mMomentPublisher = new MomentPublisher();
  private final Handler mUiHandler = new Handler(Looper.getMainLooper());

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
    if (destination.startsWith("channels"))
      return createChannelsScreen(inflater, container);
    if (destination.startsWith("closing_now"))
      return createClosingNowScreen(inflater, container);
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
    final View cameraEntry = view.findViewById(R.id.feed_camera);
    if (cameraEntry != null)
      cameraEntry.setOnClickListener(v -> openCameraFromFeed());
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

    // Bind the latest moment captured from the FOMO Camera so the Feed screen is the visual
    // destination for anything the user shoots. The caption field is editable from the camera
    // before posting, mirroring the Stories/Moments flow described in progress.md.
    bindLatestMoment(view);
    return view;
  }

  private void openCameraFromFeed()
  {
    if (requireActivity() instanceof app.organicmaps.MwmActivity)
      ((app.organicmaps.MwmActivity) requireActivity()).selectMainDestination("camera");
    else
      Toast.makeText(requireContext(), "Open camera to share a moment", Toast.LENGTH_SHORT).show();
  }

  private void bindLatestMoment(@NonNull View feedView)
  {
    final View latestCard = feedView.findViewById(R.id.feed_latest_moment);
    if (latestCard == null)
      return;
    if (sLatestCapturedMoment == null || !sLatestCapturedMoment.exists())
    {
      latestCard.setVisibility(View.GONE);
      return;
    }
    latestCard.setVisibility(View.VISIBLE);
    final ImageView thumbnail = latestCard.findViewById(R.id.feed_latest_thumbnail);
    if (thumbnail != null)
      thumbnail.setImageBitmap(BitmapFactory.decodeFile(sLatestCapturedMoment.getAbsolutePath()));
    final TextView caption = latestCard.findViewById(R.id.feed_latest_caption);
    if (caption != null)
    {
      final String venueTag = sLatestMomentVenue != null && sLatestMomentVenue.isDetected()
          ? "📍 " + sLatestMomentVenue.name + "  •  "
          : "";
      final String text = sLatestMomentCaption == null || sLatestMomentCaption.isEmpty()
          ? venueTag + "just now"
          : sLatestMomentCaption + "  •  " + venueTag + "just now";
      caption.setText(text);
    }
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
    addDiscoverSection(sections, "Flash Drops", new String[] {"Free welcome drink • 12 left", "R100 ride credit • 8 left", "Guest-list upgrade • 5 left"}, 0xFFF8D8E8);    addDiscoverSection(sections, "My Circle", new String[] {"Nomsa • 3 mutual friends", "Kagiso • Same events", "Lerato • 1.2 km away"}, 0xFFDCE8FA);
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
    // The "Channels" section is a full premium experience rather than a stub "see all".
    if ("Channels".equals(title))
    {
      getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("channels"))
          .addToBackStack("channels").commit();
      return;
    }
    // "Closing Soon" has its own live countdown see-all screen.
    if ("Closing Soon".equals(title))
    {
      getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("closing_now"))
          .addToBackStack("closing_now").commit();
      return;
    }
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
  private View createChannelsScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.channels_screen, container, false);
    final FomoCatalogue cat = FomoCatalogue.get();
    view.findViewById(R.id.channels_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.channels_search).setOnClickListener(v -> openSearchChannels());
    view.findViewById(R.id.channels_notify).setOnClickListener(v -> openNotifications());
    view.findViewById(R.id.channels_profile).setOnClickListener(v -> {
      getParentFragmentManager().beginTransaction().replace(R.id.main_screen_container, newInstance("profile:me"))
          .addToBackStack("profile").commit();
    });
    view.findViewById(R.id.channels_switcher).setOnClickListener(v -> showChannelSwitcher());
    view.findViewById(R.id.channels_hero_cta).setOnClickListener(v -> openTonightTimeline());

    view.findViewById(R.id.channels_action_live).setOnClickListener(v -> selectChannelTab(
        new TextView[] {view.findViewById(R.id.channels_tab_overview), view.findViewById(R.id.channels_tab_feed),
                        view.findViewById(R.id.channels_tab_live), view.findViewById(R.id.channels_tab_plans),
                        view.findViewById(R.id.channels_tab_polls)},
        2, view.findViewById(R.id.channels_tab_content)));
    view.findViewById(R.id.channels_action_events).setOnClickListener(v -> openDiscoverAll("Events"));
    view.findViewById(R.id.channels_action_drops).setOnClickListener(v -> openDiscoverAll("Flash Drops"));
    view.findViewById(R.id.channels_action_venues).setOnClickListener(v -> openDiscoverAll("Smart Places"));

    // Hero meta aggregates live stats from the catalogue.
    int liveCount = 0;
    for (FomoCatalogue.Venue v : cat.venues) if (v.liveNow) liveCount++;
    final TextView heroHeadline = view.findViewById(R.id.channels_hero_headline);
    heroHeadline.setText(liveCount >= 3 ? "Peak nightlife has just begun." : "The night is warming up.");
    final TextView heroMeta = view.findViewById(R.id.channels_hero_meta);
    heroMeta.setText(liveCount + " venues LIVE  •  Updated now");

    // AI Night Brief built from venue briefLine entries.
    final StringBuilder brief = new StringBuilder();
    for (FomoCatalogue.NightBriefItem item : cat.nightBrief)
      brief.append("• ").append(item.line).append("\n");
    final TextView briefView = view.findViewById(R.id.channels_brief);
    if (briefView != null) briefView.setText(brief.toString().trim());

    // Flash drop (uses the current live flash drop if any; hidden otherwise).
    final View flashCard = view.findViewById(R.id.channels_flash_drop);
    if (cat.flashDrops.isEmpty())
    {
      flashCard.setVisibility(View.GONE);
    }
    else
    {
      final FomoCatalogue.FlashDrop drop = cat.flashDrops.get(0);
      final FomoCatalogue.Venue venue = cat.venueById(drop.venueId);
      ((TextView) flashCard.findViewById(R.id.channels_flash_text))
          .setText(drop.title + "  •  " + drop.venueName);
      final long remaining = Math.max(1, (drop.endsAtMs - System.currentTimeMillis()) / 60_000L);
      ((TextView) flashCard.findViewById(R.id.channels_flash_timer))
          .setText("Ends in " + remaining + " min");
      flashCard.setOnClickListener(v -> {
        if (venue != null) openVenueWebsite(venue.name);
      });
    }

    // Trending venues — up to 4 cards, drawn from catalogue.trendingTonight().
    final int[] trendIds = {R.id.channels_trend_konka, R.id.channels_trend_madison,
                            R.id.channels_trend_booth, R.id.channels_trend_ayepyep};
    final java.util.List<FomoCatalogue.Venue> trending = cat.trendingTonight();
    final int friendsHere = Math.max(1, cat.friends.size() / 2);
    for (int i = 0; i < trendIds.length; ++i)
    {
      final View card = view.findViewById(trendIds[i]);
      if (i < trending.size())
      {
        final FomoCatalogue.Venue v = trending.get(i);
        card.setVisibility(View.VISIBLE);
        bindTrendingCard(view, trendIds[i], v.name, stateLabel(v.state), v.energyPercent + "%",
            String.format(java.util.Locale.US, "%.1f km  •  %d friends here", v.distanceKm, friendsHere), v.liveNow);
      }
      else card.setVisibility(View.GONE);
    }

    // Live streams.
    final int[] liveIds = {R.id.channels_live_konka, R.id.channels_live_cocoon, R.id.channels_live_vault};
    for (int i = 0; i < liveIds.length; ++i)
    {
      final View card = view.findViewById(liveIds[i]);
      if (i < cat.liveStreams.size())
      {
        final FomoCatalogue.LiveStream s = cat.liveStreams.get(i);
        card.setVisibility(View.VISIBLE);
        bindLiveCard(view, liveIds[i], s.venueName, s.area, formatViewers(s.viewers), s.energy + "%");
      }
      else card.setVisibility(View.GONE);
    }

    // Friend chips — render as many catalogue friends as we have chips.
    final LinearLayout friendsRow = view.findViewById(R.id.channels_friends);
    for (int i = 0; i < friendsRow.getChildCount(); ++i)
    {
      final View chip = friendsRow.getChildAt(i);
      if (i < cat.friends.size())
      {
        chip.setVisibility(View.VISIBLE);
        final FomoCatalogue.FriendMarker f = cat.friends.get(i);
        ((TextView) chip.findViewById(R.id.friend_avatar)).setText(f.initial);
        ((TextView) chip.findViewById(R.id.friend_name)).setText(f.name);
        ((TextView) chip.findViewById(R.id.friend_status)).setText(f.status);
      }
      else chip.setVisibility(View.GONE);
    }

    final TextView[] channelTabs = {
        view.findViewById(R.id.channels_tab_overview), view.findViewById(R.id.channels_tab_feed),
        view.findViewById(R.id.channels_tab_live), view.findViewById(R.id.channels_tab_plans),
        view.findViewById(R.id.channels_tab_polls)
    };
    final View tabContent = view.findViewById(R.id.channels_tab_content);
    selectChannelTab(channelTabs, 0, tabContent);
    for (int i = 0; i < channelTabs.length; ++i)
    {
      final int idx = i;
      channelTabs[i].setOnClickListener(v -> selectChannelTab(channelTabs, idx, tabContent));
    }
    return view;
  }

  @NonNull
  private static String stateLabel(@NonNull FomoCatalogue.VenueState s)
  {
    switch (s)
    {
      case PEAK: return "Peak Energy";
      case STEADY: return "Steady";
      case GAINING: return "Gaining";
      case WARMUP: return "Warm-up";
      case LIVE: return "LIVE";
      case LAST_CALL: return "Last Call";
      default: return "";
    }
  }

  @NonNull
  private static String formatViewers(int n)
  {
    if (n >= 1000) return String.format(java.util.Locale.US, "%.1fK watching", n / 1000.0);
    return n + " watching";
  }

  private void openSearchChannels() { openDiscoverAll("Channels"); }
  private void openNotifications() { showConversationNotice("Notifications for channels are coming online with the push gateway."); }
  private void openTonightTimeline() { openDiscoverAll("Tonight"); }

  private void selectChannelTab(@NonNull TextView[] tabs, int selected, @NonNull View content)
  {
    for (int i = 0; i < tabs.length; ++i)
    {
      final boolean active = i == selected;
      tabs[i].setSelected(active);
      tabs[i].setBackgroundResource(active ? R.drawable.feed_glass_pill : 0);
      tabs[i].setTextColor(active ? Color.WHITE : 0xB3FFFFFF);
      tabs[i].setTypeface(null, active ? 1 : 0);
    }
    final FomoCatalogue cat = FomoCatalogue.get();
    content.removeAllViews();
    final LinearLayout stack = new LinearLayout(requireContext());
    stack.setOrientation(LinearLayout.VERTICAL);

    final String header;
    final java.util.List<FomoCatalogue.Venue> items;
    switch (selected)
    {
      case 1:
        header = "Realtime updates — prioritized by distance, momentum, friends, venue relevance, recency.";
        items = new ArrayList<>(cat.trendingTonight());
        break;
      case 2:
        header = "Livestreams sorted by nearby, trending, highest energy, friends watching.";
        items = new ArrayList<>();
        for (FomoCatalogue.LiveStream s : cat.liveStreams)
        {
          final FomoCatalogue.Venue v = cat.venueById(s.venueId);
          if (v != null) items.add(v);
        }
        break;
      case 3:
        header = "Active plans for tonight.";
        items = new ArrayList<>();
        break;
      case 4:
        header = "Visual polls across venues, events, outfits, drinks, and afterparties.";
        items = new ArrayList<>();
        break;
      default:
        header = "Curated overview — trending venues, flash drops, and friend activity.";
        items = new ArrayList<>(cat.trendingTonight());
        break;
    }
    final TextView head = new TextView(requireContext());
    head.setText(header);
    head.setTextColor(0xB3FFFFFF);
    head.setTextSize(12);
    head.setPadding(dp(16), dp(4), dp(16), dp(12));
    stack.addView(head);
    for (FomoCatalogue.Venue v : items)
    {
      final TextView row = new TextView(requireContext());
      row.setText("● " + v.name + "  •  " + v.category + "  •  " + v.energyPercent + "% energy");
      row.setTextColor(0xFFFFFFFF);
      row.setTextSize(14);
      row.setPadding(dp(16), dp(10), dp(16), dp(10));
      row.setBackgroundResource(R.drawable.camera_look);
      final LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      rp.bottomMargin = dp(8);
      row.setLayoutParams(rp);
      row.setOnClickListener(ev -> openVenueWebsite(v.name));
      stack.addView(row);
    }
    final TextView padding = new TextView(requireContext());
    padding.setHeight(dp(8));
    stack.addView(padding);
    content.addView(stack);
  }

  private void bindTrendingCard(@NonNull View root, int id, @NonNull String venue, @NonNull String status,
                                @NonNull String energy, @NonNull String meta, boolean live)
  {
    final View card = root.findViewById(id);
    ((TextView) card.findViewById(R.id.trend_venue)).setText(venue);
    ((TextView) card.findViewById(R.id.trend_status)).setText(status);
    ((TextView) card.findViewById(R.id.trend_energy)).setText(energy);
    ((TextView) card.findViewById(R.id.trend_meta)).setText(meta);
    final View badge = card.findViewById(R.id.trend_live_badge);
    badge.setVisibility(live ? View.VISIBLE : View.GONE);
    card.setOnClickListener(v -> openVenueWebsite(venue));
  }

  private void bindLiveCard(@NonNull View root, int id, @NonNull String venue, @NonNull String area,
                            @NonNull String viewers, @NonNull String energy)
  {
    final View card = root.findViewById(id);
    ((TextView) card.findViewById(R.id.live_venue)).setText(venue);
    ((TextView) card.findViewById(R.id.live_area)).setText(area);
    ((TextView) card.findViewById(R.id.live_viewers)).setText(viewers);
    ((TextView) card.findViewById(R.id.live_energy)).setText(energy);
    card.setOnClickListener(v -> {
      final FomoCatalogue.Venue v = FomoCatalogue.get().venueByName(venue);
      if (v != null && v.websiteUrl != null)
        openVenueWebsite(v.name);
      else
        Toast.makeText(requireContext(), "Joining " + venue + " live", Toast.LENGTH_SHORT).show();
    });
  }

  private void bindFriendChip(@NonNull View root, int index, @NonNull String initial, @NonNull String name,
                              @NonNull String status)
  {
    final LinearLayout row = root.findViewById(R.id.channels_friends);
    if (row == null || index >= row.getChildCount()) return;
    final View chip = row.getChildAt(index);
    ((TextView) chip.findViewById(R.id.friend_avatar)).setText(initial);
    ((TextView) chip.findViewById(R.id.friend_name)).setText(name);
    ((TextView) chip.findViewById(R.id.friend_status)).setText(status);
  }

  private void showChannelSwitcher()
  {
    final PopupMenu menu = new PopupMenu(requireContext(), requireView().findViewById(R.id.channels_switcher));
    for (String item : new String[] {
        "📍 Johannesburg Nights ✓", "Province Channels", "City Channels", "Club Channels",
        "Pinned Channels", "Recent Channels", "Search channels…"})
      menu.getMenu().add(item);
    menu.setOnMenuItemClickListener(item -> {
      if (item.getTitle() != null && !item.getTitle().toString().contains("Johannesburg"))
        Toast.makeText(requireContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
      return true;
    });
    menu.show();
  }

  @NonNull
  private View createClosingNowScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.closing_now_screen, container, false);
    final FomoCatalogue cat = FomoCatalogue.get();
    view.findViewById(R.id.closing_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());
    view.findViewById(R.id.closing_filter).setOnClickListener(v -> showConversationNotice("Filter closing-soon venues"));
    view.findViewById(R.id.closing_sort).setOnClickListener(v -> showClosingSortMenu(v));

    final List<FomoCatalogue.Venue> closing = cat.closingSoon(60);
    int lastOrders = 0, busy = 0, nearby = 0;
    for (FomoCatalogue.Venue v : closing)
    {
      if (v.state == FomoCatalogue.VenueState.LAST_CALL) lastOrders++;
      if (v.energyPercent >= 80) busy++;
      if (v.distanceKm <= 3.0) nearby++;
    }
    bindStat(view, R.id.closing_stat_closing, String.valueOf(closing.size()), "Closing Soon");
    bindStat(view, R.id.closing_stat_last_orders, String.valueOf(lastOrders), "Last Orders");
    bindStat(view, R.id.closing_stat_busy, String.valueOf(busy), "Very Busy");
    bindStat(view, R.id.closing_stat_nearby, String.valueOf(nearby), "Within 3 km");

    final TextView header = view.findViewById(R.id.closing_header);
    final TextView subtitle = view.findViewById(R.id.closing_subtitle);
    final TextView dynamic = view.findViewById(R.id.closing_dynamic_line);
    header.setText("Closing Soon");
    subtitle.setText(closing.size() + " places closing within the next hour.");
    if (lastOrders > 0)
      dynamic.setText("Kitchen rush is ending.");
    else
      dynamic.setText("The night is still warming up.");

    bindFilterGroup(view, R.id.closing_category_filters, v -> applyClosingFilter(view));
    bindFilterGroup(view, R.id.closing_time_filters, v -> applyClosingFilter(view));
    bindFilterGroup(view, R.id.closing_distance_filters, v -> applyClosingFilter(view));

    populateClosingFeed(view, closing);

    view.findViewById(R.id.closing_bottom_action).setOnClickListener(v -> {
      if (!closing.isEmpty()) navigateToVenue(closing.get(0));
    });
    final TextView ctaText = view.findViewById(R.id.closing_bottom_text);
    if (!closing.isEmpty())
    {
      final FomoCatalogue.Venue best = closing.get(0);
      final long etaMin = Math.max(1, Math.round(best.distanceKm * 3.5));
      ctaText.setText("Leave now. You'll arrive at " + best.name + " in " + etaMin + " minutes.");
    }
    else
    {
      ctaText.setText("The night isn't ending yet — explore what's still open.");
    }
    return view;
  }

  private void applyClosingFilter(@NonNull View root) { populateClosingFeed(root, FomoCatalogue.get().closingSoon(60)); }

  private void populateClosingFeed(@NonNull View root, @NonNull List<FomoCatalogue.Venue> venues)
  {
    final LinearLayout feed = root.findViewById(R.id.closing_feed);
    for (int i = 0; i < feed.getChildCount(); ++i)
    {
      final View card = feed.getChildAt(i);
      if (i < venues.size())
      {
        card.setVisibility(View.VISIBLE);
        bindClosingCard(card, venues.get(i));
      }
      else card.setVisibility(View.GONE);
    }
  }

  private void bindStat(@NonNull View root, int id, @NonNull String number, @NonNull String label)
  {
    final View card = root.findViewById(id);
    ((TextView) card.findViewById(R.id.stat_number)).setText(number);
    ((TextView) card.findViewById(R.id.stat_label)).setText(label);
  }

  private void bindFilterGroup(@NonNull View root, int groupId, @NonNull View.OnClickListener onClick)
  {
    final ViewGroup group = root.findViewById(groupId);
    for (int i = 0; i < group.getChildCount(); ++i)
    {
      final View child = group.getChildAt(i);
      child.setOnClickListener(v -> {
        for (int j = 0; j < group.getChildCount(); ++j)
        {
          final View c = group.getChildAt(j);
          final boolean selected = c == v;
          c.setSelected(selected);
          if (group.getId() == R.id.closing_category_filters)
            c.setBackgroundResource(selected ? R.drawable.feed_glass_pill : R.drawable.camera_look);
          else
            c.setBackgroundResource(selected ? R.drawable.feed_glass_pill : 0);
          ((TextView) c).setTextColor(selected ? Color.WHITE : 0xB3FFFFFF);
          ((TextView) c).setTypeface(null, selected ? 1 : 0);
        }
        onClick.onClick(v);
      });
    }
  }

  private void showClosingSortMenu(@NonNull View anchor)
  {
    final PopupMenu menu = new PopupMenu(requireContext(), anchor);
    for (String item : new String[] {"Most Urgent", "Closest", "Most Popular", "Best Rated", "Recommended", "Fastest to Reach"})
      menu.getMenu().add(item);
    menu.setOnMenuItemClickListener(item -> {
      Toast.makeText(requireContext(), "Sorted by " + item.getTitle(), Toast.LENGTH_SHORT).show();
      return true;
    });
    menu.show();
  }

  private void bindClosingCard(@NonNull View card, @NonNull FomoCatalogue.Venue venue)
  {
    final long remaining = venue.minutesRemaining();
    final String badge = venue.liveBadge();
    final long etaMin = Math.max(1, Math.round(venue.distanceKm * 3.5));
    final String meta = String.format(Locale.US, "%s • %.1f km  •  ★ %.1f", venue.category, venue.distanceKm, venue.rating);
    ((TextView) card.findViewById(R.id.venue_name)).setText(venue.name);
    ((TextView) card.findViewById(R.id.venue_badge)).setText(badge);
    ((TextView) card.findViewById(R.id.venue_meta)).setText(meta);
    ((TextView) card.findViewById(R.id.venue_address)).setText(venue.address);
    ((TextView) card.findViewById(R.id.venue_smart_message)).setText(venue.smartClosingMessage);
    final boolean inTime = etaMin <= remaining;
    ((TextView) card.findViewById(R.id.venue_eta)).setText(
        String.format(Locale.US, "🚗 %d min away  •  %s", etaMin,
                      inTime ? "You'll arrive before closing." : "You'll arrive after closing."));
    final int friendCount = Math.max(1, (int) Math.round(venue.energyPercent / 12.0));
    ((TextView) card.findViewById(R.id.venue_social)).setText(friendCount + " heading there");
    ((TextView) card.findViewById(R.id.venue_countdown)).setText(remaining + "\nMIN");
    card.findViewById(R.id.venue_countdown_ring).getBackground().setTint(venue.ringColor());
    card.findViewById(R.id.venue_cta_directions).setOnClickListener(v -> navigateToVenue(venue));
    card.findViewById(R.id.venue_cta_save).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Saved " + venue.name, Toast.LENGTH_SHORT).show());
    card.findViewById(R.id.venue_cta_share).setOnClickListener(v -> shareVenue(venue));
    card.findViewById(R.id.venue_cta_website).setOnClickListener(v -> openVenueWebsite(venue.name));
  }

  private void navigateToVenue(@NonNull FomoCatalogue.Venue venue)
  {
    Toast.makeText(requireContext(), "Route to " + venue.name, Toast.LENGTH_SHORT).show();
  }

  private void shareVenue(@NonNull FomoCatalogue.Venue venue)
  {
    final Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("text/plain");
    final String url = venue.websiteUrl != null ? venue.websiteUrl : "";
    share.putExtra(Intent.EXTRA_TEXT, venue.name + " — " + venue.address + " " + url);
    startActivity(Intent.createChooser(share, "Share " + venue.name));
  }

  @NonNull
  private View createCameraScreen(@NonNull LayoutInflater inflater, @Nullable ViewGroup container)
  {
    final View view = inflater.inflate(R.layout.camera_screen, container, false);
    view.findViewById(R.id.camera_close).setOnClickListener(v -> {
      // If a capture preview is open, discard it and return to the live camera instead of
      // navigating away from the camera tab entirely.
      if (sLatestCapturedMoment != null)
      {
        discardCapturedMoment();
        return;
      }
      requireActivity().onBackPressed();
    });
    view.findViewById(R.id.camera_studio).setOnClickListener(v -> toggleStudio(view));
    view.findViewById(R.id.camera_gallery).setOnClickListener(v -> openGallery());
    mCameraCaptureView = view.findViewById(R.id.camera_capture);
    mCameraCaptureView.setOnClickListener(v -> capturePhoto());
    view.findViewById(R.id.camera_dual_shot).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Dual Shot is ready on supported cameras", Toast.LENGTH_SHORT).show());

    mCameraVenuePill = view.findViewById(R.id.camera_venue);
    mCameraVenueLabel = mCameraVenuePill.findViewById(R.id.camera_venue_name);
    mCameraVenuePill.setOnClickListener(v -> showVenueSheet());
    refreshCurrentVenue();

    mCapturedPreviewCard = view.findViewById(R.id.camera_captured_card);
    mCaptionEditor = view.findViewById(R.id.camera_caption);
    view.findViewById(R.id.camera_retake).setOnClickListener(v -> discardCapturedMoment());
    view.findViewById(R.id.camera_post_to_feed).setOnClickListener(v -> publishCapturedToFeed());

    // If a capture is already pending (e.g. the user rotated mid-edit) rebuild the preview card state.
    if (sLatestCapturedMoment != null && !sLatestCapturedMoment.exists())
      sLatestCapturedMoment = null;

    mCameraPreviewEngine = new CameraPreviewEngine(requireActivity(), view.findViewById(R.id.camera_preview),
                                                    new CameraPreviewEngine.Listener()
    {
      @Override public void onPhotoCaptured(@NonNull File file)
      {
        if (!isAdded())
          return;
        requireActivity().runOnUiThread(() -> showCapturedMoment(file));
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

  private void showCapturedMoment(@NonNull File file)
  {
    sLatestCapturedMoment = file;
    if (sLatestMomentCaption == null)
      sLatestMomentCaption = "";
    if (mCapturedPreviewCard == null)
      return;
    mCapturedPreviewCard.setVisibility(View.VISIBLE);
    if (mCameraCaptureView != null)
      mCameraCaptureView.setEnabled(false);
    final ImageView thumbnail = mCapturedPreviewCard.findViewById(R.id.camera_captured_thumbnail);
    if (thumbnail != null)
      thumbnail.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
    Toast.makeText(requireContext(), "Moment captured — add a caption and post to Feed", Toast.LENGTH_SHORT).show();
  }

  private void discardCapturedMoment()
  {
    sLatestCapturedMoment = null;
    sLatestMomentCaption = null;
    sLatestMomentVenue = null;
    if (mCaptionEditor != null)
      mCaptionEditor.setText("");
    if (mCapturedPreviewCard != null)
      mCapturedPreviewCard.setVisibility(View.GONE);
    if (mCameraCaptureView != null)
      mCameraCaptureView.setEnabled(true);
  }

  private void publishCapturedToFeed()
  {
    if (sLatestCapturedMoment == null)
    {
      Toast.makeText(requireContext(), "Capture a moment before posting", Toast.LENGTH_SHORT).show();
      return;
    }
    if (mCaptionEditor != null)
      sLatestMomentCaption = mCaptionEditor.getText().toString().trim();
    sLatestMomentVenue = mCurrentVenue;
    final Location location = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    final double lat = location != null ? location.getLatitude() : 0;
    final double lon = location != null ? location.getLongitude() : 0;
    final MomentPublisher.CaptionedMoment payload = new MomentPublisher.CaptionedMoment(
        mCurrentVenue != null ? mCurrentVenue.venueId : null, sLatestMomentCaption, lat, lon);

    Toast.makeText(requireContext(), "Posting to Feed…", Toast.LENGTH_SHORT).show();
    mMomentPublisher.publish(sLatestCapturedMoment, payload, new MomentPublisher.Callback()
    {
      @Override public void onPublished(@NonNull String mediaPath, @Nullable String momentId)
      {
        if (!isAdded()) return;
        mUiHandler.post(() -> {
          Toast.makeText(requireContext(),
              mCurrentVenue != null && mCurrentVenue.isDetected()
                  ? "Posted to Feed • tagged " + mCurrentVenue.name
                  : "Posted to Feed",
              Toast.LENGTH_SHORT).show();
          if (mCaptionEditor != null) mCaptionEditor.setText("");
          if (mCapturedPreviewCard != null) mCapturedPreviewCard.setVisibility(View.GONE);
          if (mCameraCaptureView != null) mCameraCaptureView.setEnabled(true);
          // Navigate to the Feed so the user sees the new moment in the Nearby/Following tabs.
          if (requireActivity() instanceof app.organicmaps.MwmActivity)
            ((app.organicmaps.MwmActivity) requireActivity()).selectMainDestination("feed");
        });
      }
      @Override public void onPublishFailed(@NonNull String reason)
      {
        if (!isAdded()) return;
        mUiHandler.post(() -> Toast.makeText(requireContext(),
            "Could not post moment: " + reason, Toast.LENGTH_SHORT).show());
      }
    });
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
    if (mCameraVenuePill != null)
      refreshCurrentVenue();
  }

  private void refreshCurrentVenue()
  {
    setVenueUi(CurrentVenue.NONE);
    mVenueDetector.detect(requireContext(), venue -> {
      if (!isAdded()) return;
      mUiHandler.post(() -> setVenueUi(venue));
    });
  }

  private void setVenueUi(@NonNull CurrentVenue venue)
  {
    mCurrentVenue = venue;
    if (mCameraVenueLabel == null) return;
    mCameraVenueLabel.setText(venue.isDetected() ? venue.name : "Share where you are");
    final View dot = mCameraVenuePill != null ? mCameraVenuePill.findViewById(R.id.camera_venue_dot) : null;
    if (dot != null)
    {
      dot.setVisibility(venue.isDetected() ? View.VISIBLE : View.GONE);
      if (venue.isDetected())
        dot.animate().alpha(venue.liveNow ? 1f : 0.55f).setDuration(200);
    }
  }

  private void showVenueSheet()
  {
    if (mCurrentVenue != null && mCurrentVenue.isDetected())
      Toast.makeText(requireContext(),
          mCurrentVenue.name + (mCurrentVenue.liveNow ? " • LIVE now" : "") + " — this moment will be tagged on the Feed",
          Toast.LENGTH_SHORT).show();
    else
      Toast.makeText(requireContext(), "Move closer to a venue to tag your moment", Toast.LENGTH_SHORT).show();
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

  private void openVenueWebsite(@NonNull String venueName)
  {
    final FomoCatalogue.Venue venue = FomoCatalogue.get().venueByName(venueName);
    if (venue == null || venue.websiteUrl == null)
    {
      Toast.makeText(requireContext(), venueName + " has no website on record", Toast.LENGTH_SHORT).show();
      return;
    }
    final WebVenue web = new WebVenue(venue.id, venue.name, venue.websiteUrl, venue.phone,
                                      venue.latitude, venue.longitude);
    getParentFragmentManager().beginTransaction()
        .replace(R.id.main_screen_container, VenueWebFragment.open(web))
        .addToBackStack("venue_web").commit();
  }
}
