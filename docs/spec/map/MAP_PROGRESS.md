# FOMO Maps — Migration & Implementation Progress Tracker

Read and written by the agent every run, per `MAP_AGENT_LOOP.md`. Don't hand-edit statuses without a note.

Status values: `Not Started` · `In Progress` · `Blocked` · `Complete`

Each item requires **both** Android and iOS to be done before `Complete` — use `In Progress` with a note like "Android done, iOS pending" rather than marking complete on partial platform coverage.

---

## Session Log

| Date | Completed this run | Assumptions logged | Open blockers | Recommended next |
|---|---|---|---|---|
| — | *(no runs yet)* | — | — | Start with Phase 1 (Study) |

---

## Phase 1 — Study (both platforms)

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Architecture & rendering pipeline (Android JNI + iOS cinterop equivalent) | Not Started | | | |
| Startup / lifecycle | Not Started | | | |
| Search subsystem | Not Started | | | |
| Routing subsystem | Not Started | | | |
| Downloads / offline region management | Not Started | | | |
| Location, camera, gestures | Not Started | | | |
| Bookmarks, overlays, storage | Not Started | | | |

## Phase 2 — Replace Renderer

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Organic Maps as default renderer — Android | Not Started | | | |
| Organic Maps as default renderer — iOS | Not Started | | | |
| Remove/retire legacy WebView-Leaflet fallback | Not Started | | | |

## Phase 3 — Migrate Application Shell

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Branding replacement (both platforms) | Not Started | | | |
| Navigation / entry points (both platforms) | Not Started | | | |
| Menus & settings (both platforms) | Not Started | | | |
| UI components migrated to FOMO design (both platforms) | Not Started | | | |

## Phase 4 — FOMO Maps Layer (per FOMO_Map_Engine_Spec_v5.1.md)

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| FomoMaps commonMain facade | Not Started | | | |
| FomoMarkerProvider | Not Started | | | |
| FomoCameraController | Not Started | | | |
| FomoSearchEngine (unified search) | Not Started | | | |
| FomoRoutingController (nightlife extensions) | Not Started | | | |
| FomoOfflineMapsManager | Not Started | | | |
| FomoMapLayerSyncer (Supabase Realtime) | Not Started | | | |
| Layer: Venue | Not Started | | | |
| Layer: Friend (consumes Night Guard Presence Engine) | Not Started | | | |
| Layer: Event | Not Started | | | |
| Layer: Flash Drop | Not Started | | | |
| Layer: Live Moments | Not Started | | | |
| Layer: Creator | Not Started | | | |
| Layer: Plan | Not Started | | | |
| Layer: AI Recommendation | Not Started | | | |
| Layer: Heatmap (accessible via Layers panel only, no standalone FAB) | Not Started | | | |
| Layer: Channel | Not Started | | | |
| Floating Top Bar (personal FOMO Score) | Not Started | | | |
| Country Pack Chips | Not Started | | | |
| Enhanced Nearest Venue Card | Not Started | | | |
| Venue Preview Overlay / Friend Details | Not Started | | | |
| Floating Actions (Add Location, Layers, Night Guard, Recenter) | Not Started | | | |
| Nearby Venue Carousel | Not Started | | | |
| Add Location moderation queue | Not Started | | | |
| Overpass caching Edge Function | Not Started | | | |

## Phase 5 — License Compliance

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| OSM attribution visible in map UI | Not Started | | | |
| Organic Maps (Apache 2.0) + OSS notices in licenses screen | Not Started | | | |
| `UPSTREAM_CHANGES.md` established and current | Not Started | | | |

## Phase 6 — Documentation

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| `MAP_ARCHITECTURE.md` | Not Started | | | |
| `FOMO_MAPS_ARCHITECTURE.md` | Not Started | | | |
| `ORGANIC_MAPS_MIGRATION.md` | Not Started | | | |
| `ORGANIC_MAPS_INTEGRATION.md` | Not Started | | | |

## Phase 7 — Store Readiness Pass

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Location permission strings (both platforms) | Not Started | | | |
| Offline region download storage disclosure | Not Started | | | |
| OSM/OSS attribution confirmed in store-submission build | Not Started | | | |
| No leftover Organic Maps branding/icons/strings in shipped UI | Not Started | | | |

---

## Blocker Detail Log

*(none yet)*
