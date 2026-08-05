import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };

/**
 * Trusted moment-publication entry point. Called as the authenticated user via the ANON key
 * (so RLS still applies and the moments row is owned by the caller). Validates that:
 *  - the caller is authenticated;
 *  - the venue_id, if provided, points at an approved snapshot (so a client can't attach a moment
 *    to a blocked/unreviewed venue);
 *  - latitude/longitude, if provided, are within the approved venue's detection radius (120 m) so
 *    moments tagged "I'm here" really were captured nearby.
 *
 * On success, inserts the row into public.moments so the ranking worker can fan it out into
 * feed_items for For You / Following / Nearby / Live without the client writing scores directly.
 */
Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  const auth = request.headers.get("Authorization");
  if (!auth) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  // Resolve the calling user using the anon key + bearer token — the user must be authenticated.
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: auth, apikey: Deno.env.get("SUPABASE_ANON_KEY")! } },
  });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return Response.json({ error: "unauthorized" }, { status: 401, headers });

  const payload = await request.json().catch(() => ({}));
  if (!["photo", "video", "live", "template"].includes(payload.kind) || typeof payload.media_path !== "string")
    return Response.json({ error: "invalid_moment" }, { status: 400, headers });

  let venueId = payload.venue_id ?? null;
  let lat = typeof payload.latitude === "number" ? payload.latitude : null;
  let lon = typeof payload.longitude === "number" ? payload.longitude : null;

  if (venueId) {
    const { data: venue } = await supabase
      .from("venue_snapshots")
      .select("venue_id,latitude,longitude,safety_state,live_now")
      .eq("venue_id", venueId)
      .maybeSingle();
    if (!venue || venue.safety_state !== "approved") return Response.json({ error: "venue_not_approved" }, { status: 400, headers });
    if (lat !== null && lon !== null) {
      const meters = distanceMeters(lat, lon, venue.latitude, venue.longitude);
      if (meters > 120) return Response.json({ error: "venue_too_far" }, { status: 400, headers });
    } else {
      lat = venue.latitude;
      lon = venue.longitude;
    }
  }

  const insert: Record<string, unknown> = {
    creator_id: user.id,
    kind: payload.kind,
    visibility: payload.visibility ?? "followers",
    caption: payload.caption ?? null,
    media_path: payload.media_path,
    thumbnail_path: payload.thumbnail_path ?? null,
    venue_id: venueId,
    event_id: payload.event_id ?? null,
    latitude: lat,
    longitude: lon,
  };

  const { data, error } = await supabase.from("moments").insert(insert).select().single();
  if (error) return Response.json({ error: "publish_failed", detail: error.message }, { status: 400, headers });

  // If a venue was attached, seed the invitation row so the Feed's "available now" chip renders.
  if (venueId) {
    await supabase.from("moment_invitations").upsert(
      { moment_id: data.id, venue_id: venueId, state: "active" },
      { onConflict: "moment_id" }
    );
  }

  return Response.json({ moment: data }, { status: 201, headers });
});

// Equirectangular distance; good enough for the 80-120 m sanity check.
function distanceMeters(aLat: number, aLon: number, bLat: number, bLon: number): number {
  const R = 6371000;
  const dLat = (bLat - aLat) * Math.PI / 180;
  const dLon = (bLon - aLon) * Math.PI / 180;
  const lat1 = aLat * Math.PI / 180;
  const lat2 = bLat * Math.PI / 180;
  const x = dLon * Math.cos((lat1 + lat2) / 2);
  return Math.sqrt(x * x + dLat * dLat) * R;
}
