package app.organicmaps.chat.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.BuildConfig;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

/** Email/password Supabase Auth used by the chats stack. */
public class FomoAuthRepository
{
  public interface Callback
  {
    void onSuccess();
    void onError(@NonNull String message);
  }

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final OkHttpClient mClient;
  private final FomoAuthSession mSession;

  public FomoAuthRepository(@NonNull OkHttpClient client, @NonNull FomoAuthSession session)
  {
    mClient = client;
    mSession = session;
  }

  public boolean isConfigured()
  {
    return !BuildConfig.FOMO_SUPABASE_URL.isEmpty() && !BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty();
  }

  public void signIn(@NonNull String email, @NonNull String password, @NonNull Callback callback)
  {
    auth("token?grant_type=password", email, password, null, callback);
  }

  public void signUp(@NonNull String email, @NonNull String password, @NonNull Callback callback)
  {
    auth("signup", email, password, usernameFromEmail(email), callback);
  }

  public void signOut()
  {
    final String token = mSession.getAccessToken();
    mSession.clear();
    if (!isConfigured() || token == null)
      return;
    new Thread(() -> {
      try
      {
        mClient.newCall(authed(BuildConfig.FOMO_SUPABASE_URL + "/auth/v1/logout", token)
                            .post(RequestBody.create("{}", JSON)).build()).execute().close();
      }
      catch (IOException ignored) {}
    }).start();
  }

  public boolean refreshIfNeeded()
  {
    if (!mSession.needsRefresh())
      return mSession.isSignedIn();
    final String refresh = mSession.getRefreshToken();
    if (refresh == null || refresh.isEmpty() || !isConfigured())
      return false;
    try
    {
      final JSONObject body = new JSONObject().put("refresh_token", refresh);
      final Request request = publicRequest(BuildConfig.FOMO_SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                                  .post(RequestBody.create(body.toString(), JSON)).build();
      try (Response response = mClient.newCall(request).execute())
      {
        if (!response.isSuccessful() || response.body() == null)
          return false;
        persistSession(new JSONObject(response.body().string()), mSession.getEmail());
        return true;
      }
    }
    catch (IOException | JSONException e)
    {
      return false;
    }
  }

  public void ensureProfile()
  {
    final String token = mSession.getAccessToken();
    if (token == null || !isConfigured())
      return;
    try
    {
      final JSONObject body = new JSONObject()
          .put("p_username", usernameFromEmail(mSession.getEmail() == null ? "user" : mSession.getEmail()))
          .put("p_display_name", displayNameFromEmail(mSession.getEmail()));
      final Request request = authed(BuildConfig.FOMO_SUPABASE_URL + "/rest/v1/rpc/ensure_own_profile", token)
                                  .post(RequestBody.create(body.toString(), JSON)).build();
      try (Response response = mClient.newCall(request).execute())
      {
        if (!response.isSuccessful() || response.body() == null)
          return;
        final JSONObject profile = new JSONObject(response.body().string());
        mSession.setDisplayName(profile.optString("display_name", mSession.getDisplayName()));
      }
    }
    catch (IOException | JSONException ignored) {}
  }

  private void auth(@NonNull String path, @NonNull String email, @NonNull String password,
                    @Nullable String username, @NonNull Callback callback)
  {
    if (!isConfigured())
    {
      callback.onError("Configure android/fomo.properties with a Supabase URL and anon key.");
      return;
    }
    new Thread(() -> {
      try
      {
        final JSONObject body = new JSONObject().put("email", email.trim()).put("password", password);
        if (username != null)
          body.put("data", new JSONObject().put("username", username).put("display_name", displayNameFromEmail(email)));
        final Request request = publicRequest(BuildConfig.FOMO_SUPABASE_URL + "/auth/v1/" + path)
                                    .post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = mClient.newCall(request).execute())
        {
          final String raw = response.body() == null ? "" : response.body().string();
          if (!response.isSuccessful())
          {
            callback.onError(errorMessage(raw, "Could not sign in"));
            return;
          }
          persistSession(new JSONObject(raw), email.trim());
          ensureProfile();
          callback.onSuccess();
        }
      }
      catch (IOException | JSONException e)
      {
        callback.onError("Network error. Try again.");
      }
    }).start();
  }

  private void persistSession(@NonNull JSONObject payload, @Nullable String email) throws JSONException
  {
    final JSONObject user = payload.optJSONObject("user");
    final String userId = user != null ? user.optString("id") : payload.optString("id");
    long expiresAt = System.currentTimeMillis() + payload.optLong("expires_in", 3600L) * 1000L;
    if (payload.has("expires_at") && payload.optLong("expires_at") > 1_000_000_000L)
      expiresAt = payload.optLong("expires_at") * 1000L;
    final String display = user != null
        ? user.optJSONObject("user_metadata") != null
            ? user.optJSONObject("user_metadata").optString("display_name", displayNameFromEmail(email))
            : displayNameFromEmail(email)
        : displayNameFromEmail(email);
    mSession.save(payload.getString("access_token"), payload.optString("refresh_token", mSession.getRefreshToken()),
                  userId, email, display, expiresAt);
  }

  @NonNull
  private static Request.Builder publicRequest(@NonNull String url)
  {
    return new Request.Builder().url(url)
        .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("Content-Type", "application/json");
  }

  @NonNull
  private static Request.Builder authed(@NonNull String url, @NonNull String token)
  {
    return publicRequest(url).header("Authorization", "Bearer " + token);
  }

  @NonNull
  static String usernameFromEmail(@NonNull String email)
  {
    final String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    final String cleaned = local.toLowerCase().replaceAll("[^a-z0-9_.]", "");
    return cleaned.length() >= 3 ? cleaned.substring(0, Math.min(32, cleaned.length()))
                                 : "user" + Integer.toHexString(email.hashCode() & 0x7fffffff);
  }

  @NonNull
  static String displayNameFromEmail(@Nullable String email)
  {
    if (email == null || email.isEmpty())
      return "You";
    final String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    if (local.isEmpty())
      return "You";
    return Character.toUpperCase(local.charAt(0)) + local.substring(1);
  }

  @NonNull
  private static String errorMessage(@NonNull String raw, @NonNull String fallback)
  {
    try
    {
      final JSONObject object = new JSONObject(raw);
      final String message = object.optString("error_description", object.optString("msg", object.optString("error", "")));
      return message.isEmpty() ? fallback : message;
    }
    catch (JSONException e)
    {
      return fallback;
    }
  }
}
