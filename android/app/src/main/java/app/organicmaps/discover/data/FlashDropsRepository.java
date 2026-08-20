package app.organicmaps.discover.data;

import androidx.annotation.NonNull;
import app.organicmaps.BuildConfig;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Fetches only currently active Drops; expiry is additionally enforced by the database RPC. */
public class FlashDropsRepository
{
  public interface Callback { void onLoaded(@NonNull JSONArray drops); void onUnavailable(); }
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient = new OkHttpClient();
  public void load(@NonNull String category, @NonNull Callback callback)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty()) { callback.onUnavailable(); return; }
    new Thread(() -> {
      try
      {
        final JSONObject body = new JSONObject().put("p_city_id", "johannesburg").put("p_category", "all".equals(category) ? JSONObject.NULL : category)
            .put("p_latitude", -26.2041).put("p_longitude", 28.0473).put("p_limit", 30);
        final Request request = new Request.Builder().url(BuildConfig.FOMO_SUPABASE_URL + "/rest/v1/rpc/active_flash_drops")
            .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY).header("Content-Type", "application/json").post(RequestBody.create(body.toString(), JSON)).build();
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
