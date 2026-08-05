package app.organicmaps.camera.data;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.BuildConfig;
import app.organicmaps.MwmApplication;
import app.organicmaps.sdk.Framework;
import java.io.IOException;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Resolves the user's current venue for the FOMO Camera.
 *
 * <p>We deliberately do not let the client fabricate venues. A snapshot must be:
 * <ul>
 *   <li>Approved by the trusted moderation/safety backend (the {@code venue_snapshots.safety_state}
 *       column — the RLS policy already filters to approved rows).</li>
 *   <li>Within the camera detection radius (80 m) of the current location fix.</li>
 *   <li>The <em>closest</em> such venue so multi-storey blocks resolve deterministically.</li>
 * </ul>
 * Results are one-shot per call; the camera tab re-queries on open and on significant location
 * changes so the pill stays fresh as the user moves between spots.
 */
public class CurrentVenueDetector
{
  private static final float DETECTION_RADIUS_METERS = 80f;

  public interface Callback
  {
    void onVenueResolved(@NonNull CurrentVenue venue);
  }

  private final OkHttpClient mClient = new OkHttpClient();

  /**
   * Runs the detection off the UI thread. The callback is always invoked on the thread that owns
   * the OkHttp call; callers routing to the UI must re-post explicitly.
   */
  public void detect(@NonNull Context context, @NonNull Callback callback)
  {
    final Location location = MwmApplication.from(context).getLocationHelper().getSavedLocation();
    if (location == null)
    {
      callback.onVenueResolved(CurrentVenue.NONE);
      return;
    }
    final double lat = location.getLatitude();
    final double lon = location.getLongitude();
    final String reverseAddress = Framework.nativeGetAddress(lat, lon);
    new Thread(() -> {
      CurrentVenue chosen = queryBackend(lat, lon, reverseAddress);
      if (chosen == null)
        chosen = CurrentVenue.NONE;
      callback.onVenueResolved(chosen);
    }, "VenueDetector").start();
  }

  @Nullable
  private CurrentVenue queryBackend(double lat, double lon, @Nullable String reverseAddress)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty())
      return null;
    // PostGIS-style bounding box filter (kept simple: a 300 m box around the user); the RLS policy
    // already enforces safety_state = 'approved' so only reviewed venues are considered.
    final double latSpan = 0.0027; // ~300 m
    final double lonSpan = 0.0027 / Math.max(Math.cos(Math.toRadians(lat)), 0.2);
    final String url = String.format(
        Locale.US,
        "%s/rest/v1/venue_snapshots?select=venue_id,name,category,address,latitude,longitude,live_now&latitude=gte.%f&latitude=lte.%f&longitude=gte.%f&longitude=lte.%f&order=latitude.asc&limit=50",
        BuildConfig.FOMO_SUPABASE_URL, lat - latSpan, lat + latSpan, lon - lonSpan, lon + lonSpan);
    final Request request = new Request.Builder().url(url).header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("Authorization", "Bearer " + BuildConfig.FOMO_SUPABASE_ANON_KEY).build();
    try (Response response = mClient.newCall(request).execute())
    {
      if (!response.isSuccessful() || response.body() == null)
        return null;
      final JSONArray rows = new JSONArray(response.body().string());
      return pickClosest(rows, lat, lon, reverseAddress);
    }
    catch (IOException | JSONException e)
    {
      return null;
    }
  }

  @Nullable
  private CurrentVenue pickClosest(@NonNull JSONArray rows, double lat, double lon,
                                   @Nullable String reverseAddress) throws JSONException
  {
    CurrentVenue best = null;
    float bestMeters = DETECTION_RADIUS_METERS;
    final float[] result = new float[1];
    for (int i = 0; i < rows.length(); ++i)
    {
      final JSONObject row = rows.getJSONObject(i);
      final double vLat = row.optDouble("latitude", Double.NaN);
      final double vLon = row.optDouble("longitude", Double.NaN);
      if (Double.isNaN(vLat) || Double.isNaN(vLon)) continue;
      Location.distanceBetween(lat, lon, vLat, vLon, result);
      if (result[0] < bestMeters)
      {
        bestMeters = result[0];
        final String venueId = row.optString("venue_id", null);
        final String name = row.optString("name", null);
        best = new CurrentVenue(
            venueId,
            name,
            row.optString("category", null),
            firstNonEmpty(row.optString("address", null), reverseAddress),
            row.optBoolean("live_now", false),
            vLat, vLon, result[0]);
      }
    }
    return best;
  }

  @Nullable
  private static String firstNonEmpty(@Nullable String primary, @Nullable String fallback)
  {
    if (primary != null && !primary.isEmpty()) return primary;
    return fallback;
  }
}
