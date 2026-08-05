import "dotenv/config";

const url = process.env.SUPABASE_URL;
const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
const intervalMs = Number(process.env.FEED_RANK_INTERVAL_MS ?? 30_000);
if (!url || !key) throw new Error("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are required");
const headers = { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": "application/json" };

async function query(path) {
  const response = await fetch(`${url}/rest/v1/${path}`, { headers });
  if (!response.ok) throw new Error(`Supabase query failed: ${response.status}`);
  return response.json();
}
async function upsertFeedItem(item) {
  const response = await fetch(`${url}/rest/v1/feed_items?on_conflict=user_id,moment_id,feed_kind`, {
    method: "POST", headers: { ...headers, Prefer: "resolution=merge-duplicates" }, body: JSON.stringify(item)
  });
  if (!response.ok) throw new Error(`Feed upsert failed: ${response.status}`);
}
function recencyScore(publishedAt) {
  const ageHours = Math.max(0, (Date.now() - Date.parse(publishedAt)) / 3_600_000);
  return Math.exp(-ageHours / 12);
}
async function rank() {
  // This worker only ranks approved public Moments. Follow graph, venue proximity, safety,
  // blocks, reports, and engagement velocity must be added before production rollout.
  const [users, moments] = await Promise.all([
    query("profiles?select=id"),
    query("moments?select=id,creator_id,published_at&moderation_state=eq.approved&visibility=eq.public&deleted_at=is.null&order=published_at.desc&limit=200")
  ]);
  for (const user of users) {
    for (const moment of moments) {
      if (moment.creator_id === user.id) continue;
      await upsertFeedItem({ user_id: user.id, moment_id: moment.id, feed_kind: "for_you", score: recencyScore(moment.published_at), reasons: ["recency"], expires_at: new Date(Date.now() + 86_400_000).toISOString() });
    }
  }
  console.log(`ranked ${moments.length} moments for ${users.length} users`);
}
async function run() {
  try { await rank(); } catch (error) { console.error("feed worker failed", error); }
}
await run();
setInterval(run, intervalMs);
