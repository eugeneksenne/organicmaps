import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const headers = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, content-type" };

/**
 * Issues a short-lived signed upload URL the Android camera client uses to PUT the captured JPEG
 * to the `moments` Storage bucket. Clients must then send the returned media_path to
 * `feed-publish` so the moderation + ranking pipeline sees the moment.
 */
Deno.serve(async request => {
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  const auth = request.headers.get("Authorization");
  if (!auth) return Response.json({ error: "unauthorized" }, { status: 401, headers });
  // Auth resolution uses the ANON key — we never expose a service key to clients.
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: auth } },
  });
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return Response.json({ error: "unauthorized" }, { status: 401, headers });

  const payload = await request.json().catch(() => ({}));
  const isVideo = payload.kind === "video";
  const extension = isVideo ? "mp4" : "jpg";
  const contentType = isVideo ? "video/mp4" : "image/jpeg";
  const path = `${user.id}/${Date.now()}.${extension}`;

  const { data, error } = await supabase.storage.from("moments").createSignedUploadUrl(path);
  if (error || !data?.signedUrl) return Response.json({ error: "upload_url_failed" }, { status: 500, headers });
  return Response.json({ upload_url: data.signedUrl, token: data.token, media_path: path, content_type: contentType }, { status: 200, headers });
});
