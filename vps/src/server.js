import "dotenv/config";
import http from "node:http";
import express from "express";
import { Server } from "socket.io";
import { createClient } from "redis";
import { createAdapter } from "@socket.io/redis-adapter";
import { createRemoteJWKSet, jwtVerify } from "jose";

const required = ["SUPABASE_URL", "SUPABASE_SERVICE_ROLE_KEY"];
for (const name of required) if (!process.env[name]) throw new Error(`Missing ${name}`);
const origins = (process.env.ALLOWED_ORIGINS ?? "").split(",").map(value => value.trim()).filter(Boolean);
if (process.env.NODE_ENV === "production" && origins.length === 0) throw new Error("ALLOWED_ORIGINS is required in production");
const jwks = createRemoteJWKSet(new URL(`${process.env.SUPABASE_URL}/auth/v1/.well-known/jwks.json`));
const app = express();
app.disable("x-powered-by");
app.get("/healthz", (_request, response) => response.status(200).json({ status: "ok" }));
const server = http.createServer(app);
const io = new Server(server, {
  transports: ["websocket", "polling"],
  cors: { origin: origins, methods: ["GET", "POST"], credentials: false },
  maxHttpBufferSize: 1_000_000,
  pingInterval: 25_000,
  pingTimeout: 20_000
});

async function isConversationMember(userId, conversationId) {
  if (!/^[0-9a-f-]{36}$/i.test(conversationId)) return false;
  const url = new URL(`${process.env.SUPABASE_URL}/rest/v1/conversation_members`);
  url.searchParams.set("select", "conversation_id");
  url.searchParams.set("conversation_id", `eq.${conversationId}`);
  url.searchParams.set("user_id", `eq.${userId}`);
  url.searchParams.set("left_at", "is.null");
  const response = await fetch(url, { headers: { apikey: process.env.SUPABASE_SERVICE_ROLE_KEY, Authorization: `Bearer ${process.env.SUPABASE_SERVICE_ROLE_KEY}` } });
  return response.ok && (await response.json()).length > 0;
}

io.use(async (socket, next) => {
  try {
    const token = socket.handshake.auth?.accessToken;
    if (typeof token !== "string" || token.length > 8_000) return next(new Error("unauthorized"));
    const { payload } = await jwtVerify(token, jwks, { issuer: `${process.env.SUPABASE_URL}/auth/v1`, audience: "authenticated" });
    if (typeof payload.sub !== "string") return next(new Error("unauthorized"));
    socket.data.userId = payload.sub;
    next();
  } catch { next(new Error("unauthorized")); }
});

io.on("connection", socket => {
  const userRoom = `user:${socket.data.userId}`;
  socket.join(userRoom);

  socket.on("conversation:join", async ({ conversationId }, acknowledge = () => {}) => {
    if (!(await isConversationMember(socket.data.userId, conversationId))) return acknowledge({ error: "forbidden" });
    await socket.join(`conversation:${conversationId}`);
    acknowledge({ ok: true });
  });
  socket.on("conversation:leave", ({ conversationId }) => socket.leave(`conversation:${conversationId}`));
  socket.on("typing:update", async ({ conversationId, isTyping }) => {
    if (await isConversationMember(socket.data.userId, conversationId)) socket.to(`conversation:${conversationId}`).emit("typing:update", { userId: socket.data.userId, isTyping: Boolean(isTyping) });
  });
  // Persistence happens via authenticated Supabase/RPC. This gateway only fans out already-persisted events.
  socket.on("message:published", async ({ conversationId, message }) => {
    if (await isConversationMember(socket.data.userId, conversationId)) socket.to(`conversation:${conversationId}`).emit("message:published", message);
  });
  // WebRTC SDP/ICE is relayed only after membership authorization; media never traverses this server.
  socket.on("call:signal", async ({ conversationId, targetUserId, signal }) => {
    if (typeof targetUserId === "string" && await isConversationMember(socket.data.userId, conversationId)
        && await isConversationMember(targetUserId, conversationId))
      io.to(`user:${targetUserId}`).emit("call:signal", { fromUserId: socket.data.userId, conversationId, signal });
  });
  socket.on("disconnect", () => {});
});

if (process.env.REDIS_URL) {
  const publisher = createClient({ url: process.env.REDIS_URL });
  const subscriber = publisher.duplicate();
  await Promise.all([publisher.connect(), subscriber.connect()]);
  io.adapter(createAdapter(publisher, subscriber));
}
server.listen(Number(process.env.PORT ?? 3001), "0.0.0.0", () => console.log(`FOMO realtime gateway listening on ${process.env.PORT ?? 3001}`));
