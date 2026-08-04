# FOMO communications backend foundation

This directory contains the versioned data model for Supabase and local supporting services.

## Local development

1. Install Docker and the Supabase CLI.
2. Copy `.env.example` to `.env` and set non-placeholder development secrets.
3. Run `supabase start` from this directory to boot the local Supabase stack.
4. Run `docker compose --env-file .env up -d` for Redis and MinIO.
5. Apply schema changes with `supabase db reset`.

Do not put production credentials in `.env`; use the Supabase secret store and your deployment platform's secret manager. The Android application must only contain the Supabase URL and anonymous key, never a service-role key, database URL, TURN credential, or object-store secret.

## Scope

The migration creates the server-of-record schema, RLS boundaries, direct/group conversations, messages, attachments, stories, call records, and a durable client-operation outbox. It is a backend foundation, not a claim that encryption, media scanning, WebRTC TURN, push delivery, or moderation are production-complete.
