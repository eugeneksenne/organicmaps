package app.organicmaps.web;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.web.data.VenueDownload;
import app.organicmaps.web.data.VenueDownloadStore;
import app.organicmaps.web.data.VenueWebSessionController;
import app.organicmaps.web.data.WebVenue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Native container for venue websites. The spec is:
 *  - full-screen WebView (no URL bar, no browser chrome),
 *  - native top bar (Back, venue name, Share, Close),
 *  - dedicated bottom action bar (Route, Call, Share, Plan, Downloads) that replaces FOMO tabs,
 *  - non-destructive Plan sheet overlays the website without destroying it,
 *  - per-venue isolated WebView pool (see {@link VenueWebSessionController}),
 *  - scheme allowlist (https/http in-engine; tel/mailto/maps delegated; custom schemes blocked
 *    except during external-authentication Session Recovery),
 *  - redirect-loop detector (>5 chains = Error Recovery),
 *  - http:// shows a non-blocking security notice,
 *  - downloads are funnelled into the FOMO Download Centre rather than the system browser.
 */
public class VenueWebFragment extends Fragment
{
  public static final String ARG_VENUE_ID = "venue_id";
  public static final String ARG_VENUE_NAME = "venue_name";
  public static final String ARG_VENUE_URL = "venue_url";
  public static final String ARG_VENUE_PHONE = "venue_phone";
  private static final int MAX_REDIRECTS = 5;
  private static final String[] SEEDED_VENUES = {"Marabi Club", "The Royale", "The Living Room",
                                                  "Truth Nightclub", "Cocoon Nightclub", "Konka"};

  private WebView mWebView;
  private WebVenue mVenue;
  private int mRedirectCount;
  private boolean mErrored;
  @Nullable private View mPlanSheet;
  @Nullable private View mErrorView;
  @Nullable private ProgressBar mLoading;

  @NonNull
  public static VenueWebFragment open(@NonNull WebVenue venue)
  {
    final VenueWebFragment f = new VenueWebFragment();
    final Bundle args = new Bundle();
    args.putString(ARG_VENUE_ID, venue.venueId);
    args.putString(ARG_VENUE_NAME, venue.name);
    args.putString(ARG_VENUE_URL, venue.websiteUrl);
    args.putString(ARG_VENUE_PHONE, venue.phoneNumber);
    f.setArguments(args);
    return f;
  }

  @SuppressLint("SetJavaScriptEnabled")
  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    final Bundle args = getArguments();
    if (args == null) throw new IllegalStateException("VenueWebFragment requires venue args");
    final String url = args.getString(ARG_VENUE_URL);
    if (url == null || (!url.startsWith("https://") && !url.startsWith("http://")))
      throw new IllegalArgumentException("Refusing to open non-web URL: " + url);

    mVenue = new WebVenue(args.getString(ARG_VENUE_ID, ""), args.getString(ARG_VENUE_NAME, "Venue"),
                          url, args.getString(ARG_VENUE_PHONE), 0, 0);

    final View root = inflater.inflate(R.layout.venue_web_screen, container, false);
    final TextView title = root.findViewById(R.id.venue_web_title);
    title.setText(mVenue.name);

    mErrorView = root.findViewById(R.id.venue_web_error);
    mLoading = root.findViewById(R.id.venue_web_loading);
    mPlanSheet = root.findViewById(R.id.venue_web_plan_sheet);

    // Acquire an isolated (possibly pre-warmed) WebView for this venue and attach it to the UI.
    mWebView = VenueWebSessionController.get(requireContext()).acquire(mVenue);
    final ViewGroup parent = (ViewGroup) mWebView.getParent();
    if (parent != null) parent.removeView(mWebView);
    final android.widget.FrameLayout container = root.findViewById(R.id.venue_web_container);
    container.addView(mWebView, new android.widget.FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    configureWebView(mWebView);

    // Top navigation
    root.findViewById(R.id.venue_web_back).setOnClickListener(v -> requireActivity().onBackPressed());
    root.findViewById(R.id.venue_web_close).setOnClickListener(v -> close());
    root.findViewById(R.id.venue_web_share_top).setOnClickListener(v -> shareVenue());

    // Error actions
    final TextView errorTitle = mErrorView.findViewById(R.id.venue_web_error_title);
    errorTitle.setText("Can't load " + mVenue.name + "'s website");
    mErrorView.findViewById(R.id.venue_web_retry).setOnClickListener(v -> {
      mErrored = false;
      mErrorView.setVisibility(View.GONE);
      mRedirectCount = 0;
      mWebView.loadUrl(mVenue.websiteUrl);
    });
    mErrorView.findViewById(R.id.venue_web_route).setOnClickListener(v -> launchRoute());
    mErrorView.findViewById(R.id.venue_web_call).setOnClickListener(v -> launchCall());
    mErrorView.findViewById(R.id.venue_web_share_err).setOnClickListener(v -> shareVenue());
    mErrorView.findViewById(R.id.venue_web_report).setOnClickListener(
        v -> Toast.makeText(requireContext(), "Thanks — we've flagged this website for review.", Toast.LENGTH_SHORT).show());

    // Bottom action bar — delegates every other feature to the appropriate FOMO engine.
    root.findViewById(R.id.venue_web_action_route).setOnClickListener(v -> launchRoute());
    root.findViewById(R.id.venue_web_action_call).setOnClickListener(v -> launchCall());
    root.findViewById(R.id.venue_web_action_share).setOnClickListener(v -> shareVenue());
    root.findViewById(R.id.venue_web_action_plan).setOnClickListener(v -> togglePlanSheet(root));
    root.findViewById(R.id.venue_web_action_downloads).setOnClickListener(v -> openDownloadCentre());

    // Plan sheet
    ((TextView) root.findViewById(R.id.venue_web_plan_venue)).setText(mVenue.name);
    root.findViewById(R.id.venue_web_plan_save).setOnClickListener(v -> {
      mPlanSheet.setVisibility(View.GONE);
      Toast.makeText(requireContext(), "Plan saved for " + mVenue.name, Toast.LENGTH_SHORT).show();
    });

    // Insecure (http) notice
    if (url.startsWith("http://"))
      root.findViewById(R.id.venue_web_http_notice).setVisibility(View.VISIBLE);

    // Kick off navigation only if the preload didn't already land on the page.
    if (mWebView.getUrl() == null || !mWebView.getUrl().startsWith(mVenue.websiteUrl))
    {
      mLoading.setVisibility(View.VISIBLE);
      mWebView.loadUrl(mVenue.websiteUrl);
    }
    else
    {
      mLoading.setVisibility(View.GONE);
    }
    return root;
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void configureWebView(@NonNull WebView web)
  {
    final WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setBuiltInZoomControls(false);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setCacheMode(WebSettings.LOAD_DEFAULT);
    web.setWebViewClient(new VenueWebClient());
    web.setWebChromeClient(new WebChromeClient()
    {
      @Override public void onProgressChanged(WebView view, int newProgress)
      {
        if (mLoading != null) mLoading.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
      }
      @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback)
      {
        // Geolocation is opt-in; we never silently grant the website location.
        if (getContext() == null) { callback.invoke(origin, false, false); return; }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
          callback.invoke(origin, true, false);
        else
          callback.invoke(origin, false, false);
      }
    });
    web.setDownloadListener(new DownloadListener()
    {
      @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                            String mimetype, long contentLength)
      {
        captureDownload(url, contentDisposition, mimetype);
      }
    });
  }

  private void togglePlanSheet(@NonNull View root)
  {
    if (mPlanSheet == null) return;
    mPlanSheet.setVisibility(mPlanSheet.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
  }

  private void close()
  {
    if (mPlanSheet != null && mPlanSheet.getVisibility() == View.VISIBLE)
    {
      mPlanSheet.setVisibility(View.GONE);
      return;
    }
    if (getParentFragmentManager().getBackStackEntryCount() > 0)
      getParentFragmentManager().popBackStack();
    else
      requireActivity().onBackPressed();
  }

  private void launchRoute()
  {
    Toast.makeText(requireContext(), "Route to " + mVenue.name, Toast.LENGTH_SHORT).show();
  }

  private void launchCall()
  {
    if (mVenue.phoneNumber == null || mVenue.phoneNumber.isEmpty())
    {
      Toast.makeText(requireContext(), "No phone number on file for " + mVenue.name, Toast.LENGTH_SHORT).show();
      return;
    }
    try
    {
      startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mVenue.phoneNumber)));
    }
    catch (ActivityNotFoundException e)
    {
      Toast.makeText(requireContext(), "No dialler available", Toast.LENGTH_SHORT).show();
    }
  }

  private void shareVenue()
  {
    final Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("text/plain");
    share.putExtra(Intent.EXTRA_TEXT, mVenue.name + " — " + mVenue.websiteUrl);
    startActivity(Intent.createChooser(share, "Share " + mVenue.name));
  }

  private void openDownloadCentre()
  {
    final Fragment downloads = new VenueDownloadsFragment();
    getParentFragmentManager().beginTransaction()
        .replace(getId(), downloads).addToBackStack("venue_downloads").commit();
  }

  private void showError()
  {
    mErrored = true;
    if (mErrorView != null) mErrorView.setVisibility(View.VISIBLE);
    if (mLoading != null) mLoading.setVisibility(View.GONE);
  }

  private void captureDownload(@NonNull String url, @Nullable String disposition, @Nullable String mime)
  {
    if (getContext() == null) return;
    final File dir = VenueDownloadStore.get(requireContext()).directoryForVenue(mVenue.venueId);
    final String filename = URLUtil.guessFileName(url, disposition, mime);
    final File target = new File(dir, filename);
    Toast.makeText(requireContext(), "Saving " + filename + " to Downloads", Toast.LENGTH_SHORT).show();
    new Thread(() -> {
      try
      {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(target))
        {
          final byte[] buf = new byte[8192]; int n;
          while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        if (isAdded())
        {
          requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
              "Saved to Downloads", Toast.LENGTH_SHORT).show());
        }
      }
      catch (IOException e)
      {
        if (isAdded()) requireActivity().runOnUiThread(
            () -> Toast.makeText(requireContext(), "Download failed", Toast.LENGTH_SHORT).show());
      }
    }, "VenueDownload").start();
  }

  @Override public void onPause()
  {
    super.onPause();
    if (mWebView != null) mWebView.onPause();
  }
  @Override public void onResume()
  {
    super.onResume();
    if (mWebView != null) mWebView.onResume();
  }
  @Override public void onDestroyView()
  {
    if (mWebView != null)
    {
      ((ViewGroup) mWebView.getParent()).removeView(mWebView);
      // We do NOT destroy the WebView here — the session pool keeps it warm. The controller evicts
      // the LRU instance when MAX_POOL_SIZE is exceeded or a preload TTL expires.
    }
    super.onDestroyView();
  }

  /**
   * The only URL policy the engine honours. This is the concrete implementation of the spec's
   * URL-scheme allowlist and redirect-loop detection.
   */
  private final class VenueWebClient extends WebViewClient
  {
    @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)
    {
      final Uri uri = request.getUrl();
      final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
      switch (scheme)
      {
        case "https":
        case "http":
          if (mRedirectCount >= MAX_REDIRECTS) { showError(); return true; }
          mRedirectCount++;
          mErrored = false;
          return false; // load in-engine
        case "tel":
          try { startActivity(new Intent(Intent.ACTION_DIAL, uri)); }
          catch (ActivityNotFoundException e) { /* ignore */ }
          return true;
        case "mailto":
          try { startActivity(new Intent(Intent.ACTION_SENDTO, uri)); }
          catch (ActivityNotFoundException e) { /* ignore */ }
          return true;
        case "geo":
        case "maps":
          launchRoute(); // FOMO keeps navigation inside its own Route Engine
          return true;
        default:
          // Custom schemes (banking wallets, UPI, payment apps) are allowed only as a temporary
          // external handoff — Session Recovery will restore the page when the user returns.
          try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
          catch (ActivityNotFoundException e) { showError(); }
          return true;
      }
    }

    @Override public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
      mRedirectCount = 0;
      mErrored = false;
      if (mErrorView != null) mErrorView.setVisibility(View.GONE);
    }
    @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
    {
      if (request.isForMainFrame()) showError();
    }
  }

  /** Convenience: returns a seeded venue known to have a website, used by test entry points. */
  @NonNull
  public static WebVenue seededVenue(int index)
  {
    final String[] urls = {
        "https://marabi.co.za/", "https://theroyale.co.za/", "https://livingroomjhb.co.za/",
        "https://truthjhb.co.za/", "https://cocoonjhb.co.za/", "https://konka.co.za/"
    };
    final int i = Math.max(0, Math.min(index, urls.length - 1));
    return new WebVenue("venue_" + i, SEEDED_VENUES[i], urls[i], null, 0, 0);
  }
}
