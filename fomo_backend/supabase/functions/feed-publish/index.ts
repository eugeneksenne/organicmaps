import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };
Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  const auth = request.headers.get("Authorization");
  if (!auth) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: auth } } });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const payload = await request.json();
  if (!['photo', 'video', 'template'].includes(payload.kind) || typeof payload.media_path !== 'string')
    return Response.json({ error: "invalid_moment" }, { status: 400, headers });
  const { data, error } = await supabase.from('moments').insert({ creator_id: user.id, kind: payload.kind, visibility: payload.visibility ?? 'followers', caption: payload.caption ?? null, media_path: payload.media_path, thumbnail_path: payload.thumbnail_path ?? null, venue_id: payload.venue_id ?? null, event_id: payload.event_id ?? null }).select().single();
  if (error) return Response.json({ error: "publish_failed" }, { status: 400, headers });
  return Response.json({ moment: data }, { status: 201, headers });
});
