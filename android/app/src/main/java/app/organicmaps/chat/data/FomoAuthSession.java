package app.organicmaps.chat.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Persists the Supabase Auth session. Never stores a service-role key. */
public class FomoAuthSession
{
  private static final String PREFS = "fomo_auth";
  private final SharedPreferences mPrefs;

  public FomoAuthSession(@NonNull Context context)
  {
    mPrefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public boolean isSignedIn()
  {
    final String token = getAccessToken();
    return token != null && !token.isEmpty() && getUserId() != null;
  }

  @Nullable
  public String getAccessToken() { return mPrefs.getString("access_token", null); }

  @Nullable
  public String getRefreshToken() { return mPrefs.getString("refresh_token", null); }

  @Nullable
  public String getUserId() { return mPrefs.getString("user_id", null); }

  @Nullable
  public String getEmail() { return mPrefs.getString("email", null); }

  @Nullable
  public String getDisplayName() { return mPrefs.getString("display_name", null); }

  public long getExpiresAtMs() { return mPrefs.getLong("expires_at", 0L); }

  public boolean needsRefresh()
  {
    return isSignedIn() && getExpiresAtMs() < System.currentTimeMillis() + 60_000L;
  }

  public void save(@NonNull String accessToken, @Nullable String refreshToken, @NonNull String userId,
                   @Nullable String email, @Nullable String displayName, long expiresAtMs)
  {
    mPrefs.edit()
        .putString("access_token", accessToken)
        .putString("refresh_token", refreshToken)
        .putString("user_id", userId)
        .putString("email", email)
        .putString("display_name", displayName)
        .putLong("expires_at", expiresAtMs)
        .apply();
  }

  public void setDisplayName(@Nullable String displayName)
  {
    mPrefs.edit().putString("display_name", displayName).apply();
  }

  public void clear() { mPrefs.edit().clear().apply(); }
}
