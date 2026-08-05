import { AccessToken } from "npm:livekit-server-sdk@2";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };
Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  const auth = request.headers.get("Authorization");
  if (!auth) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: auth } } });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const { room, publish = false } = await request.json();
  if (typeof room !== "string" || !/^[a-zA-Z0-9_-]{1,128}$/.test(room)) return Response.json({ error: "invalid_room" }, { status: 400, headers });
  const token = new AccessToken(Deno.env.get("LIVEKIT_API_KEY")!, Deno.env.get("LIVEKIT_API_SECRET")!, { identity: user.id });
  token.addGrant({ roomJoin: true, room, canPublish: Boolean(publish), canSubscribe: true });
  return Response.json({ url: Deno.env.get("LIVEKIT_URL"), token: await token.toJwt() }, { headers });
});
