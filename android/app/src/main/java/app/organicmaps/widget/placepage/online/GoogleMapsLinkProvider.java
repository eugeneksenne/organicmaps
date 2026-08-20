package app.organicmaps.widget.placepage.online;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Builds an external Google Maps link without displaying Google Places content in Organic Maps. */
public final class GoogleMapsLinkProvider
{
  private GoogleMapsLinkProvider() {}

  @NonNull public static String createSearchUrl(@Nullable String name, double latitude, double longitude)
  {
    String label = name == null ? "" : name.trim();
    String query = label.isEmpty() ? String.format(java.util.Locale.US, "%.6f,%.6f", latitude, longitude)
                                   : label + " " + String.format(java.util.Locale.US, "%.6f,%.6f", latitude, longitude);
    return "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query);
  }
}
