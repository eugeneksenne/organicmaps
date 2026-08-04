package app.organicmaps.main;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;

/** The non-map destinations in the app's primary navigation. */
public class MainScreenFragment extends Fragment
{
  public static final String ARG_DESTINATION = "destination";

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
    final View view = inflater.inflate(R.layout.main_screen, container, false);
    final String destination = requireArguments().getString(ARG_DESTINATION);
    final TextView title = view.findViewById(R.id.main_screen_title);
    final TextView headline = view.findViewById(R.id.main_screen_headline);
    final TextView body = view.findViewById(R.id.main_screen_body);
    final View action = view.findViewById(R.id.main_screen_action);

    if ("discover".equals(destination))
    {
      title.setText(R.string.main_tab_discover);
      headline.setText(R.string.discover_headline);
      body.setText(R.string.discover_body);
    }
    else if ("feed".equals(destination))
    {
      title.setText(R.string.main_tab_feed);
      headline.setText(R.string.feed_headline);
      body.setText(R.string.feed_body);
    }
    else if ("camera".equals(destination))
    {
      title.setText(R.string.main_tab_camera);
      headline.setText(R.string.camera_headline);
      body.setText(R.string.camera_body);
      action.setVisibility(View.VISIBLE);
      ((TextView) action).setText(R.string.camera_action);
      action.setOnClickListener(v -> openCamera());
    }
    else
    {
      title.setText(R.string.main_tab_chats);
      headline.setText(R.string.chats_headline);
      body.setText(R.string.chats_body);
    }
    return view;
  }

  private void openCamera()
  {
    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (intent.resolveActivity(requireActivity().getPackageManager()) != null)
      startActivity(intent);
  }
}
