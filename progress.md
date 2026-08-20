# FOMO Chats — Implementation Progress

_Last reviewed: 2026-08-20_

## Status

**Overall: chats inbox/conversation/groups/stories/calls are repository-backed; not production-ready.**

Shared Kotlin chat logic lives in `shared/chats` and is used by the Android Kotlin data layer. iOS has a UIKit adapter in `iphone/Maps/UI/Chats` that follows the same RPC contract. The client stack is open-source only: Supabase + supabase-kt + OkHttp/Ktor + SQLite cache + LiveKit (WebRTC) + MapLibre. No Stream/Sendbird/Twilio Chat. Encryption, media uploads, LiveKit remote-track rendering, incoming-call push, and ConnectionService are still open. Story privacy is Friends or Only me. Coil/Media3/CameraX are not wired until the media pipeline exists.

The chats screen itself (tray, filters, search, auth, new-chat, pin/mute/leave, conversation send/edit/delete/reply/react, contact/venue/event/location cards) is wired to Supabase RPCs. It is not a 10/10 production messenger.

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
- [x] Implement Android Supabase Auth: sign-up, sign-in, sign-out, and session refresh.
- [ ] Add account deletion and recovery.
- [x] Create profile bootstrap trigger/function after an Auth user is created.
- [ ] Implement follow/friend/block relationships and privacy settings.
- [ ] Finish and test RLS policies for every table and Storage object path.
- [x] Add authorization RPCs for direct conversation creation, group creation, and read receipts.
- [ ] Add invitation, moderation, and administrator workflows.
- [ ] Add rate limits, abuse reporting, audit events, and administrator workflows.

## Realtime engine

- [x] Define Supabase-source-of-truth and Socket.IO-ephemeral event protocol.
- [x] Add authenticated VPS event fanout for typing, low-frequency ephemeral status, delivery/read hints, and WebRTC signaling.
- [x] Add Android Socket.IO transport, lifecycle-aware connection state, exponential reconnect, typing and call-signal listeners, and SQLite durable operation outbox foundation.
- [x] Add authenticated Supabase Edge Function operation queue and Android OkHttp dispatcher for durable queued operations.
- [x] Add public Android build configuration and an engine factory that binds Socket.IO transport to the authenticated Supabase operation dispatcher.
- [x] Connect Android repositories/UI to the engine with local inbox/message cache, optimistic send, and delivery state.
- [x] V1 realtime: delivered receipts, typing labels, busy/in-call presence, connection banners, live hint refresh for edits/reactions/deletes. Push, media, and nightlife V2 layers remain open.
- [ ] Implement presence, attachment, analytics, and notification managers.
- [ ] Add Supabase Realtime subscriptions, presence, database reconciliation cursor, foreground/background handling, and network-change tests.
- [ ] Add OpenTelemetry traces, rate limits, Redis-backed distributed presence, and operational dashboards.

## Direct messaging

- [x] Native chats inbox and direct-message UI flow.
- [x] Initial tables for conversations, members, messages, attachments, and idempotent client operations.
- [x] Add Android data layer and repositories for Supabase REST/RPC.
- [x] Add local SQLite inbox/message cache, drafts, and outbox replay.
- [ ] Add encrypted local database and local search index.
- [x] Implement conversation pagination, optimistic sends, idempotency, retries, pins, and read receipts.
- [x] Add editing, deletion, reply threading, and reactions in the UI (outbox-backed).
- [ ] Add Supabase Realtime subscriptions (Socket.IO hints + polling fallback are live).
- [x] Implement offline operation queue replay and queued/server merge.
- [x] Add composer attachment sheet for venue/event/location/contact cards (camera/gallery/document wait on media pipeline).
- [x] Device contacts: READ_CONTACTS / CNContactStore, picker, share-as-card, and new-chat matching. The address book is never bulk-uploaded.
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
- [x] Text story publish with audience selection (Friends or Only me); photo/video still opens FOMO Camera.
- [x] View tracking, replies into chat, reactions, mute, and delete-own-story RPCs/UI.
- [x] Story privacy is Friends or Only me only; followers/public were removed from UI, RPCs, RLS, and the column check.
- [ ] Add scheduled expiry/deletion worker and storage cleanup.
- [ ] Add moderation/reporting and notification behavior.

## Groups

- [x] Groups list and group-conversation UI entry points.
- [x] Initial conversation member roles in the schema.
- [ ] Implement create-group wizard, membership, owner/admin/moderator permissions, invites, QR/invite links, and revocation.
- [ ] Add group information, shared media, plans, venue voting, events, polls, announcements, and shared locations.
- [ ] Add NightGuard/Buddy Pair permissions and safety workflows.

## Calling

- [x] Calls history and all core call-state UI screens: outgoing, incoming, active voice, active video, group, reconnecting, participant sheet, reply/silence.
- [x] Initial call-session table plus `call_inbox` / `start_chat_call` / `end_chat_call` RPCs.
- [x] Integrate the maintained LiveKit Android WebRTC SDK and add a real room-session engine for microphone/camera publication.
- [x] Implement authenticated Socket.IO signaling and a Supabase Edge Function that issues short-lived LiveKit tokens.
- [x] Wire call lifecycle UI, runtime permission requests, and LiveKit token retrieval (demo UI continues when LiveKit is unconfigured). Remote-track rendering and incoming-call notifications remain open.
- [ ] Provision STUN/TURN infrastructure, credential issuance, ICE validation, regional routing, and observability.
- [ ] Implement incoming-call push notifications, Android ConnectionService/foreground service behavior, lock-screen answer/decline, and call notifications.
- [ ] Implement peer connections, media tracks, echo cancellation, noise suppression, audio routing, Bluetooth/headset behavior, camera switching, PiP, bitrate adaptation, and reconnect logic.
- [ ] Implement group-call SFU infrastructure; peer mesh does not scale for group video.
- [ ] Add call quality telemetry, call history writes, E2EE media design, spam/rate controls, and emergency-call safety policy.
- [ ] Test calls across network transitions, background/foreground, low-end devices, packet loss, and different Android versions.

## Notifications and background work

- [ ] Deploy/configure a self-hosted UnifiedPush-compatible gateway and Android credentials.
- [ ] Implement secure notification payloads for messages, mentions, stories, calls, groups, venue/event updates, and NightGuard alerts.
- [ ] Add batching, mute/mention rules, notification settings, deep links, and delivery telemetry.
- [ ] Add WorkManager jobs for sync, retry, upload, expiry, cleanup, and key rotation.

## Quality, accessibility, and release readiness

- [x] Replace prototype chats inbox/conversation UI with repository-backed state and loading/error/empty states.
- [ ] Add TalkBack labels, keyboard behavior, font scaling, contrast testing, reduced-motion behavior, and localization.
- [ ] Add unit, integration, migration, contract, security, load, and end-to-end tests.
- [ ] Measure launch/open/send/call targets on supported devices and network conditions.
- [ ] Add privacy policy, terms, consent flows, data retention policy, and support/reporting workflows.
- [ ] Conduct production readiness review and staged rollout.

## Profiles

- [x] Add reusable social-profile UI, profile schema fields/statistics view, and Feed/Map profile entry points.
- [ ] Add authenticated profile repository, editing, avatar/media upload, follow/block mutation, profile privacy enforcement, followers/following lists, and Moments/saved-place paging.
- [ ] Connect profile links from Chats, Stories, Calls, Discover people cards, venue/creator surfaces, and push deep links.

## Discover

- [x] Add city-level Dynamic Hero snapshot schema and Android repository with offline UI fallback.
- [x] Add keyless real-time Open-Meteo weather refresh for the Johannesburg Hero.
- [ ] Deploy a trusted city aggregation worker for weather, venue/event/drop counts, energy score, and personalised recommendations.
- [ ] Implement repository-backed Discover sections, distance sorting, filters, See All pages, venue/event/people APIs, and privacy-aware location handoff.

## Feed

- [x] Native media-first Feed UI with tabs and local interaction states.
- [x] Initial Moments, invitations, reactions, comments, and personalised feed-item schema migration.
- [x] Android `FeedRepository` + `FeedBinder`: For You / Following / Nearby / Live, `feed_page` RPC, optimistic like/ripple/save/follow/comment. Unsigned/unconfigured uses labeled demo Moments.
- [x] Server Ripple engine: one Ripple per user, removable, idempotent `set_moment_reaction`, trust weight, velocity, decay, Quiet→Viral bands. Rollup tables for venue / event / city / creator. Clients never score. Invitation card states Active/Ended/Venue Closed. Who's Here is not in Feed or Ripple RPCs.
- [x] Offline Ripple outbox stores desired on/off state and replays `set_moment_reaction`.
- [x] Ranking worker reads server `decayed_score`; Discover / Smart Places / Flash Drops order by public bands. `ripple_snapshot` / `ripple_trending` / own-creator analytics RPCs.
- [ ] Create signed upload/publish workflow from Camera with moderation state transitions.
- [x] Add a self-hosted trusted For You ranking-worker baseline; clients cannot write rank scores.
- [x] Add self-hosted follow/block graph, consented discovery-location, venue safety schema, and ranking branches for Following, Nearby, and Live.
- [ ] Add reports, sponsored-content policy, user-facing privacy controls, cache invalidation, and ranking evaluation before production enablement.
- [x] Add initial authenticated publish and short-lived LiveKit-token Edge Function foundations.
- [x] Add initial live broadcast, device-push, and feed-event schema migration.
- [ ] Add LiveKit ingest/egress/replay pipeline and invitation expiry worker.
- [ ] Implement self-hosted push gateway worker and Android UnifiedPush registration/incoming notification flows.
- [ ] Add moderation, sponsored-content disclosure, reporting, blocking, privacy enforcement, creator analytics UI, and stronger abuse/rate protections (device attestation, farms, impossible travel).
- [ ] Add media CDN/transcode/thumbnail delivery, preload strategy, performance telemetry, and feed-load tests.
- [ ] Club Lobby heat banners, Ripple realtime fanout to other viewers, iOS Feed UI in the Xcode target.

## Current committed foundations

- `3e3b533` — Supabase communications backend foundation.
- `14029d5` — Core Calls UI state screens.
- Earlier commits on this branch — Chats, groups, stories, camera, feed, discover, and map UI work.
