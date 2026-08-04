package app.organicmaps.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
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

    final View view = inflater.inflate(R.layout.main_screen, container, false);
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
    else
    {
      title.setText(R.string.main_tab_chats);
      headline.setText(R.string.chats_headline);
      body.setText(R.string.chats_body);
    }
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
