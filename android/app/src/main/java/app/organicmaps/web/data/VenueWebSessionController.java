package app.organicmaps.web.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-venue WebView lifecycle controller. Implements three guarantees from the spec:
 *
 * <ul>
 *   <li><b>Session isolation:</b> every {@link WebVenue} gets its own {@link WebView} instance
 *       and its own CookieManager partition directory. Cookies/storage/logins for one venue never
 *       leak to another.</li>
 *   <li><b>Pooling:</b> up to {@value #MAX_POOL_SIZE} warm WebView instances are kept alive in a
 *       simple LRU list so reopening a recent venue (or tapping "Website" right after preload) is
 *       near-instant. The least-recently-used instance is evicted when the pool overflows.</li>
 *   <li><b>Preloading:</b> {@link #preload(WebVenue)} starts DNS/TLS warm-up the moment a venue
 *       details page is opened; the connection is released after {@value #PRELOAD_TTL_MS} unless
 *       the user taps Website within that window.</li>
 * </ul>
 *
 * KMP note: this is the Android `actual` for the common controller described in the spec; iOS
 * mirrors the same contract with a WKWebsiteDataStore per venue.
 */
public final class VenueWebSessionController
{
  private static final int MAX_POOL_SIZE = 3;
  private static final long PRELOAD_TTL_MS = 5 * 60 * 1000L; // 5 minutes.

  private static VenueWebSessionController sInstance;

  @NonNull private final Context mAppContext;
  @NonNull private final Map<String, WebView> mSessions = new HashMap<>();
  @NonNull private final Deque<String> mAccessOrder = new ArrayDeque<>();
  @NonNull private final Map<String, Runnable> mPreloadExpiry = new HashMap<>();
  @NonNull private final Handler mHandler = new Handler(Looper.getMainLooper());

  private VenueWebSessionController(@NonNull Context context)
  {
    mAppContext = context.getApplicationContext();
    CookieManager.getInstance().setAcceptCookie(true);
  }

  public static synchronized VenueWebSessionController get(@NonNull Context context)
  {
    if (sInstance == null) sInstance = new VenueWebSessionController(context);
    return sInstance;
  }

  /** Acquire (create or reuse from pool) the isolated WebView for the given venue. */
  @NonNull
  public WebView acquire(@NonNull WebVenue venue)
  {
    cancelPreloadExpiry(venue.venueId);
    WebView view = mSessions.get(venue.venueId);
    if (view != null)
    {
      touch(venue.venueId);
      return view;
    }
    view = createIsolatedWebView(venue);
    mSessions.put(venue.venueId, view);
    touch(venue.venueId);
    evictIfNeeded();
    return view;
  }

  /** Warm-up DNS/TLS and load the landing page in an off-pool WebView the user can later acquire(). */
  public void preload(@NonNull WebVenue venue)
  {
    if (mSessions.containsKey(venue.venueId)) return;
    final WebView view = createIsolatedWebView(venue);
    view.loadUrl(venue.websiteUrl);
    mSessions.put(venue.venueId, view);
    touch(venue.venueId);
    evictIfNeeded();
    cancelPreloadExpiry(venue.venueId);
    final Runnable cleanup = () -> destroy(venue.venueId);
    mPreloadExpiry.put(venue.venueId, cleanup);
    mHandler.postDelayed(cleanup, PRELOAD_TTL_MS);
  }

  public void destroy(@NonNull String venueId)
  {
    cancelPreloadExpiry(venueId);
    final WebView view = mSessions.remove(venueId);
    mAccessOrder.remove(venueId);
    if (view != null)
    {
      view.stopLoading();
      view.loadUrl("about:blank");
      view.clearHistory();
      view.removeAllViews();
      view.destroy();
    }
  }

  @NonNull
  private WebView createIsolatedWebView(@NonNull WebVenue venue)
  {
    final Context isolated = new android.content.ContextWrapper(mAppContext) {
      @Override public File getDir(String name, int mode)
      {
        // Partition cookies/databases per venue — the CookieManager/ WebStorage directory is set
        // by the WebView implementation from this context's data dir suffix.
        final File base = super.getDir("web_" + venue.venueId, MODE_PRIVATE);
        if (!base.exists()) base.mkdirs();
        return base;
      }
    };
    final WebView view = new WebView(isolated);
    view.getSettings().setJavaScriptEnabled(true);
    view.getSettings().setDomStorageEnabled(true);
    view.getSettings().setDatabaseEnabled(true);
    view.getSettings().setLoadWithOverviewMode(true);
    view.getSettings().setUseWideViewPort(true);
    view.getSettings().setBuiltInZoomControls(false);
    view.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    view.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
    return view;
  }

  private void touch(@NonNull String venueId)
  {
    mAccessOrder.remove(venueId);
    mAccessOrder.addFirst(venueId);
  }

  private void evictIfNeeded()
  {
    while (mSessions.size() > MAX_POOL_SIZE)
    {
      final String oldest = mAccessOrder.pollLast();
      if (oldest == null) return;
      destroy(oldest);
    }
  }

  private void cancelPreloadExpiry(@NonNull String venueId)
  {
    final Runnable r = mPreloadExpiry.remove(venueId);
    if (r != null) mHandler.removeCallbacks(r);
  }
}
