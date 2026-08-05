import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };
const allowed = new Set(["message", "reaction", "receipt", "draft", "story_view", "presence"]);
Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  const authorization = request.headers.get("Authorization");
  if (!authorization) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const client = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, { global: { headers: { Authorization: authorization } } });
  const { data: { user } } = await client.auth.getUser();
  if (!user) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  const operation = await request.json();
  if (typeof operation.id !== "string" || !allowed.has(operation.type) || typeof operation.payload !== "object")
    return Response.json({ error: "invalid_operation" }, { status: 400, headers });
  const { error } = await client.from("client_operations").upsert({ id: operation.id, user_id: user.id, operation_type: operation.type, payload: operation.payload, status: "queued" }, { onConflict: "id" });
  if (error) return Response.json({ error: "queue_failed" }, { status: 400, headers });
  return Response.json({ accepted: true }, { status: 202, headers });
});
