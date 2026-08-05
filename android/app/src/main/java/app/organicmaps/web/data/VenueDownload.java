package app.organicmaps.web.data;

import androidx.annotation.NonNull;
import java.io.File;

/** A file (ticket / receipt / menu / booking confirmation) downloaded during a Venue Web session. */
public final class VenueDownload
{
  public enum Kind { Ticket, Receipt, Invoice, Menu, BookingConfirmation }

  @NonNull public final String downloadId;
  @NonNull public final String venueId;
  @NonNull public final String venueName;
  @NonNull public final String title;
  @NonNull public final Kind kind;
  @NonNull public final File file;
  public final long createdAt;

  public VenueDownload(@NonNull String downloadId, @NonNull String venueId, @NonNull String venueName,
                       @NonNull String title, @NonNull Kind kind, @NonNull File file, long createdAt)
  {
    this.downloadId = downloadId;
    this.venueId = venueId;
    this.venueName = venueName;
    this.title = title;
    this.kind = kind;
    this.file = file;
    this.createdAt = createdAt;
  }
}
