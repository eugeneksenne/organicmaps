package app.organicmaps.widget.placepage.online;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Map;

/** Provider-labelled, session-only place details. Do not persist provider content. */
public final class OnlinePlaceDetails
{
  @NonNull public final String provider;
  @Nullable public final String providerId;
  @Nullable public final String name;
  @Nullable public final String address;
  @Nullable public final String website;
  @Nullable public final String phone;
  @Nullable public final String hours;
  @Nullable public final Double rating;
  @Nullable public final String sourceUrl;
  @NonNull public final List<String> photoUrls;
  @NonNull public final Map<String, String> socialLinks;

  public OnlinePlaceDetails(@NonNull String provider, @Nullable String providerId, @Nullable String name,
                            @Nullable String address, @Nullable String website, @Nullable String phone,
                            @Nullable String hours, @Nullable Double rating, @Nullable String sourceUrl,
                            @NonNull List<String> photoUrls, @NonNull Map<String, String> socialLinks)
  {
    this.provider = provider;
    this.providerId = providerId;
    this.name = name;
    this.address = address;
    this.website = website;
    this.phone = phone;
    this.hours = hours;
    this.rating = rating;
    this.sourceUrl = sourceUrl;
    this.photoUrls = photoUrls;
    this.socialLinks = socialLinks;
  }

  public OnlinePlaceDetails(@NonNull String provider, @Nullable String providerId, @Nullable String name,
                            @Nullable String address, @Nullable String website, @Nullable String phone,
                            @Nullable String hours, @Nullable Double rating, @Nullable String sourceUrl)
  {
    this(provider, providerId, name, address, website, phone, hours, rating, sourceUrl,
         java.util.Collections.emptyList(), java.util.Collections.emptyMap());
  }

}
