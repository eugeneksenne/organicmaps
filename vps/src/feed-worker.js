import "dotenv/config";

const url = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
const intervalMs = Number(process.env.FEED_RANK_INTERVAL_MS ?? 30_000);
if (!url || !key) throw new Error("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are required");
const headers = { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" };
async function query(path) { const response = await fetch(`${url}/rest/v1/${path}`, { headers }); if (!response.ok) throw new Error(`Supabase query failed: ${response.status}`); return response.json(); }
async function rpc(name, body = {}) {
  const response = await fetch(`${url}/rest/v1/rpc/${name}`, { method: "POST", headers, body: JSON.stringify(body) });
  if (!response.ok) throw new Error(`rpc ${name} failed: ${response.status}`);
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
async function upsert(item) { const response = await fetch(`${url}/rest/v1/feed_items?on_conflict=user_id,moment_id,feed_kind`, { method: "POST", headers: { ...headers, Prefer: "resolution=merge-duplicates" }, body: JSON.stringify(item) }); if (!response.ok) throw new Error(`Feed upsert failed: ${response.status}`); }
function recency(publishedAt) { return Math.exp(-Math.max(0, (Date.now() - Date.parse(publishedAt)) / 3_600_000) / 12); }
function distanceKm(a, b) { const rad = value => value * Math.PI / 180; const dLat = rad(b.latitude - a.latitude), dLon = rad(b.longitude - a.longitude); const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLon / 2) ** 2; return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h)); }
function rippleBoost(pulse, venueHeat) {
  // Scores come from the Ripple engine. This worker only reads them.
  return Number(pulse?.decayed_score ?? 0) * 0.05
    + Number(pulse?.ripple_hour ?? 0) * 0.15
    + Number(pulse?.ripple_count ?? 0) * 0.01
    + Number(venueHeat?.decayed_score ?? 0) * 0.02;
}
async function rank() {
  try { await rpc("decay_ripple_momentum"); } catch (error) { console.warn("ripple decay skipped", error.message); }
  const [users, moments, follows, blocks, locations, venues, live, momentum, venueMomentum] = await Promise.all([
    query("profiles?select=id"),
    query("moments?select=id,creator_id,published_at,venue_id,event_id&moderation_state=eq.approved&visibility=eq.public&deleted_at=is.null&order=published_at.desc&limit=200"),
    query("user_follows?select=follower_id,followee_id"),
    query("user_blocks?select=blocker_id,blocked_id"),
    query("user_discovery_locations?select=user_id,latitude,longitude,consented,expires_at"),
    query("venue_snapshots?select=venue_id,latitude,longitude,safety_state"),
    query("live_broadcasts?select=moment_id,viewer_count,state&state=eq.live"),
    query("moment_momentum?select=moment_id,ripple_count,ripple_hour,tonight_count,decayed_score,band"),
    query("venue_momentum?select=venue_id,decayed_score,band,tonight_count")
  ]);
  const following = new Map(), blocked = new Map(), location = new Map(), venue = new Map(), liveRows = new Map();
  const heat = new Map(momentum.map(row => [row.moment_id, row]));
  const venueHeat = new Map(venueMomentum.map(row => [row.venue_id, row]));
  for (const row of follows) (following.get(row.follower_id) ?? following.set(row.follower_id, new Set()).get(row.follower_id)).add(row.followee_id);
  for (const row of blocks) (blocked.get(row.blocker_id) ?? blocked.set(row.blocker_id, new Set()).get(row.blocker_id)).add(row.blocked_id);
  for (const row of locations) if (row.consented && Date.parse(row.expires_at) > Date.now()) location.set(row.user_id, row);
  for (const row of venues) if (row.safety_state === "approved") venue.set(row.venue_id, row);
  for (const row of live) liveRows.set(row.moment_id, row);
  for (const user of users) for (const moment of moments) {
    if (moment.creator_id === user.id || blocked.get(user.id)?.has(moment.creator_id)) continue;
    const expires_at = new Date(Date.now() + 86_400_000).toISOString();
    const pulse = heat.get(moment.id);
    const place = venueHeat.get(moment.venue_id);
    const boost = rippleBoost(pulse, place);
    const reasons = pulse?.band && pulse.band !== "quiet" ? ["recency", "ripple"] : ["recency"];
    await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "for_you", score: recency(moment.published_at) + boost, reasons, expires_at });
    if (following.get(user.id)?.has(moment.creator_id)) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "following", score: recency(moment.published_at), reasons: ["follow"], expires_at });
    const userLocation = location.get(user.id), venueLocation = venue.get(moment.venue_id);
    if (userLocation && venueLocation) { const km = distanceKm(userLocation, venueLocation); if (km <= 50) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "nearby", score: recency(moment.published_at) + (1 / (1 + km)) + boost * 0.25, reasons: ["distance", "recency"], expires_at }); }
    if (liveRows.has(moment.id)) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "live", score: recency(moment.published_at) + boost + Number(liveRows.get(moment.id)?.viewer_count ?? 0) * 0.001, reasons: pulse?.band && pulse.band !== "quiet" ? ["live", "ripple"] : ["live"], expires_at });
  }
  console.log(`ranked ${moments.length} approved moments for ${users.length} users`);
}
async function run() { try { await rank(); } catch (error) { console.error("feed worker failed", error); } }
await run(); setInterval(run, intervalMs);
