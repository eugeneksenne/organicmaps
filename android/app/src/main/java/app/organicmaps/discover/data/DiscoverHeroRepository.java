package app.organicmaps.discover.data;

import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/** Fetches a short-lived city snapshot; the UI retains its cached/default hero while offline. */
public class DiscoverHeroRepository
{
  public interface Callback { void onLoaded(@NonNull Hero hero); void onUnavailable(); }
  public static class Hero
  {
    @NonNull public final String city, headline, recommendation, weather;
    public final int energy, liveVenues, flashDrops;
    Hero(@NonNull JSONObject data)
    {
      city = data.optString("city_name"); headline = data.optString("headline"); recommendation = data.optString("recommendation"); weather = data.optString("weather_summary");
      energy = data.optInt("energy_percent"); liveVenues = data.optInt("live_venue_count"); flashDrops = data.optInt("flash_drop_count");
    }
  }
  private final OkHttpClient client = new OkHttpClient();
  public void load(@NonNull String cityId, @NonNull Callback callback)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty()) { callback.onUnavailable(); return; }
    new Thread(() -> {
      final String endpoint = BuildConfig.FOMO_SUPABASE_URL + "/rest/v1/city_discover_snapshots?city_id=eq." + cityId + "&select=*&limit=1";
      final Request request = new Request.Builder().url(endpoint).header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY).build();
      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful() || response.body() == null) { callback.onUnavailable(); return; }
        final JSONArray rows = new JSONArray(response.body().string());
        if (rows.length() == 0) callback.onUnavailable(); else callback.onLoaded(new Hero(rows.getJSONObject(0)));
      } catch (IOException | org.json.JSONException e) { callback.onUnavailable(); }
    }).start();
  }
}
