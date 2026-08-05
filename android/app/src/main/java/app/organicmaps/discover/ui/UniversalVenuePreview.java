package app.organicmaps.discover.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

/** Shared two-stage venue interaction for all FOMO discovery surfaces. */
public final class UniversalVenuePreview
{
  public static final class Venue
  {
    @NonNull public final String name, suburb, category, rating, address, status, distance, hours, tags;
    public final boolean verified;
    public Venue(@NonNull String name, @NonNull String suburb, @NonNull String category, @NonNull String rating,
                 @NonNull String address, @NonNull String status, @NonNull String distance, @NonNull String hours,
                 @NonNull String tags, boolean verified)
    { this.name = name; this.suburb = suburb; this.category = category; this.rating = rating; this.address = address; this.status = status; this.distance = distance; this.hours = hours; this.tags = tags; this.verified = verified; }
  }
  private UniversalVenuePreview() {}

  public static void show(@NonNull ViewGroup root, @NonNull Venue venue)
  {
    final Context context = root.getContext();
    final FrameLayout scrim = new FrameLayout(context); scrim.setBackgroundColor(0xD9000000); scrim.setAlpha(0f);
    root.addView(scrim, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    scrim.animate().alpha(1f).setDuration(180).start(); scrim.setOnClickListener(v -> dismiss(root, scrim));
    final LinearLayout sheet = new LinearLayout(context); sheet.setOrientation(LinearLayout.VERTICAL); sheet.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 18)); sheet.setBackground(round(context, 0xFF201B29, 28)); sheet.setScaleX(.94f); sheet.setScaleY(.94f); sheet.setAlpha(0f); sheet.setOnClickListener(v -> { });
    final FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER); sheetParams.setMargins(dp(context, 18), dp(context, 26), dp(context, 18), dp(context, 26)); scrim.addView(sheet, sheetParams);
    final TextView hero = text(context, venue.name.toUpperCase() + "\n" + venue.suburb.toUpperCase(), 25, Color.WHITE, 1); hero.setGravity(Gravity.CENTER); hero.setBackground(round(context, heroColor(venue.category), 20)); sheet.addView(hero, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 145)));
    final LinearLayout titleRow = new LinearLayout(context); titleRow.setGravity(Gravity.CENTER_VERTICAL); titleRow.setPadding(0, dp(context, 15), 0, 0); sheet.addView(titleRow);
    final TextView title = text(context, venue.name + (venue.verified ? "  ✓" : ""), 23, Color.WHITE, 1); titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(context, 34), 1));
    final TextView heart = text(context, "♡", 27, 0xFFFF7BCB, 0); heart.setGravity(Gravity.CENTER); heart.setBackground(round(context, 0xFF302A39, 17)); titleRow.addView(heart, new LinearLayout.LayoutParams(dp(context, 42), dp(context, 34))); heart.setOnClickListener(v -> heart.setText("♥"));
    line(sheet, "●  " + venue.status + "   ·   " + venue.distance + " away", 13, 0xFF8CEBB8, 0);
    line(sheet, "★ " + venue.rating + "   ·   " + venue.address, 13, 0xDFFFFFFF, 0);
    final TextView chips = text(context, venue.tags, 12, 0xFFFFC477, 1); chips.setPadding(0, dp(context, 9), 0, 0); sheet.addView(chips);
    line(sheet, "Opening hours  ·  " + venue.hours, 12, 0xAFFFFFFF, 0);
    final LinearLayout actions = new LinearLayout(context); actions.setGravity(Gravity.CENTER_VERTICAL); actions.setPadding(0, dp(context, 15), 0, 0); sheet.addView(actions);
    final String primary = "nightlife".equals(venue.category) || "rooftops".equals(venue.category) ? "Club Lobby" : "Website";
    final TextView primaryButton = text(context, primary, 14, 0xFF1A1320, 1); primaryButton.setGravity(Gravity.CENTER); primaryButton.setBackground(round(context, 0xFFFFC477, 16)); actions.addView(primaryButton, new LinearLayout.LayoutParams(0, dp(context, 47), 1));
    final TextView route = text(context, "Route  →", 14, Color.WHITE, 1); route.setGravity(Gravity.CENTER); route.setBackground(round(context, 0xFF322B3A, 16)); LinearLayout.LayoutParams routeParams = new LinearLayout.LayoutParams(0, dp(context, 47), 1); routeParams.setMargins(dp(context, 9), 0, 0, 0); actions.addView(route, routeParams);
    primaryButton.setOnClickListener(v -> Toast.makeText(context, primary + ": " + venue.name, Toast.LENGTH_SHORT).show()); route.setOnClickListener(v -> Toast.makeText(context, "Route to " + venue.name, Toast.LENGTH_SHORT).show());
    sheet.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
  }
  private static void dismiss(@NonNull ViewGroup root, @NonNull View scrim) { scrim.animate().alpha(0f).setDuration(150).withEndAction(() -> root.removeView(scrim)).start(); }
  @NonNull private static TextView text(Context c, String value, int size, int color, int style) { TextView v = new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setTypeface(null, style); return v; }
  private static void line(LinearLayout parent, String value, int size, int color, int style) { TextView v = text(parent.getContext(), value, size, color, style); v.setPadding(0, dp(parent.getContext(), 5), 0, 0); parent.addView(v); }
  @NonNull private static GradientDrawable round(Context c, int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d; }
  private static int heroColor(String category) { if ("food".equals(category)) return 0xFF754232; if ("prep".equals(category)) return 0xFF3D6B7A; if ("wellness".equals(category)) return 0xFF347664; if ("travel".equals(category)) return 0xFF62534B; return 0xFF5D276D; }
  private static int dp(Context c, int dp) { return Math.round(dp * c.getResources().getDisplayMetrics().density); }
}
