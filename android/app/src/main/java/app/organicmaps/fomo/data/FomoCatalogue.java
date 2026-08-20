package app.organicmaps.fomo.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for the FOMO social/nightlife catalogue used across Camera, Feed,
 * Discover, Channels, Closing Now, and the Venue Web Experience Engine. All screens read
 * venues, friends, live events, and flash drops from here so no module hardcodes display
 * strings or venue identifiers.
 *
 * <p>In production this is populated from the Supabase backend (city_discover_snapshots +
 * venue_snapshots + moment_invitations + friend graph). Until those repositories are wired,
 * this class carries the curated Johannesburg seed set so all social surfaces bind to the
 * same entity record.
 */
public final class FomoCatalogue
{
  private static FomoCatalogue sInstance;

  @NonNull public final List<Venue> venues;
  @NonNull public final List<FriendMarker> friends;
  @NonNull public final List<FlashDrop> flashDrops;
  @NonNull public final List<LiveStream> liveStreams;
  @NonNull public final List<NightBriefItem> nightBrief;

  private FomoCatalogue()
  {
    venues = Collections.unmodifiableList(buildVenues());
    friends = Collections.unmodifiableList(buildFriends());
    flashDrops = Collections.unmodifiableList(buildFlashDrops());
    liveStreams = Collections.unmodifiableList(buildLiveStreams());
    nightBrief = Collections.unmodifiableList(buildBrief());
  }

  public static synchronized FomoCatalogue get()
  {
    if (sInstance == null) sInstance = new FomoCatalogue();
    return sInstance;
  }

  @Nullable
  public Venue venueById(@NonNull String id)
  {
    for (Venue v : venues) if (v.id.equals(id)) return v;
    return null;
  }

  @Nullable
  public Venue venueByName(@NonNull String name)
  {
    final String needle = name.toLowerCase();
    for (Venue v : venues)
      if (v.name.toLowerCase().contains(needle) || needle.contains(v.name.toLowerCase()))
        return v;
    return null;
  }

  @NonNull
  public List<Venue> closingSoon(int maxMinutes)
  {
    final List<Venue> out = new ArrayList<>();
    final long now = System.currentTimeMillis();
    for (Venue v : venues)
    {
      if (v.closesAtMs <= 0) continue;
      final long remaining = v.closesAtMs - now;
      if (remaining > 0 && remaining <= maxMinutes * 60_000L) out.add(v);
    }
    return out;
  }

  @NonNull
  public List<Venue> trendingTonight()
  {
    final List<Venue> out = new ArrayList<>();
    for (Venue v : venues) if (v.energyPercent >= 75) out.add(v);
    return out;
  }

  // ------- Seed data (curated Johannesburg nightlife catalogue) -------
  @NonNull
  private static List<Venue> buildVenues()
  {
    final long now = System.currentTimeMillis();
    return Arrays.asList(
        new Venue("venue_marabi",       "Marabi Club",      "Live Music",   "Maboneng, Johannesburg",       -26.2050, 28.0610, "https://marabi.co.za/",       null,    4.7f, 1.1f, 94, VenueState.PEAK,     false, now + 18L * 60_000L, "Kitchen closes in 17 min", "DBN Gogo starts in 40 min."),
        new Venue("venue_konka",        "Konka",            "Nightclub",    "Soweto, Johannesburg",         -26.2473, 27.8765, "https://konka.co.za/",        null,    4.6f, 1.2f, 92, VenueState.PEAK,     true,  now + 999L * 60_000L, "Peak energy",               "Konka is filling rapidly."),
        new Venue("venue_madison",      "Madison",          "Lounge",       "Sandton, Johannesburg",        -26.1143, 28.0520, null,                          null,    4.4f, 0.9f, 78, VenueState.STEADY,   false, now + 30L * 60_000L, "Shorter queues tonight",    "Madison has shorter queues."),
        new Venue("venue_booth",        "Booth",           "Nightclub",    "Sandton, Johannesburg",        -26.1090, 28.0530, "https://boothjhb.co.za/",     null,    4.5f, 2.1f, 84, VenueState.GAINING,  true,  now + 45L * 60_000L, "Gaining momentum",          "Your friends are heading to Booth."),
        new Venue("venue_ayepyep",      "Ayepyep",          "Nightclub",    "Midrand, Johannesburg",        -25.9988, 28.1268, "https://ayepyeplifestyle.co.za/", null, 4.3f, 3.4f, 62, VenueState.WARMUP,   false, now + 60L * 60_000L, "Warming up",                "Ayepyep matches your music taste."),
        new Venue("venue_truth",        "Truth Nightclub",  "Nightclub",    "Sandton, Johannesburg",        -26.1076, 28.0567, "https://truthjhb.co.za/",     null,    4.4f, 2.0f, 88, VenueState.GAINING,  true,  now + 999L * 60_000L, "Amapiano tonight",          "Afrohouse dominates tonight."),
        new Venue("venue_cocoon",       "Cocoon Nightclub", "Nightclub",    "Sandton, Johannesburg",        -26.1120, 28.0490, "https://cocoonjhb.co.za/",    null,    4.3f, 2.0f, 88, VenueState.LIVE,     true,  now + 4L * 60_000L,  "Final entry in 4 min",      "Cocoon final entry soon."),
        new Venue("venue_vault",        "The Vault",        "Lounge",       "Braamfontein, Johannesburg",   -26.1932, 28.0311, "https://thevaultjhb.co.za/",  null,    4.5f, 1.4f, 81, VenueState.LIVE,     true,  now + 999L * 60_000L, "Live",                      "Friends at The Vault."),
        new Venue("venue_living_room",  "The Living Room",  "Rooftop",      "Maboneng, Johannesburg",       -26.2035, 28.0603, "https://livingroomjhb.co.za/", null,   4.5f, 0.8f, 70, VenueState.LAST_CALL,false, now + 9L * 60_000L,  "Last orders being called",  "Rooftop last orders."),
        new Venue("venue_royale",       "The Royale",       "Cocktail Bar", "Rosebank, Johannesburg",       -26.1451, 28.0422, "https://theroyale.co.za/",    null,    4.8f, 2.6f, 76, VenueState.LAST_CALL,false, now + 22L * 60_000L, "Last cocktails in 22 min",  "Last cocktails."),
        new Venue("venue_rosebank",     "Rosebank Social",  "Bar",          "Rosebank, Johannesburg",       -26.1456, 28.0430, null,                          null,    4.2f, 3.1f, 58, VenueState.WARMUP,   false, now + 32L * 60_000L, "Doors closing shortly",     "Rosebank is gaining momentum."),
        new Venue("venue_artivist",     "Artivist",         "Bar/Kitchen",  "Braamfontein, Johannesburg",   -26.1930, 28.0320, "https://artivistjhb.co.za/",  null,    4.4f, 1.1f, 65, VenueState.STEADY,   false, now + 999L * 60_000L, "Open late",                 null)
    );
  }

  @NonNull
  private static List<FriendMarker> buildFriends()
  {
    return Arrays.asList(
        new FriendMarker("Sipho",  "S", "Inside Konka"),
        new FriendMarker("Neo",    "N", "Heading to Madison"),
        new FriendMarker("Lerato", "L", "Watching Live"),
        new FriendMarker("Nomsa",  "N", "At The Vault"),
        new FriendMarker("Alfred", "A", "At Booth"),
        new FriendMarker("Kagiso", "K", "Nearby")
    );
  }

  @NonNull
  private static List<FlashDrop> buildFlashDrops()
  {
    final long now = System.currentTimeMillis();
    return Collections.singletonList(
        new FlashDrop("venue_konka", "FREE ENTRY", "Konka", now + 12L * 60_000L)
    );
  }

  @NonNull
  private static List<LiveStream> buildLiveStreams()
  {
    return Arrays.asList(
        new LiveStream("venue_konka",  "Konka",       "Soweto",       1400, 94),
        new LiveStream("venue_cocoon", "Cocoon",      "Sandton",      820,  88),
        new LiveStream("venue_vault",  "The Vault",   "Braamfontein", 612,  81),
        new LiveStream("venue_truth",  "Truth",       "Sandton",      520,  86)
    );
  }

  @NonNull
  private static List<NightBriefItem> buildBrief()
  {
    return Arrays.asList(
        new NightBriefItem("DBN Gogo starts in 40 min."),
        new NightBriefItem("Konka is filling rapidly."),
        new NightBriefItem("Madison has shorter queues."),
        new NightBriefItem("Rosebank is gaining momentum."),
        new NightBriefItem("Your friends are heading to Booth.")
    );
  }

  // ------- Domain models -------
  public enum VenueState { PEAK, STEADY, GAINING, WARMUP, LIVE, LAST_CALL }

  public static final class Venue
  {
    @NonNull public final String id;
    @NonNull public final String name;
    @NonNull public final String category;
    @NonNull public final String address;
    public final double latitude;
    public final double longitude;
    @Nullable public final String websiteUrl;
    @Nullable public final String phone;
    public final float rating;
    public final float distanceKm;
    public final int energyPercent;
    @NonNull public final VenueState state;
    public final boolean liveNow;
    public final long closesAtMs;
    @NonNull public final String smartClosingMessage;
    @Nullable public final String briefLine;

    public Venue(@NonNull String id, @NonNull String name, @NonNull String category, @NonNull String address,
                 double lat, double lon, @Nullable String websiteUrl, @Nullable String phone,
                 float rating, float distanceKm, int energy, @NonNull VenueState state, boolean live,
                 long closesAtMs, @NonNull String closingMessage, @Nullable String briefLine)
    {
      this.id = id; this.name = name; this.category = category; this.address = address;
      this.latitude = lat; this.longitude = lon; this.websiteUrl = websiteUrl; this.phone = phone;
      this.rating = rating; this.distanceKm = distanceKm; this.energyPercent = energy;
      this.state = state; this.liveNow = live; this.closesAtMs = closesAtMs;
      this.smartClosingMessage = closingMessage; this.briefLine = briefLine;
    }

    public long minutesRemaining()
    {
      if (closesAtMs <= 0) return Long.MAX_VALUE;
      return Math.max(0, (closesAtMs - System.currentTimeMillis()) / 60_000L);
    }

    @NonNull
    public String liveBadge()
    {
      switch (state)
      {
        case PEAK:      return "🔥 Popular";
        case LAST_CALL: return "🍸 Last Drinks";
        case LIVE:      return liveNow ? "● LIVE" : "● LIVE";
        case WARMUP:    return "☕ Last Coffee";
        case GAINING:   return "🍔 Last Orders";
        default:        return "✨ Open";
      }
    }

    public int ringColor()
    {
      final long m = minutesRemaining();
      if (m <= 5)  return 0xFFFF4D6C;
      if (m <= 10) return 0xFFE5533D;
      if (m <= 20) return 0xFFFF8A3D;
      return 0xFFFFB06A;
    }
  }

  public static final class FriendMarker
  {
    @NonNull public final String name;
    @NonNull public final String initial;
    @NonNull public final String status;
    public FriendMarker(@NonNull String name, @NonNull String initial, @NonNull String status)
    { this.name = name; this.initial = initial; this.status = status; }
  }

  public static final class FlashDrop
  {
    @NonNull public final String venueId;
    @NonNull public final String title;
    @NonNull public final String venueName;
    public final long endsAtMs;
    public FlashDrop(@NonNull String venueId, @NonNull String title, @NonNull String venueName, long endsAtMs)
    { this.venueId = venueId; this.title = title; this.venueName = venueName; this.endsAtMs = endsAtMs; }
  }

  public static final class LiveStream
  {
    @NonNull public final String venueId;
    @NonNull public final String venueName;
    @NonNull public final String area;
    public final int viewers;
    public final int energy;
    public LiveStream(@NonNull String venueId, @NonNull String venueName, @NonNull String area, int viewers, int energy)
    { this.venueId = venueId; this.venueName = venueName; this.area = area; this.viewers = viewers; this.energy = energy; }
  }

  public static final class NightBriefItem
  {
    @NonNull public final String line;
    public NightBriefItem(@NonNull String line) { this.line = line; }
  }
}
