# FOMO Socket.IO realtime gateway

This directory runs the VPS-deployed realtime gateway and a trusted Feed ranking worker. It is **not** the database of record: messages, membership, calls, media, and Feed records stay in self-hosted Supabase/LiveKit.

The `feed-worker` only ranks approved public Moments by recency as a safe baseline. It deliberately does not bypass blocks, follows, proximity, venue safety, reports, or moderation policy; those must be implemented before enabling it for production.

## Setup

1. On the VPS, install Docker/Compose and clone this repository.
2. Copy `.env.example` to `.env`; set the production Supabase URL, server-only service-role key, allowed browser origins, and optional Redis URL.
3. Run `docker compose up -d --build`.
4. Place a TLS reverse proxy (Nginx/Caddy) in front of the gateway. Proxy WebSocket upgrade requests to `127.0.0.1:3001` and expose only HTTPS/WSS publicly.
5. Probe `GET /healthz` from your monitoring system.

## Security constraints

- Android/browser clients authenticate with a short-lived Supabase access token in `socket.auth.accessToken`.
- The service verifies JWTs with Supabase JWKS and verifies conversation membership server-side before joining a room or relaying events.
- Never expose `SUPABASE_SERVICE_ROLE_KEY` to an Android app, browser, repository, logs, or crash report.
- Run two or more instances with Redis configured for horizontal Socket.IO fanout.
- Rate limiting, audit logging, distributed presence, and FCM delivery workers must be deployed alongside this gateway before production launch.
