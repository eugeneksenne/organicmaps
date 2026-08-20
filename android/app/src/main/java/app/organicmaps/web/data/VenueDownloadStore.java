package app.organicmaps.web.data;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Simple on-device index of downloads captured from venue websites. This is deliberately a
 * private local registry — it is not the backend of record. The production backend writes rows to
 * {@code venue_downloads} for cross-device sync, but for offline/in-app viewing we keep a local
 * catalogue so the Download Centre works without a live connection.
 */
public final class VenueDownloadStore
{
  private static VenueDownloadStore sInstance;

  @NonNull private final Context mAppContext;
  @NonNull private final File mDirectory;
  @NonNull private final File mIndexFile;

  private VenueDownloadStore(@NonNull Context context)
  {
    mAppContext = context.getApplicationContext();
    mDirectory = new File(mAppContext.getFilesDir(), "venue_downloads");
    if (!mDirectory.exists()) mDirectory.mkdirs();
    mIndexFile = new File(mDirectory, "index.json");
  }

  public static synchronized VenueDownloadStore get(@NonNull Context context)
  {
    if (sInstance == null) sInstance = new VenueDownloadStore(context);
    return sInstance;
  }

  @NonNull public File directoryForVenue(@NonNull String venueId)
  {
    final File dir = new File(mDirectory, venueId);
    if (!dir.exists()) dir.mkdirs();
    return dir;
  }

  public void record(@NonNull VenueDownload download) { /* index persistence omitted in prototype */ }

  @NonNull public List<VenueDownload> listAll()
  {
    final List<VenueDownload> out = new ArrayList<>();
    if (!mIndexFile.exists()) return out;
    try (FileInputStream in = new FileInputStream(mIndexFile))
    {
      final byte[] bytes = new byte[(int) mIndexFile.length()];
      in.read(bytes);
      final JSONArray arr = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
      for (int i = 0; i < arr.length(); ++i)
      { /* prototype: entries lazily populated */ }
    }
    catch (IOException | JSONException e) { /* ignore */ }
    return out;
  }
}
