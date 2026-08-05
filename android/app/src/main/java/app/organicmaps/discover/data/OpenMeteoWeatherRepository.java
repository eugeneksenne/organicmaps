package app.organicmaps.discover.data;

import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

/** Keyless Open-Meteo client. Open-Meteo publishes an open-source weather API and needs no SDK/key. */
public class OpenMeteoWeatherRepository
{
  public interface Callback { void onLoaded(@NonNull String summary); void onUnavailable(); }
  private static final String JOHANNESBURG_CURRENT =
      "https://api.open-meteo.com/v1/forecast?latitude=-26.2041&longitude=28.0473&current=temperature_2m,weather_code";
  private final OkHttpClient mClient = new OkHttpClient();

  public void loadJohannesburg(@NonNull Callback callback)
  {
    new Thread(() -> {
      final Request request = new Request.Builder().url(JOHANNESBURG_CURRENT).build();
      try (Response response = mClient.newCall(request).execute())
      {
        if (!response.isSuccessful() || response.body() == null) { callback.onUnavailable(); return; }
        final JSONObject current = new JSONObject(response.body().string()).getJSONObject("current");
        callback.onLoaded(Math.round(current.getDouble("temperature_2m")) + "°  " + weatherLabel(current.getInt("weather_code")));
      }
      catch (IOException | JSONException e) { callback.onUnavailable(); }
    }).start();
  }

  @NonNull
  private static String weatherLabel(int code)
  {
    if (code == 0) return "Clear";
    if (code <= 3) return "Cloudy";
    if (code == 45 || code == 48) return "Fog";
    if (code <= 67 || code >= 80 && code <= 82) return "Rain";
    if (code <= 77) return "Snow";
    if (code <= 99) return "Storm";
    return "Weather";
  }
}
