package app.organicmaps.camera.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A venue (bar, club, restaurant, event space, ...) currently near the user so that a captured
 * Moment can be tagged for the Feed's Nearby and Following tabs and for the venue invitation
 * surface described in the FOMO feed schema.
 *
 * <p>This is surfaced by {@link CurrentVenueDetector} using a two-step lookup:
 * <ol>
 *   <li>Reverse-geocode the current lat/lon with Organic Maps core to get a street/suburb anchor.</li>
 *   <li>Query the trusted backend {@code venue-snapshots} table (which holds only approved venues
 *       and their live/event metadata) and pick the closest snapshot within the detection radius.</li>
 * </ol>
 * When no approved venue is nearby the detector returns {@link #NONE} so the camera pill renders a
 * neutral "share where you are" state instead of fabricating a place.
 */
public final class CurrentVenue
{
  public static final CurrentVenue NONE = new CurrentVenue(null, null, null, null, false, 0, 0, Float.NaN);

  @Nullable public final String venueId;
  @Nullable public final String name;
  @Nullable public final String category;
  @Nullable public final String address;
  public final boolean liveNow;
  public final double latitude;
  public final double longitude;
  public final float distanceMeters;

  public CurrentVenue(@Nullable String venueId, @Nullable String name, @Nullable String category,
                      @Nullable String address, boolean liveNow, double latitude, double longitude,
                      float distanceMeters)
  {
    this.venueId = venueId;
    this.name = name;
    this.category = category;
    this.address = address;
    this.liveNow = liveNow;
    this.latitude = latitude;
    this.longitude = longitude;
    this.distanceMeters = distanceMeters;
  }

  public boolean isDetected() { return venueId != null && name != null; }

  @NonNull
  public String displayLabel()
  {
    if (!isDetected())
      return "Share where you are";
    if (liveNow)
      return "● " + name + "  •  LIVE NOW";
    return name;
  }
}
