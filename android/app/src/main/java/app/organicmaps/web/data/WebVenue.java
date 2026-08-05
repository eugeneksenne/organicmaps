package app.organicmaps.web.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A venue known to the Venue Web Experience Engine. The engine refuses to open a WebView unless
 * the incoming venue has a trusted URL on record — this keeps the engine from becoming a generic
 * "open any URL" browser as required by the spec.
 */
public final class WebVenue
{
  @NonNull public final String venueId;
  @NonNull public final String name;
  @NonNull public final String websiteUrl;
  @Nullable public final String phoneNumber;
  @Nullable public final double latitude;
  @Nullable public final double longitude;

  public WebVenue(@NonNull String venueId, @NonNull String name, @NonNull String websiteUrl,
                  @Nullable String phoneNumber, double latitude, double longitude)
  {
    this.venueId = venueId;
    this.name = name;
    this.websiteUrl = websiteUrl;
    this.phoneNumber = phoneNumber;
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
