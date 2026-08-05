package app.organicmaps.discover.data;

import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads curated Explore venues from the server-ranked, public discovery RPC. */
public class CityExploreRepository
{
  public interface Callback { void onLoaded(@NonNull JSONArray venues); void onUnavailable(); }
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient = new OkHttpClient();

  public void load(@NonNull String world, @NonNull Callback callback)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty())
    {
      callback.onUnavailable();
      return;
    }
    new Thread(() -> {
      try
      {
        final JSONObject requestData = new JSONObject();
        requestData.put("p_city_id", "johannesburg");
        requestData.put("p_world", world); // coarse city-centre fallback; consented location can replace this.
        requestData.put("p_latitude", -26.2041);
        requestData.put("p_longitude", 28.0473);
        final Calendar localTime = Calendar.getInstance();
        requestData.put("p_now", new SimpleDateFormat("HH:mm:ss", Locale.US).format(localTime.getTime()));
        requestData.put("p_local_date", new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(localTime.getTime()));
        int dayOfWeek = localTime.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayOfWeek == 0) dayOfWeek = 7; // PostgreSQL ISO weekday: Monday=1, Sunday=7.
        requestData.put("p_day_of_week", dayOfWeek);
        requestData.put("p_limit", 20);
        final Request request = new Request.Builder()
            .url(BuildConfig.FOMO_SUPABASE_URL + "/rest/v1/rpc/explore_city_venues")
            .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestData.toString(), JSON)).build();
        try (Response response = mClient.newCall(request).execute())
        {
          if (!response.isSuccessful() || response.body() == null) { callback.onUnavailable(); return; }
          callback.onLoaded(new JSONArray(response.body().string()));
        }
      }
      catch (IOException | JSONException e) { callback.onUnavailable(); }
    }).start();
  }
}
