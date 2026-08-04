package app.organicmaps.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import java.io.File;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;

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
    if (destination.startsWith("discover_all:"))
      return createDiscoverAllScreen(inflater, container, destination.substring("discover_all:".length()));

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
