import "dotenv/config";

const url = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
const intervalMs = Number(process.env.FEED_RANK_INTERVAL_MS ?? 30_000);
if (!url || !key) throw new Error("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are required");
const headers = { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" };
async function query(path) { const response = await fetch(`${url}/rest/v1/${path}`, { headers }); if (!response.ok) throw new Error(`Supabase query failed: ${response.status}`); return response.json(); }
async function upsert(item) { const response = await fetch(`${url}/rest/v1/feed_items?on_conflict=user_id,moment_id,feed_kind`, { method: "POST", headers: { ...headers, Prefer: "resolution=merge-duplicates" }, body: JSON.stringify(item) }); if (!response.ok) throw new Error(`Feed upsert failed: ${response.status}`); }
function recency(publishedAt) { return Math.exp(-Math.max(0, (Date.now() - Date.parse(publishedAt)) / 3_600_000) / 12); }
function distanceKm(a, b) { const rad = value => value * Math.PI / 180; const dLat = rad(b.latitude - a.latitude), dLon = rad(b.longitude - a.longitude); const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLon / 2) ** 2; return 6371 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h)); }
async function rank() {
  const [users, moments, follows, blocks, locations, venues, live] = await Promise.all([
    query("profiles?select=id"), query("moments?select=id,creator_id,published_at,venue_id&moderation_state=eq.approved&visibility=eq.public&deleted_at=is.null&order=published_at.desc&limit=200"), query("user_follows?select=follower_id,followee_id"), query("user_blocks?select=blocker_id,blocked_id"), query("user_discovery_locations?select=user_id,latitude,longitude,consented,expires_at"), query("venue_snapshots?select=venue_id,latitude,longitude,safety_state"), query("live_broadcasts?select=moment_id&state=eq.live")
  ]);
  const following = new Map(), blocked = new Map(), location = new Map(), venue = new Map(), liveIds = new Set(live.map(row => row.moment_id));
  for (const row of follows) (following.get(row.follower_id) ?? following.set(row.follower_id, new Set()).get(row.follower_id)).add(row.followee_id);
  for (const row of blocks) (blocked.get(row.blocker_id) ?? blocked.set(row.blocker_id, new Set()).get(row.blocker_id)).add(row.blocked_id);
  for (const row of locations) if (row.consented && Date.parse(row.expires_at) > Date.now()) location.set(row.user_id, row);
  for (const row of venues) if (row.safety_state === "approved") venue.set(row.venue_id, row);
  for (const user of users) for (const moment of moments) {
    if (moment.creator_id === user.id || blocked.get(user.id)?.has(moment.creator_id)) continue;
    const expires_at = new Date(Date.now() + 86_400_000).toISOString();
    await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "for_you", score: recency(moment.published_at), reasons: ["recency"], expires_at });
    if (following.get(user.id)?.has(moment.creator_id)) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "following", score: recency(moment.published_at), reasons: ["follow"], expires_at });
    const userLocation = location.get(user.id), venueLocation = venue.get(moment.venue_id);
    if (userLocation && venueLocation) { const km = distanceKm(userLocation, venueLocation); if (km <= 50) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "nearby", score: recency(moment.published_at) + (1 / (1 + km)), reasons: ["distance", "recency"], expires_at }); }
    if (liveIds.has(moment.id)) await upsert({ user_id: user.id, moment_id: moment.id, feed_kind: "live", score: recency(moment.published_at), reasons: ["live"], expires_at });
  }
  console.log(`ranked ${moments.length} approved moments for ${users.length} users`);
}
async function run() { try { await rank(); } catch (error) { console.error("feed worker failed", error); } }
await run(); setInterval(run, intervalMs);
