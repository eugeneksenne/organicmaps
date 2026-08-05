# FOMO Chats — Implementation Progress

_Last reviewed: 2026-08-05_

## Status

**Overall: foundation started; not production-ready.**

The Android project now has native prototype screens for Chats, DMs, Calls, Groups, and Stories, plus an initial Supabase data model and local Docker support. The systems below are not yet end-to-end connected, tested, or deployable as a production communications platform.

## Environment and secret policy

- [x] Provide a committed `fomo_backend/.env.example` with placeholders only.
- [x] Ignore the local `fomo_backend/.env` file.
- [x] Document that mobile clients may only use a Supabase URL and anonymous key.
- [ ] Provision development/staging/production Supabase projects.
- [ ] Store service-role, database, MinIO, push-provider, and TURN credentials in deployment secret stores.
- [ ] Define key rotation, backup, incident response, and access-control procedures.

## Local infrastructure

- [x] Docker Compose definition for Redis and MinIO.
- [x] Supabase local project configuration.
- [x] Local database bootstrap/reset and reviewed schema-push scripts.
- [ ] Verify `supabase start`, Docker Compose, migrations, and seed data on a machine with Docker/Supabase CLI.
- [ ] Add health checks, metrics, structured logs, backups, and alerting.
- [ ] Build CI jobs for migration validation, container scans, tests, and deployment.

## Identity and authorization

- [x] Initial profile schema and Row Level Security enablement.
- [ ] Implement Android Supabase Auth: sign-up, sign-in, sign-out, session refresh, account deletion, and recovery.
- [ ] Create profile bootstrap trigger/function after an Auth user is created.
- [ ] Implement follow/friend/block relationships and privacy settings.
- [ ] Finish and test RLS policies for every table and Storage object path.
- [ ] Add authorization RPCs for direct conversation creation, group management, invitations, moderation, and read receipts.
- [ ] Add rate limits, abuse reporting, audit events, and administrator workflows.

## Realtime engine

- [x] Define Supabase-source-of-truth and Socket.IO-ephemeral event protocol.
- [x] Add authenticated VPS event fanout for typing, low-frequency ephemeral status, delivery/read hints, and WebRTC signaling.
- [ ] Implement Android connection/realtime/presence/sync/delivery/retry managers and connect them to the protocol.
- [ ] Add Supabase Realtime subscriptions, presence, database reconciliation cursor, foreground/background handling, and network-change tests.
- [ ] Add OpenTelemetry traces, rate limits, Redis-backed distributed presence, and operational dashboards.

## Direct messaging

- [x] Native chats inbox and direct-message UI flow.
- [x] Initial tables for conversations, members, messages, attachments, and idempotent client operations.
- [ ] Add Android data layer and repositories for Supabase REST/RPC and Realtime.
- [ ] Add encrypted local database, local search index, drafts, and migration strategy.
- [ ] Implement conversation pagination, optimistic sends, idempotency, retries, editing, deletion, reply threading, reactions, pins, and read/delivery receipts.
- [ ] Add realtime subscriptions for messages, typing, presence, edits, reactions, and membership.
- [ ] Implement offline operation queue replay and conflict handling.
- [ ] Add composer attachment bottom sheet, camera/gallery/document/location/contact integrations, and upload progress.
- [ ] Add rich native cards for venues, events, routes, Moments, profiles, tickets, and Flash Drops.

## Encryption and security

- [ ] Select and document a vetted end-to-end encryption protocol/library (do not invent cryptography).
- [ ] Implement device registration, prekeys, session establishment, group sender keys, rotation, verification, and recovery.
- [ ] Encrypt local message/media data at rest using Android Keystore-backed keys.
- [ ] Encrypt media keys separately from message content.
- [ ] Perform independent security review, threat modeling, penetration testing, and privacy assessment before launch.

## Media pipeline

- [x] Initial attachment metadata schema and local MinIO development service.
- [ ] Create restricted Supabase Storage buckets and object-level authorization policies.
- [ ] Implement resumable/background uploads, checksums, retries, cancellation, and cleanup.
- [ ] Add image compression, video transcode/thumbnail generation, audio waveform generation, and HEIC compatibility path.
- [ ] Add malware scanning, content moderation, lifecycle retention, CDN configuration, and deletion workflows.

## Stories

- [x] Stories inbox and viewer UI.
- [x] Initial story/story-view schema with expiry timestamp.
- [ ] Add story create/upload/publish flow from FOMO Camera.
- [ ] Add audience/privacy selection, view tracking, replies, reactions, mute/block behavior, and viewer lists.
- [ ] Add scheduled expiry/deletion worker and storage cleanup.
- [ ] Add moderation/reporting and notification behavior.

## Groups

- [x] Groups list and group-conversation UI entry points.
- [x] Initial conversation member roles in the schema.
- [ ] Implement create-group wizard, membership, owner/admin/moderator permissions, invites, QR/invite links, and revocation.
- [ ] Add group information, shared media, plans, venue voting, events, polls, announcements, and shared locations.
- [ ] Add NightGuard/Buddy Pair permissions and safety workflows.

## Calling

- [x] Calls history and all core call-state UI screens: outgoing, incoming, active voice, active video, and group state.
- [x] Initial call-session table.
- [ ] Choose and integrate a maintained Android WebRTC SDK.
- [ ] Implement a dedicated authenticated signaling service; Supabase Realtime alone is not a complete call-signaling solution.
- [ ] Provision STUN/TURN infrastructure, credential issuance, ICE validation, regional routing, and observability.
- [ ] Implement incoming-call push notifications, Android ConnectionService/foreground service behavior, lock-screen answer/decline, and call notifications.
- [ ] Implement peer connections, media tracks, echo cancellation, noise suppression, audio routing, Bluetooth/headset behavior, camera switching, PiP, bitrate adaptation, and reconnect logic.
- [ ] Implement group-call SFU infrastructure; peer mesh does not scale for group video.
- [ ] Add call quality telemetry, call history writes, E2EE media design, spam/rate controls, and emergency-call safety policy.
- [ ] Test calls across network transitions, background/foreground, low-end devices, packet loss, and different Android versions.

## Notifications and background work

- [ ] Select/configure push provider and Android credentials.
- [ ] Implement secure notification payloads for messages, mentions, stories, calls, groups, venue/event updates, and NightGuard alerts.
- [ ] Add batching, mute/mention rules, notification settings, deep links, and delivery telemetry.
- [ ] Add WorkManager jobs for sync, retry, upload, expiry, cleanup, and key rotation.

## Quality, accessibility, and release readiness

- [ ] Replace prototype/static UI content with repository-backed state and loading/error/empty states.
- [ ] Add TalkBack labels, keyboard behavior, font scaling, contrast testing, reduced-motion behavior, and localization.
- [ ] Add unit, integration, migration, contract, security, load, and end-to-end tests.
- [ ] Measure launch/open/send/call targets on supported devices and network conditions.
- [ ] Add privacy policy, terms, consent flows, data retention policy, and support/reporting workflows.
- [ ] Conduct production readiness review and staged rollout.

## Feed

- [x] Native media-first Feed UI with tabs and local interaction states.
- [x] Initial Moments, invitations, reactions, comments, and personalised feed-item schema migration.
- [ ] Implement Android feed repository, paging, cache, optimistic interactions, and realtime delta updates.
- [ ] Create signed upload/publish workflow from Camera with moderation state transitions.
- [ ] Implement a trusted ranking worker for For You, Following, Nearby, and Live feeds; clients must not write rank scores.
- [x] Add initial authenticated publish and short-lived LiveKit-token Edge Function foundations.
- [x] Add initial live broadcast, device-push, and feed-event schema migration.
- [ ] Add follow graph, venue/event integration, LiveKit ingest/egress/replay pipeline, and invitation expiry worker.
- [ ] Implement FCM delivery worker and Android FCM registration/incoming notification flows.
- [ ] Add moderation, sponsored-content disclosure, reporting, blocking, privacy enforcement, analytics, and abuse/rate protections.
- [ ] Add media CDN/transcode/thumbnail delivery, preload strategy, performance telemetry, and feed-load tests.

## Current committed foundations

- `3e3b533` — Supabase communications backend foundation.
- `14029d5` — Core Calls UI state screens.
- Earlier commits on this branch — Chats, groups, stories, camera, feed, discover, and map UI work.
