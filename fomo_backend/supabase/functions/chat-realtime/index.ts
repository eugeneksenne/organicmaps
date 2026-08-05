import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };

Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  const authorization = request.headers.get("Authorization");
  if (!authorization) return Response.json({ error: "unauthorized" }, { status: 401, headers: corsHeaders });
  const client = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: authorization } } });
  const { data: { user }, error } = await client.auth.getUser();
  if (error || !user) return Response.json({ error: "unauthorized" }, { status: 401, headers: corsHeaders });
  return Response.json({ userId: user.id, realtime: "subscribe to private conversation channels" }, { headers: corsHeaders });
});
