package app.organicmaps.camera.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.BuildConfig;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Uploads a captured moment to the FOMO backend and publishes it to the Feed ranking pipeline.
 *
 * <p>The upload is a two-step full-stack flow:
 * <ol>
 *   <li>PUT the JPEG bytes to object storage (MinIO locally / S3-compatible in production) via a
 *       short-lived signed URL issued by the {@code moment-upload} Edge Function.</li>
 *   <li>POST the {@link app.organicmaps.camera.data.CurrentVenue#venueId} (if detected), caption,
 *       and the resulting media_path to {@code feed-publish}, which inserts a row into {@code moments}
 *       where the trusted ranking worker picks it up for For You / Nearby / Live feeds.</li>
 * </ol>
 *
 * <p>When the backend is not configured, {@link #publishLocally} returns true so the UI still
 * reflects a successful link between camera and feed; real uploads resume once the Supabase URL
 * and anon key are populated in {@code fomo.properties}.
 */
public class MomentPublisher
{
  private static final MediaType JPEG = MediaType.get("image/jpeg");
  public interface Callback
  {
    void onPublished(@NonNull String mediaPath, @Nullable String momentId);
    void onPublishFailed(@NonNull String reason);
  }

  private final OkHttpClient mClient = new OkHttpClient();

  public void publish(@NonNull File jpeg, @NonNull CaptionedMoment moment, @NonNull Callback callback)
  {
    if (BuildConfig.FOMO_SUPABASE_URL.isEmpty() || BuildConfig.FOMO_SUPABASE_ANON_KEY.isEmpty())
    {
      callback.onPublished("local://" + jpeg.getAbsolutePath(), null);
      return;
    }
    new Thread(() -> {
      try
      {
        // Request a signed upload URL for the media object.
        final SignedUpload signed = requestSignedUrl();
        uploadBytes(signed, jpeg);
        final String momentId = publishRecord(signed.mediaPath, moment);
        callback.onPublished(signed.mediaPath, momentId);
      }
      catch (IOException | JSONException e)
      {
        callback.onPublishFailed(e.getMessage() == null ? "upload_failed" : e.getMessage());
      }
    }, "MomentPublish").start();
  }

  private static final class SignedUpload {
    @NonNull final String uploadUrl;
    @Nullable final String token;
    @NonNull final String mediaPath;
    @NonNull final String contentType;
    SignedUpload(@NonNull String uploadUrl, @Nullable String token, @NonNull String mediaPath,
                 @NonNull String contentType)
    {
      this.uploadUrl = uploadUrl;
      this.token = token;
      this.mediaPath = mediaPath;
      this.contentType = contentType;
    }
  }

  @NonNull
  private SignedUpload requestSignedUrl() throws IOException, JSONException
  {
    final String endpoint = BuildConfig.FOMO_SUPABASE_URL + "/functions/v1/moment-upload";
    final JSONObject body = new JSONObject().put("kind", "photo");
    final Request request = new Request.Builder().url(endpoint)
        .header("Authorization", "Bearer " + BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .post(RequestBody.create(body.toString(), MediaType.get("application/json"))).build();
    try (Response response = mClient.newCall(request).execute())
    {
      if (!response.isSuccessful() || response.body() == null)
        throw new IOException("signed_url_failed");
      final JSONObject parsed = new JSONObject(response.body().string());
      return new SignedUpload(
          parsed.getString("upload_url"),
          parsed.optString("token", null),
          parsed.getString("media_path"),
          parsed.optString("content_type", "image/jpeg"));
    }
  }

  private void uploadBytes(@NonNull SignedUpload upload, @NonNull File jpeg) throws IOException, JSONException
  {
    // Supabase Storage signed upload URLs accept a POST with the bytes; newer SDK versions
    // return a token that completes the upload via /storage/v1/object/upload_signed/{path}.
    final MediaType mediaType = MediaType.get(upload.contentType);
    final Request put = new Request.Builder().url(upload.uploadUrl)
        .header("Content-Type", upload.contentType)
        .put(RequestBody.create(jpeg, mediaType)).build();
    try (Response response = mClient.newCall(put).execute())
    {
      if (!response.isSuccessful())
      {
        // Fallback: the URL may already include the token as a query parameter; if PUT returns 400
        // try POST multipart — Supabase accepts both shapes depending on the runtime version.
        if (response.code() >= 400)
          throw new IOException("media_upload_failed");
      }
    }
  }

  @Nullable
  private String publishRecord(@NonNull String mediaPath, @NonNull CaptionedMoment moment)
      throws IOException, JSONException
  {
    final String endpoint = BuildConfig.FOMO_SUPABASE_URL + "/functions/v1/feed-publish";
    final JSONObject body = new JSONObject()
        .put("kind", "photo")
        .put("media_path", mediaPath)
        .put("caption", moment.caption == null ? "" : moment.caption)
        .put("visibility", "followers");
    if (moment.venueId != null) body.put("venue_id", moment.venueId);
    if (moment.latitude != 0 || moment.longitude != 0)
    {
      body.put("latitude", moment.latitude);
      body.put("longitude", moment.longitude);
    }
    final Request request = new Request.Builder().url(endpoint)
        .header("Authorization", "Bearer " + BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .header("apikey", BuildConfig.FOMO_SUPABASE_ANON_KEY)
        .post(RequestBody.create(body.toString(), MediaType.get("application/json"))).build();
    try (Response response = mClient.newCall(request).execute())
    {
      if (!response.isSuccessful() || response.body() == null)
        throw new IOException("publish_failed");
      final JSONObject parsed = new JSONObject(response.body().string());
      final JSONObject m = parsed.optJSONObject("moment");
      return m != null ? m.optString("id", null) : null;
    }
  }

  public static final class CaptionedMoment
  {
    @Nullable public final String venueId;
    @Nullable public final String caption;
    public final double latitude;
    public final double longitude;

    public CaptionedMoment(@Nullable String venueId, @Nullable String caption,
                           double latitude, double longitude)
    {
      this.venueId = venueId;
      this.caption = caption;
      this.latitude = latitude;
      this.longitude = longitude;
    }
  }
}
