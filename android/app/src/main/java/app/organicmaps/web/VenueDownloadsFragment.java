package app.organicmaps.web;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.web.data.VenueDownload;
import app.organicmaps.web.data.VenueDownloadStore;

/** The Download Centre — tickets/receipts/menus captured during venue web sessions. */
public class VenueDownloadsFragment extends Fragment
{
  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    final View root = inflater.inflate(R.layout.venue_downloads_screen, container, false);
    root.findViewById(R.id.downloads_back).setOnClickListener(v -> getParentFragmentManager().popBackStack());

    final LinearLayout list = root.findViewById(R.id.downloads_list);
    final java.util.List<VenueDownload> items = VenueDownloadStore.get(requireContext()).listAll();
    if (items.isEmpty())
    {
      final TextView empty = new TextView(requireContext());
      empty.setText("🌙\n\nNo downloads yet.\nTickets, receipts, menus, and booking confirmations you collect from venue websites will appear here.");
      empty.setTextColor(0xB3FFFFFF);
      empty.setTextSize(14);
      empty.setGravity(Gravity.CENTER);
      empty.setLineSpacing(0, 1.3f);
      final LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      ep.topMargin = dp(60);
      empty.setPadding(dp(24), dp(32), dp(24), dp(32));
      empty.setLayoutParams(ep);
      list.addView(empty);
    }
    else
    {
      for (VenueDownload d : items)
        addRow(list, iconFor(d.kind), d.title, d.venueName);
    }
    return root;
  }

  private void addRow(@NonNull LinearLayout list, @NonNull String icon, @NonNull String title, @NonNull String sub)
  {
    final LinearLayout row = new LinearLayout(requireContext());
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(14), 0, dp(14), 0);
    row.setBackgroundResource(R.drawable.camera_venue_pill);
    final LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
    rp.bottomMargin = dp(10);
    row.setLayoutParams(rp);

    final TextView iconView = new TextView(requireContext());
    iconView.setText(icon);
    iconView.setTextSize(22);
    row.addView(iconView, new LinearLayout.LayoutParams(dp(44), dp(44)));

    final LinearLayout text = new LinearLayout(requireContext());
    text.setOrientation(LinearLayout.VERTICAL);
    final LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    tp.leftMargin = dp(12);
    final TextView titleView = new TextView(requireContext());
    titleView.setText(title);
    titleView.setTextColor(0xFFFFFFFF);
    titleView.setTextSize(14);
    titleView.setTypeface(null, Typeface.BOLD);
    text.addView(titleView);
    final TextView subView = new TextView(requireContext());
    subView.setText(sub);
    subView.setTextColor(0xB3FFFFFF);
    subView.setTextSize(11);
    text.addView(subView);
    row.addView(text, tp);

    final TextView open = new TextView(requireContext());
    open.setText("↗");
    open.setTextColor(0xFFFFFFFF);
    open.setTextSize(18);
    open.setGravity(Gravity.CENTER);
    row.addView(open, new LinearLayout.LayoutParams(dp(36), dp(36)));

    list.addView(row);
  }

  private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

  private static String iconFor(VenueDownload.Kind kind)
  {
    switch (kind)
    {
      case Ticket: return "🎟";
      case Receipt: return "🧾";
      case Invoice: return "🧾";
      case Menu: return "📖";
      case BookingConfirmation: return "✅";
      default: return "📄";
    }
  }
}
