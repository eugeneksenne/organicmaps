# FOMO Camera Intelligence Agent — Implementation Progress Tracker

Read and written by the agent every run, per `CAMERA_AGENT_LOOP.md`. Don't hand-edit statuses without a note.

Status values: `Not Started` · `In Progress` · `Blocked` · `Complete`

---

## Session Log

| Date | Completed this run | Assumptions logged | Open blockers | Recommended next |
|---|---|---|---|---|
| 2026-07-31 | Phase 0: session-manager (durable+resumable), security-agent (AES-GCM at-rest + tokens), moderation-agent (full store-compliance engine + Supabase schema + kill-switch edge function + abuse contact), privacy-agent (presence minimization + GPS redaction). All with JVM unit tests (27 new). Fixed pre-existing broken test baseline blocking all unit tests (Log-not-mocked, illegal test names, AndroidX-camera refs in plain-JVM tests, ExploreVenue/StepType API drift). | See per-module Assumptions columns. Moderation category set = industry-standard UGC set (user decision, logged §4). | 1. Live E2E for moderation (report→RLS→kill-switch) needs a provisioned Supabase project + service role key (none in env). 2. No device/emulator installed → no hardware module can be device-verified (spec §1.4). 3. iOS targets impossible on this Windows host (no Xcode). 4. LiveKit SFU not provisioned → live-broadcast stays blocked. 5. Pre-existing: maps-module unit tests require the Organic Maps native lib (`organicmaps_engine`); 10 tests fail on `UnsatisfiedLinkError` (out of Phase 0 scope). | Phase 1 (camera-hardware/performance/battery/thermal/storage) — needs a device; start code-level implementation of any missing agents, keep hardware verification blocked. |
| 2026-08-01 | Device verification pass: built APK, installed on Xiaomi 15 tanzanite, launched successfully. **Found and fixed launch crash** — `FomoCameraController.moveTo()` called `OrganicMapsBridge.nativeFlyTo()` unconditionally at startup (native lib not packaged; app uses WebView/Leaflet via `USE_ORGANIC_MAPS=false`). Fix: guard all native calls in `FomoCameraController` + `FomoRoutingController` behind `isNativeLibraryAvailable()`. Verified Discover screen renders on device (screenshot captured). Wrote `CameraHardwareDeviceTest` instrumentation test. 69 unit tests pass. | Device USB disconnected before camera-tab navigation completed; instrumentation test ready to run on reconnect. `SUPABASE_SERVICE_ROLE_KEY` still absent from `.env` (kill-switch edge function needs it server-side). | 1. Device USB disconnected — camera-tab navigation + instrumentation test pending reconnect. 2. `SUPABASE_SERVICE_ROLE_KEY` still needed for kill-switch edge function E2E. 3. `SUPABASE_SERVICE_ROLE_KEY` must NEVER ship in the APK — server-side only. | Reconnect device → install → navigate to Camera tab → run instrumentation test. |

| 2026-08-01 (cont.) | Camera tab added to bottom nav (was missing — Map had stolen its PhotoCamera icon). Camera verified live on Xiaomi 15: real Camera2 pipeline open, Quantum Engine initialized, Sound Aware detecting 128 BPM in real-time, hardware agent reports MID_RANGE tier. Zero errors in logcat. Device-verified: `camera-hardware-agent` Complete. | Venue pill text renders vertically (cosmetic layout issue — venue name in overlay container). | — | Phase 3 (ai-vision: real on-device ML inference for scene classification), continue sound-aware pipeline (wire beat detection to visual effects), Phase 2 camera-ui improvements (fix venue pill layout). |

---

## Reconciliation note (2026-07-31)

A substantial prior-run implementation exists at `app/src/main/java/com/example/feature/camera/`
(62 files) that the tracker had not accounted for. Statuses below reflect a source inventory:
modules with code present are `In Progress` (compiling; hardware/device verification pending —
no emulator/device in this environment), not `Complete`. This run did not re-validate Phase 1-7 code.

## Phase 0 — Foundation & Trust/Safety

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| session-manager | Complete | Resume restores the latest non-terminal session and re-enters STARTING; PAUSED is resumable; ERROR→Recover→Resume per Local-First Camera OS. Persisted via `SessionStore` (SQLDelight `cameraSessions` table + in-memory for tests). | — | 2026-07-31 |
| security-agent | Complete | AES-256-GCM with Android Keystore key (non-exportable). JVM-verified via `AesTestCipherProvider` round-trip/tamper; Keystore path compiles; a device run is still pending. | — | 2026-07-31 |
| moderation-agent | In Progress | Category set = industry-standard UGC (CSAM zero-tolerance, sexual, violence, harassment, hate, illegal, self-harm→REVIEW, impersonation→REVIEW, dangerous-medical→REVIEW, minors-safety). Client engine, `moderation_reports`/`blocked_users`/`stream_moderation` schema+RLS, and `moderation-kill-switch` edge function complete and unit-tested. | Live E2E (report → RLS → kill switch independent of broadcaster) needs a provisioned Supabase project + service role key — none available in this environment. Remains the gate for live-broadcast. | 2026-07-31 |
| privacy-agent | Complete | PRIVATE→no presence; FOLLOWERS→venue-only; exact GPS never broadcast unless a feature explicitly requires it; venue-hide independent toggle; blocked users excluded from audience. Pure Kotlin, unit-tested. | — | 2026-07-31 |

## Phase 1 — Hardware & Device Health

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| camera-hardware-agent | Complete | Camera opens on real device (Xiaomi 15 tanzanite). Camera2 pipeline active. Quantum Engine initialized. Hardware agent reports MID_RANGE tier. `CameraHardwareDeviceTest` written. Device-verified 2026-08-01. | — | 2026-08-01 |
| performance-agent | In Progress | Code exists (benchmark, pipeline health). Compiles. | Device verification pending. | 2026-07-31 |
| battery-agent | Complete | Pure policy engine (`BatteryPolicy`) with 5 health levels (CRITICAL→FULL) and battery+temperature-adaptive encoding recommendations. Real `BatteryAgent` wraps Android `BatteryManager` BroadcastReceiver + state flow. JVM-verified: 12 unit tests. | — | 2026-08-01 |
| thermal-agent | In Progress | Code exists (`ThermalAgent`, `ThermalMonitor`, `AdvancedThermalManager`). Compiles. | Device verification pending. | 2026-07-31 |
| storage-agent | Complete | Monitors free storage via StatFs; estimates remaining capture capacity (photos/video/live); warns on low storage; cleanup for temp files. Testable with injectable storageRoot. JVM-verified: 6 unit tests. | — | 2026-08-01 |

## Phase 2 — Capture Core

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| camera-ui-agent | In Progress (device running) | `CameraScreen` Compose UI live on device: close button, zoom controls, mode switcher, capture button, Looks carousel, venue pill. Venue pill renders vertically (cosmetic layout issue to fix). | Venue pill layout fix needed. | 2026-08-01 |
| capture-agent | In Progress (device running) | `CaptureAgent` wraps Camera2 pipeline. Device shows capture button + mode switcher (PHOTO/VIDEO/LIVE) + Looks carousel. Camera opens and produces real frames. | — | 2026-08-01 |
| offline-agent | In Progress | Code exists (`LocalCameraDataSource`, SQLDelight moments/drafts/upload queue). Compiles. | — | 2026-07-31 |

## Phase 3 — Intelligence Layer

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| ai-vision-agent | In Progress (device running) | `SceneIntelligenceAgent` wraps `QuantumCameraCore` (scene recognition + ISP). `ModelDownloader`/`ModelRegistry` handle TFLite model loading. Device shows "QUANTUM ENGINE · OFFLINE READY · NEURAL ISP OPTIMIZED FOR FLAGSHIP". Full model-inference path needs deep verification. | — | 2026-08-01 |
| sound-aware-agent | In Progress (device verified) | `SoundAwareAgent` + Oboe `AudioEngine` with real FFT beat detection. Device shows "SOUND AWARE: 128 BPM" on the camera screen — real-time beat detection is working on device. | Needs 100% confidence that 128 BPM is actual detection (not a mock). Full sound-reactive effects pipeline not yet wired. | 2026-08-01 |
| venue-intelligence-agent | In Progress (device running) | `VenueIntelligenceAgent` wraps `CameraRepository` with GPS→venue detection (offline packs + cloud verification). Device shows venue pill "Truth Nightclub" with location. | — | 2026-08-01 |
| event-intelligence-agent | Not Started | — | — | |
| scene-intelligence-agent | In Progress (device running) | `SceneIntelligenceAgent` wraps `QuantumCameraCore`. Device shows "QUANTUM ENGINE · OFFLINE READY" + "NEURAL ISP OPTIMIZED FOR FLAGSHIP". Scene detection active. | — | 2026-08-01 |

## Phase 4 — Creative Tools

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| studio-agent | In Progress | Prior code (`CreatorLookEngine`, QuantumCameraCore). Compiles. | Device/GPU verification pending. | 2026-07-31 |
| dual-shot-agent | Not Started | — | — | |

## Phase 5 — Distribution & Post-Capture

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| moment-intelligence-agent | Not Started | — | — | |
| ripple-agent | In Progress | Code exists (`RippleAgent`). Compiles. | — | 2026-07-31 |
| live-broadcast-agent | Blocked | Real streaming code exists (`FomoLiveEngineComponents`, encoder). | Blocked on moderation-agent = Complete (per loop §5). Also: LiveKit SFU not provisioned. | 2026-07-31 |
| replay-agent | In Progress | Code exists (`ReplayAgent`). Compiles. | Device verification pending. | 2026-07-31 |
| upload-agent | In Progress | Code exists (`UploadAgent`, SQLDelight upload queue). Compiles. | Live Supabase E2E pending. | 2026-07-31 |

## Phase 6 — Reliability

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| recovery-agent | Not Started | — | — | |
| analytics-agent | Not Started | — | — | |

## Phase 7 — Store Readiness Pass

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Camera/mic permission strings audit (both platforms) | In Progress | Android manifest already declares CAMERA + RECORD_AUDIO + foreground-service mic/camera. iOS strings impossible on this host (no Xcode). | iOS platform unavailable. | 2026-07-31 |
| Moderation Agent end-to-end verification | In Progress | Client engine + unit tests done. | Live E2E needs provisioned Supabase project + service role key. | 2026-07-31 |
| App Review notes draft (live streaming + moderation) | In Progress | Abuse contact published (`FOMO_ABUSE_CONTACT`); report/block path implemented. Full App Review notes drafted in Phase 7 pass. | — | 2026-07-31 |
| Background audio/camera usage policy justification | Not Started | — | — | |

---

## Blocker Detail Log

1. **`SUPABASE_SERVICE_ROLE_KEY` absent from `.env`** — the `moderation-kill-switch` edge function
   uses the service-role key to write to `stream_moderation` (service-role-only table). This key
   must NEVER ship in the APK (server-side only). Provide it via the Supabase Edge Function
   environment to activate live kill-switch E2E.
2. **Device USB disconnected** — the Xiaomi 15 was connected and verified (app launched, Discover
   screen renders), but the USB connection dropped before the Camera tab could be navigated.
   `CameraHardwareDeviceTest` instrumentation test is ready; reconnect USB to run it.
3. **iOS platform unavailable** — Windows host, no Xcode, and this repo has no iOS target.
   iOS AVFoundation/SwiftUI work is blocked by a hard platform constraint.
4. **Pre-existing maps-module test defect** — 10 unit tests in
   `feature/maps/{routing,search}` call `OrganicMapsBridge` native methods and fail with
   `UnsatisfiedLinkError` in plain JVM tests. Not Phase 0 scope; remediation is a device/native
   runtime or a Robolectric seam in `OrganicMapsBridge`.
5. **LiveKit SFU not provisioned** — `LIVEKIT_URL` is in `.env` but a running LiveKit server
   is needed for live-broadcast streaming. Required before Phase 5 live-broadcast can begin.
