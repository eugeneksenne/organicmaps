package app.organicmaps.widget.placepage.online;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Session-only Foursquare Places client. The key is supplied by build configuration and is never
 * stored in the map or MWM files. Provider data must be displayed with the required attribution.
 */
public final class FoursquarePlacesRepository
{
  public interface Callback
  {
    void onLoaded(@NonNull OnlinePlaceDetails details);
    void onUnavailable();
  }

  private static final String BASE_URL = "https://api.foursquare.com/v3/places/search";
  private final OkHttpClient mClient = new OkHttpClient();
  private final ExecutorService mExecutor = Executors.newCachedThreadPool();
  private final String mApiKey;

  public FoursquarePlacesRepository(@Nullable String apiKey)
  {
    mApiKey = apiKey == null ? "" : apiKey.trim();
  }

  public void findNearby(double latitude, double longitude, @Nullable String query, @NonNull Callback callback)
  {
    if (mApiKey.isEmpty()) { callback.onUnavailable(); return; }
    mExecutor.execute(() -> {
      HttpUrl.Builder url = HttpUrl.parse(BASE_URL).newBuilder()
          .addQueryParameter("ll", String.format(Locale.US, "%.6f,%.6f", latitude, longitude))
          .addQueryParameter("radius", "100")
          .addQueryParameter("limit", "1")
          .addQueryParameter("fields", "fsq_id,name,location,website,tel,hours,rating,link,photos,social_media");
      if (query != null && !query.trim().isEmpty()) url.addQueryParameter("query", query.trim());
      Request request = new Request.Builder().url(url.build()).addHeader("Authorization", mApiKey)
          .addHeader("Accept", "application/json").build();
      try (Response response = mClient.newCall(request).execute())
      {
        if (!response.isSuccessful() || response.body() == null) { callback.onUnavailable(); return; }
        JSONArray results = new JSONObject(response.body().string()).optJSONArray("results");
        if (results == null || results.length() == 0) { callback.onUnavailable(); return; }
        callback.onLoaded(parse(results.getJSONObject(0)));
      }
      catch (IOException | JSONException e) { callback.onUnavailable(); }
    });
  }

  @NonNull private static OnlinePlaceDetails parse(@NonNull JSONObject venue)
  {
    JSONObject location = venue.optJSONObject("location");
    String address = location == null ? null : location.optString("formatted_address", null);
    JSONObject hours = venue.optJSONObject("hours");
    String hoursText = hours == null ? null : hours.optString("display", null);
    Double rating = venue.has("rating") ? venue.optDouble("rating") : null;
    List<String> photos = new ArrayList<>();
    JSONArray photoArray = venue.optJSONArray("photos");
    if (photoArray != null)
      for (int i = 0; i < Math.min(3, photoArray.length()); ++i)
      {
        JSONObject photo = photoArray.optJSONObject(i);
        if (photo == null) continue;
        String prefix = photo.optString("prefix", "");
        String suffix = photo.optString("suffix", "");
        if (!prefix.isEmpty() && !suffix.isEmpty()) photos.add(prefix + "original" + suffix);
      }
    Map<String, String> social = new HashMap<>();
    JSONObject socialMedia = venue.optJSONObject("social_media");
    if (socialMedia != null)
      for (String key : new String[] {"facebook", "instagram", "twitter"})
      {
        String value = socialMedia.optString(key, "");
        if (!value.isEmpty()) social.put(key, value);
      }
    return new OnlinePlaceDetails("Foursquare", venue.optString("fsq_id", null),
        venue.optString("name", null), address, venue.optString("website", null),
        venue.optString("tel", null), hoursText, rating, venue.optString("link", null), photos, social);
  }
}
