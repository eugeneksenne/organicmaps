# FOMO communications backend foundation

This directory contains the versioned data model for Supabase and local supporting services.

## Local development

1. Install Docker and the Supabase CLI.
2. Copy `.env.example` to `.env` and set non-placeholder development secrets.
3. Run `supabase start` from this directory to boot the local Supabase stack.
4. Run `docker compose --env-file .env up -d` for Redis and MinIO.
5. Apply schema changes with `supabase db reset`, or run `./scripts/local-db.sh` to start and reset the local database in one command.

## Promoting schema changes

Supabase migrations under `supabase/migrations/` are the portable database source of truth. After a reviewed backup and migration plan, set `SUPABASE_PROJECT_REF` only in the uncommitted `.env` and run `./scripts/push-schema.sh`. This runs `supabase db push`; it does not copy local development user data or secrets to production.

Do not put production credentials in `.env`; use the Supabase secret store and your deployment platform's secret manager. The Android application must only contain the Supabase URL and anonymous key, never a service-role key, database URL, TURN credential, object-store secret, LiveKit API secret, or FCM service-account JSON.

## Open-source deployment policy

This project is configured to work without paid APIs: self-host Supabase, LiveKit, Socket.IO, Valkey, MinIO, and PostgreSQL on your own infrastructure. LiveKit Cloud and hosted Supabase are optional conveniences, not requirements. The local Compose stack uses Valkey rather than Redis to retain an open-source cache/queue runtime. For Android push delivery, use a self-hosted UnifiedPush-compatible gateway such as ntfy where device support allows it; FCM is not required by this repository.

## Feed and live integrations

`functions/feed-publish` is the authenticated moment-publication entry point. `functions/livekit-token` issues short-lived LiveKit room tokens after Supabase authentication. Before deployment, set the LiveKit and push-gateway values from `.env.example` as Supabase Function secrets. push delivery, media moderation/transcoding, ranking, replay egress, and scheduled expiry remain trusted-worker responsibilities and are intentionally not run in an Edge Function request.

## Scope

The migration creates the server-of-record schema, RLS boundaries, direct/group conversations, messages, attachments, stories, call records, and a durable client-operation outbox. It is a backend foundation, not a claim that encryption, media scanning, WebRTC TURN, push delivery, or moderation are production-complete.
