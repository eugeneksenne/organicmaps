package app.organicmaps.widget.placepage.sections;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import app.organicmaps.R;
import app.organicmaps.widget.placepage.PlacePageViewModel;
import app.organicmaps.widget.placepage.online.OnlinePlaceDetails;
import app.organicmaps.util.Utils;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PlacePageOnlineFragment extends Fragment implements Observer<OnlinePlaceDetails>
{
  private View mRoot;
  private LinearLayout mPhotos;
  private TextView mSocial;
  private final ExecutorService mExecutor = Executors.newFixedThreadPool(3);

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state)
  { return inflater.inflate(R.layout.place_page_online_fragment, parent, false); }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    mRoot = view; mPhotos = view.findViewById(R.id.online_photos); mSocial = view.findViewById(R.id.online_social);
    PlacePageViewModel vm = new ViewModelProvider(requireActivity()).get(PlacePageViewModel.class);
    vm.getOnlinePlaceDetails().observe(getViewLifecycleOwner(), this);
  }

  @Override public void onChanged(@Nullable OnlinePlaceDetails details)
  {
    if (details == null || (details.photoUrls.isEmpty() && details.socialLinks.isEmpty())) { mRoot.setVisibility(View.GONE); return; }
    mRoot.setVisibility(View.VISIBLE); mPhotos.removeAllViews();
    for (String url : details.photoUrls) loadPhoto(url);
    StringBuilder social = new StringBuilder();
    for (Map.Entry<String, String> entry : details.socialLinks.entrySet())
    { if (social.length() > 0) social.append("  •  "); social.append(entry.getKey()).append(": ").append(entry.getValue()); }
    mSocial.setText(social.toString());
    mSocial.setOnClickListener(v -> { if (details.sourceUrl != null) Utils.openUrl(requireContext(), details.sourceUrl); });
  }

  private void loadPhoto(String url)
  {
    ImageView image = new ImageView(requireContext()); image.setBackgroundColor(Color.LTGRAY); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    mPhotos.addView(image, new LinearLayout.LayoutParams(0, 120, 1));
    image.setOnClickListener(v -> Utils.openUrl(requireContext(), url));
    mExecutor.execute(() -> { try { final var bitmap = BitmapFactory.decodeStream(new URL(url).openStream()); if (bitmap != null) requireActivity().runOnUiThread(() -> image.setImageBitmap(bitmap)); } catch (IOException | IllegalStateException ignored) {} });
  }

  @Override public void onDestroyView() { mExecutor.shutdownNow(); super.onDestroyView(); }
}
