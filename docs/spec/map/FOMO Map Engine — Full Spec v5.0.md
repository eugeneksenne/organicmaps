# FOMO Map Engine — Full Spec v5.1

Foundation: [Organic Maps](https://github.com/organicmaps/organicmaps.git) (native C++ rendering core, OpenStreetMap data).

## ⚠️ Before anything else — two structural issues in the source doc

The uploaded v4.0 plan has two problems worth fixing before it's treated as ground truth:

1. **It's Android-only.** The folder structure (`app/src/main/java/com/example/feature/maps/`), the JNI bridge, and every file referenced (`OrganicMapCanvas.kt`, `OrganicMapsBridge.java`) are Android-specific. There's no iOS path at all, despite FOMO's client being Kotlin Multiplatform with shared logic across Android and iOS. Organic Maps itself already ships a working iOS app built on the same C++ Drape engine, so this isn't a blocker — it means the module needs restructuring into `commonMain`/`androidMain`/`iosMain`, with the iOS side bridging the same native core via Objective-C++ interop instead of JNI. See **Restructured Architecture** below.
2. **The "✅ Completed" status table is a claim, not a verification.** Marking ten complex native-integration items as fully done in a single status table — including a JNI bridge, a C++ render loop, and realtime sync — is exactly the kind of self-reported completion that's worth treating skeptically rather than building on top of blindly. This spec keeps the status column but reframes it as **Claimed / Unverified** until each item has an actual passing build + test to back it up, so the tracker doesn't silently inherit false confidence. The package name `com.example` is itself a tell — that's a default template package, not one that survived a real integration pass.

**v5.1 note:** the actual approved UI design has now been supplied as a screenshot. Several UI sections below were rewritten to match it exactly rather than the prose description in the v4.0 source doc — see the v5.1 changelog at the bottom for the full diff. The most significant correction: the top bar shows the *user's own* FOMO Score, not a "dynamic city status" string — those are two different concepts that the original spec conflated.

---

## Vision

The FOMO Map is the real-world venue discovery engine of the platform — designed to help users discover places worth visiting based on venue quality, proximity, and live venue activity, not to replace generic navigation apps.

> **Help users discover venues.**

Everything else — Flash Drops, live broadcasts, events, creator content, Night Guard, social experiences — is attached to venues or temporarily overlaid when required. Organic Maps provides native mapping infrastructure; FOMO provides intelligence and social experience.

## Foundation

```
OpenStreetMap
      │
Organic Maps Engine (C++ Native, shared by Android + iOS)
      │
──────────────────────────
      │
FomoMaps API Facade (commonMain interface, expect/actual per platform)
      │
──────────────────────────
      │
FOMO Social Intelligence & Realtime Sync (SupabaseMapSync — shared KMP)
      │
──────────────────────────
      │
FOMO Map Experience (Compose on Android, SwiftUI on iOS)
```

Organic Maps provides: native rendering, offline maps, offline routing, offline search, GPS, compass, camera controls, download manager, gestures, POI rendering, battery-efficient location updates, performance optimisations. FOMO extends it rather than replacing it, on both platforms.

## Core Principles

- Venue-first discovery
- Organic Maps remains close to upstream (both platforms)
- One clean interactive native map
- One permanent pin type (Venue Pin)
- Minimal floating controls
- One-handed operation
- Dark-first premium UI
- Offline-first architecture
- Social discovery over navigation
- Privacy by default

---

## FOMO Score Engine *(new in v5.1 — not present in the v4.0 source doc)*

The approved UI surfaces a "FOMO Score" (fire icon + percentage) in two distinct contexts that must not be confused with each other:

| | Personal FOMO Score | Venue FOMO Score |
|---|---|---|
| Where shown | Top bar (user's own) | Venue pins, Nearest Venue Card, carousel cards |
| What it measures | The user's own engagement/activity level on FOMO | How "hot" a specific venue is right now |
| Inputs (indicative — needs product definition, not guessed here) | Check-ins, plans made, events attended, streaks | Live occupancy signals, check-in velocity, Flash Drop activity, trending rate, rating |
| Update cadence | Slower-moving, session/day level | Near-real-time, minute level |

Both are out of scope for this map spec to fully define — the map only needs to *display* them — but the map's data layer should consume them as two clearly separate fields (`userFomoScore` vs `venue.fomoScore`) rather than one shared concept, since the v4.0 doc's "dynamic city status" text implied a single vague signal where the approved design actually shows two well-defined, differently-scoped scores.

---

## Restructured Architecture (Kotlin Multiplatform)

```
feature/maps/
├── src/commonMain/kotlin/com/fomo/maps/
│   ├── api/
│   │   └── FomoMaps.kt                (public facade interface & state — no platform types)
│   ├── layers/
│   │   ├── FomoLayerType.kt            (Venue, Friends, Events, FlashDrops, Creators,
│   │   │                                LiveMoments, Plans, AIRecommendations, Heatmap, Channel)
│   │   ├── FomoLayerManager.kt         (layer registry & toggle manager)
│   │   ├── VenueLayer.kt
│   │   ├── FriendLayer.kt              (delegates presence data to Night Guard's Presence Engine —
│   │   │                                does not reimplement location sharing)
│   │   ├── EventLayer.kt
│   │   ├── FlashDropLayer.kt
│   │   └── HeatmapLayer.kt
│   ├── markers/
│   │   └── FomoMarkerProvider.kt       (marker translation interface, engine-agnostic)
│   ├── camera/
│   │   └── FomoCameraController.kt     (interface — actual framing/animation is platform-specific)
│   ├── routing/
│   │   └── FomoRoutingController.kt    (walking-first nightlife navigation, shared logic)
│   ├── search/
│   │   └── FomoSearchEngine.kt         (unified Organic Maps + FOMO search, shared query logic)
│   ├── offline/
│   │   └── FomoOfflineMapsManager.kt   (Country Pack download orchestration, shared)
│   ├── sync/
│   │   └── FomoMapLayerSyncer.kt       (Supabase Realtime → Layer System, fully shared KMP)
│   └── state/
│       └── FomoMapsState.kt            (UI state representation, shared)
│
├── src/androidMain/kotlin/com/fomo/maps/
│   ├── engine/
│   │   ├── OrganicMapsBridge.kt        (JNI bridge, replaces .java original)
│   │   └── OrganicMapsEngineAndroid.kt (actual FomoMapsImpl — C++ native renderer wrapper)
│   ├── ui/
│   │   ├── OrganicMapCanvas.kt         (SurfaceView render loop, Compose interop)
│   │   ├── MapScreen.kt                (Compose UI, zero WebView references)
│   │   ├── LayerControlOverlay.kt
│   │   └── RouteActiveBanner.kt
│   └── viewmodel/
│       └── FomoMapsViewModel.kt        (androidx ViewModel wrapping shared use cases)
│
└── src/iosMain/kotlin/com/fomo/maps/
    ├── engine/
    │   └── OrganicMapsEngineIOS.kt     (actual FomoMapsImpl — cinterop binding to the same
    │                                      C++ Drape core via an Objective-C++ bridging layer)
    └── ui/
        (SwiftUI views live in the iOS app target, consuming shared KMP state via
         Combine bridge, mirroring MapScreen.kt's Compose structure)
```

The `FomoMaps` facade, layer system, sync, search, and offline orchestration are fully shared. Only the actual native engine binding (`FomoMapsImpl`) and the rendering surface are platform-specific — exactly matching how Night Guard's location/notification `expect`/`actual` split already works elsewhere in the app, so this isn't a new pattern for the codebase.

---

## Screen Layout

```
┌────────────────────────────────────────────┐
│ Status Bar                                 │
├────────────────────────────────────────────┤
│ Floating Top Bar                           │
├────────────────────────────────────────────┤
│ Country Pack Chips                         │
├────────────────────────────────────────────┤
│ Enhanced Nearest Venue Card                │
├────────────────────────────────────────────┤
│                                            │
│        Organic Maps Native Surface         │
│         (C++ Drape Renderer, both OS)      │
│                                            │
│                               ➕ Add Location │
│                               🗂 Layers    │
│                               🛡 Night Guard │
│                               🎯 Recenter  │
├────────────────────────────────────────────┤
│ Route Active Banner (When Navigating)      │
├────────────────────────────────────────────┤
│ Venue Preview Overlay / Friend Details     │
├────────────────────────────────────────────┤
│ Nearby Venue Carousel                      │
└────────────────────────────────────────────┘
```

## Floating Top Bar

**Corrected from the v4.0 source doc**, which described this as a "dynamic city status" text bar (e.g. "🔥 Johannesburg Pulse"). The approved design shows something different: the user's own **personal FOMO Score**, not a city-vibe string. These are two distinct concepts, and the original spec conflated them — see **FOMO Score Engine** below for how the personal score differs from a venue's FOMO Score.

```
[avatar]  FOMO Score          🔍  🔔•  💬•
          🔥 92%          >
```

- **Avatar** — tappable, opens the user's own profile.
- **Personal FOMO Score** — label "FOMO Score" with a fire icon and percentage beneath it, plus a chevron that expands into the score breakdown (see FOMO Score Engine). This is a gamified engagement metric for the *user*, not a venue.
- **Search** — venues, events, friends, users, cities, channels.
- **Notifications** (bell, badge-dot when unread) — Flash Drops, friend activity, event reminders, venue updates, safety alerts.
- **Chats** (speech bubble, badge-dot when unread) — a shortcut into the same Chats tab that also lives in the bottom nav, mirroring how Venue Web's top-nav Share duplicates the bottom-bar Share for one-handed reach; it isn't a second messaging surface.

## Country Pack Chips

Generated dynamically from installed Country Packs — All, 🌙 Nightlife, 🍔 Food, ✨ Prep, ☕ Wellness, ✈ Travel, 🎫 Events. Changing a chip instantly updates visible venue pins, nearest venue card, and nearby carousel — no screen reload, animated transition.

## Enhanced Nearest Venue Card

Beneath the category chips. **Corrected to match the approved design**, which is more detailed than the v4.0 prose description and uses two primary actions with subtitles rather than four icon-only buttons:

```
[hero image, "HOT TONIGHT" ribbon top-left]     92%
                                            🔥 FOMO SCORE
📍 NEAREST VENUE
The Vault ✓
Techno Club

📍 Braamfontein, Johannesburg
🚶 0.1 km away (2 min walk)

🟢 Open Now              🕐 Thu – Sun        >
   Closes 04:00 AM          18:00 – 04:00

★ 4.7 (2,481 reviews)                View Reviews >

🎵 Techno   🍸 Cocktails   🔞 21+

[ 👥 Club Lobby           ]  [ ↗ Route          ]
    Photos, Events & More        Get Directions
```

Fields, corrected against the design:
- **"HOT TONIGHT" ribbon** — a badge on the hero image itself, distinct from the FOMO Score box; shown only when the venue is trending tonight specifically, not just generally popular.
- **Venue FOMO Score box** — top-right, fire icon + percentage, large and separated from the rating line below. This is a different score from the personal one in the top bar — see FOMO Score Engine.
- **Distance + walk time** are one combined line ("0.1 km away (2 min walk)"), not two separate fields.
- **Hours** are a collapsed accordion row (current status + today's closing time on the left, this week's hours on the right) with a chevron to expand full weekly hours — not just a static "Open/Closed" label.
- **Rating row** includes an explicit "View Reviews" link, not just the number.
- **Vibe/amenity tag chips** (e.g. Techno, Cocktails, 21+) sit below the rating — these weren't specified in the v4.0 doc at all.
- **Only two primary actions** appear on this card, each with a title + one-line subtitle, not four icon buttons. Plan and Share are still available, but from the Venue Preview Overlay / venue detail screen, not duplicated here — the nearest venue card optimizes for the two actions someone leaving home or a venue actually needs first.

Adaptive primary action by category (second button is always **Route**):

| Category | Primary action |
|---|---|
| Nightlife | Club Lobby — *Photos, Events & More* |
| Events | Event Lobby — *Photos, Lineup & More* |
| Food / Prep / Wellness | Website — *Menu, Booking & More* |
| Travel | Reserve — *Availability & Booking* |

The **Website** action is the same Venue Web Experience Engine entry point covered in that spec — not a separate implementation.

## Organic Maps Canvas

Center of the screen. Responsibilities: render map tiles natively, camera movement, zoom, rotation, compass, user location dot, offline routing, offline search, gesture handling. FOMO adds social intelligence without modifying the upstream rendering engine, on either platform.

## Venue Pin Philosophy

Every destination on the map is a venue — nightclubs, bars, lounges, restaurants, cafés, hotels, wellness, prep, shopping, attractions, temporary venues. Flash Drops, live broadcasts, and events are contextual badges on venue pins, not separate pin types.

**Corrected pin design**, per the approved UI: pins are **circular photo/avatar-style thumbnails** with a colored ring border and a teardrop pointer beneath, not glyph-icon map pins. Below each pin: venue name and distance, stacked.

```
   ╭───────╮
   │ photo │ 🔥92%   ← FOMO Score badge, top-right of the circle
   ╰───╮───╯
       ▼
   The Vault
   0.1 km
```

## Venue Pin Badges

**Corrected from a bitmask system to match the design**, which shows a single **FOMO Score badge** (fire icon + percentage) as the pin's primary overlay, not a set of enumerated badge glyphs:

- **FOMO Score badge** — fire icon + percentage, present on every venue pin. See FOMO Score Engine below for computation.
- **"LIVE" ribbon** — overrides the FOMO Score badge entirely for pins currently broadcasting live content (e.g. a Live Broadcast pin shows "LIVE" instead of a percentage). A pin shows either its score or LIVE, never both, to keep the badge legible at small map sizes.
- **Ring color** — the approved design uses varied ring colors (purple, red, orange, blue) across pins that don't map cleanly onto either category or score tier in the screenshot provided. Treat ring color as **per-venue accent tinting** (e.g. drawn from the venue's own brand color or a rotating palette) rather than a fixed semantic rule, and confirm the actual intended rule with design before implementation — this is the one visual element in the screenshot that doesn't have an obvious deterministic mapping, so it's flagged rather than guessed at.
- Flash Drop, Event, Trending, and Closing Soon are **surfaced elsewhere** (Nearest Venue Card ribbon, carousel cards, Venue Preview Overlay) rather than as additional pin-level badges — stacking multiple badge types on a small circular pin at map scale isn't legible, which the approved design confirms by showing only one badge per pin.

Only the badge value/state changes via the native update call; the underlying pin (photo, ring) never re-renders from scratch for a badge change.

## Friend Pins

Appear when the Friend Layer is enabled or during active Night Guard location sharing (Walk Me Home, Buddy Pair, Safety Check, shared meeting point, emergency location sharing). **This layer consumes presence data from Night Guard's existing Presence Engine and Buddy Pair Live Presence Engine — it does not run a second, parallel location-sharing pipeline.** When Night Guard ends, friend pins disappear and the map immediately returns to venue-only mode.

## Venue Preview Overlay / Friend Details

Referenced in the screen layout but not previously specified. Appears as a bottom sheet over the map when a venue pin or friend pin is tapped:

**Venue tap:**
```
[hero image]
Venue Name · ✓ Verified
🌙 Nightlife · 🟢 Open until 3:00 AM
★ 4.6 (312)   ·   1.8 km   ·   6 min walk

[ Club Lobby ]  [ Route ]  [ Plan ]  [ Share ]
```

**Friend tap** (only reachable while a Night Guard session is active):
```
[avatar] Sarah
🟢 Walking · 340 m away · Battery 61%

[ Call ]  [ Chat ]  [ Navigate to Sarah ]
```
This mirrors Buddy Pair's Live Presence card exactly rather than introducing a third presence-card design.

## Floating Actions

**Corrected to 4 FABs, matching the approved design** — the v4.0 doc listed 5 (Recenter, Layers, Heatmap, Add Place, Overpass), but the approved UI shows only: ➕ Add Location · 🗂 Layers · 🛡 Night Guard · 🎯 Recenter, top to bottom.

- **Add Location** — bottom sheet for submitting a custom venue. **Goes to a moderation queue, not directly live** — see Moderation below; the v4.0 doc wrote straight to Supabase state, which would let unmoderated submissions appear on every user's map immediately.
- **Layers** — slide-up panel controlling all 10 FOMO layers independently, shown with a small dot badge when any non-default layer is active. **Heatmap is one of these 10 layers (it already was, per the Restructured Architecture layer list), not a separate FAB** — the v4.0 doc listed it as both a standalone button and one of the 10 layers, which was redundant. This spec removes the standalone Heatmap FAB in favor of its existing Layers-panel toggle.
- **Night Guard** — new FAB, elevated to a primary map action rather than buried in a menu. Opens the Night Guard Dashboard directly from the map, which fits the map being the primary surface where Walk Me Home / Buddy Pair / Safety Check are relevant (leaving a venue, walking somewhere).
- **Recenter** — returns camera to user's location or city default.

**Overpass has no FAB at all in the approved design** — which actually confirms the production concern already raised below: Overpass shouldn't be a manual, user-triggered action in the first place. It happens automatically server-side (see Overpass Usage), not via a button a user taps.

### Add Place Moderation *(resolved gap)*

User-submitted venues are the one place this engine accepts unverified third-party content onto the map. Submissions go into a `venue_submissions` table with `status: Pending`, are reviewed (automated basic checks — duplicate detection, profanity/spam filter — plus a light human review queue for anything the automated pass doesn't clearly approve), and only promoted to the live `venues` table once approved. The submitting user sees "Submitted for review" rather than an immediate live pin.

### Overpass Usage *(resolved production concern, and confirmed by the approved design having no Overpass button)*

The original spec calls `SupabaseMapSync.queryOverpassPOIs()` directly against the public Overpass API. The public instance (`overpass-api.de`) has strict rate limits and usage-policy expectations that a production app at scale will hit quickly, and hitting it directly from client devices is also a privacy/reliability risk (client IP exposure, no server-side caching). **Resolved approach:** Overpass queries route through a FOMO Edge Function that caches results server-side (venue-adjacent POI data doesn't change minute-to-minute) and either runs against a self-hosted Overpass instance or a paid Overpass provider once usage crosses a defined threshold — not directly from the client to the public endpoint.

## Nearby Venue Carousel

Above the bottom navigation, titled by active category (e.g. "Nightlife Nearby") with a "See All" link. **Corrected against the approved design** to include two details the v4.0 doc missed:

```
[hero image]        🔥92%
The Vault ✓
Techno Club
★ 4.7 (2.4K)
0.1 km · Open Now

[ 👥 Lobby ]  [ ↗ Route ]
```

- **FOMO Score badge** (fire + percentage) on every card, not just a generic rating — same score as the pin and Nearest Venue Card.
- **Review counts are abbreviated** (2.4K, 1.2K) to fit the card width, distance and open/closed status share one line with the status color-coded (green "Open Now" / orange "Closing Soon").
- **Two quick-action buttons are embedded directly on the card** ("Lobby", "Route") for one-tap access — tapping the card body (not a button) still centers the camera, highlights the pin, and opens the Venue Preview Overlay, exactly as the v4.0 doc described; the buttons are an addition for speed, not a replacement for that behavior.

## Offline Engine

Powered by Organic Maps on both platforms: offline maps (`.mwm` data files), offline routing, offline search, downloaded Country Packs, cached venues, cached favourites, background synchronization.

## Live Synchronization

Online: FOMO synchronizes venue updates, events, Flash Drops, creator broadcasts, and venue check-ins via Supabase Realtime WebSockets — all shared KMP logic, so Android and iOS consume the identical sync pipeline rather than two implementations.

Offline: native rendering continues, downloaded `.mwm` regions remain available, cached venue information remains accessible, changes sync automatically on reconnect.

## Routing & Navigation

Organic Maps handles navigation; FOMO extends it with route-to-venue, route-to-event, route-to-meeting-point, multi-stop nightlife plans, walking-first routing, and the active walk-time banner overlay. **This is the same Route Engine referenced by the Venue Web Experience Engine's Route button and Night Guard's Walk Me Home** — one routing implementation, multiple entry points, not three separate route stacks.

## Security & Privacy

- Authenticate synchronized updates with Supabase JWT
- Validate venue data (including submitted-venue moderation above)
- Encrypt all communications via HTTPS / WebSockets
- Respect user privacy controls
- Friend pins available only during active Night Guard sharing with user consent

---

## Backend Data Model *(additions — most map data already lives in existing `venues`/`flash_drops`/`venue_checkins` tables)*

```
venue_submissions
------------------
id
submittedByUserId
name
category
latitude
longitude
status              -- Pending | Approved | Rejected
reviewedAt
reviewedBy
createdAt

map_overpass_cache
-------------------
regionKey           -- geohash-bucketed region
queryType
resultJson
fetchedAt
expiresAt           -- TTL-based, avoids re-querying Overpass for stable POI data
```

---

## Verification Checklist *(replaces the unverified "✅ Completed" table)*

| Item | Claimed | Verification needed |
|---|---|---|
| Native Organic Maps rendering engine | Android only | Confirm iOS cinterop binding builds against the same C++ target |
| `FomoMaps` facade | Android only | Confirm `commonMain` interface has zero platform-specific types leaking through |
| Engine-agnostic dispatcher | Unverified | Confirm Leaflet fallback path is still needed, or was a bridge to a since-removed WebView approach |
| Engine-agnostic `MapScreen` UI | Android only | iOS SwiftUI equivalent does not yet exist |
| 10 composable layers | Android only | Each layer's `commonMain` logic vs. platform rendering split needs confirming |
| Layer control overlay | Android only | iOS equivalent needed |
| Active navigation banner | Android only | iOS equivalent needed |
| Realtime Supabase sync | Likely shareable as-is | Confirm no Android-specific types in `SupabaseMapSync` |
| Marker coordinate fix | Android only | Re-verify once markers route through shared `FomoMarkerProvider` |
| Camera controller | Android only | iOS framing/animation equivalent needed |

---

## Changes from v4.0 (carried over from v5.0)

- Restructured the entire module from an Android-only package layout (`com.example...`) into proper `commonMain`/`androidMain`/`iosMain` Kotlin Multiplatform, since the original had no iOS path despite FOMO's native KMP architecture.
- Replaced the "✅ Completed" status table with a **Verification Checklist** — the original claimed ten complex native-integration items as fully done with no test or build evidence attached, which is worth treating as a claim rather than ground truth (also flagged by the leftover `com.example` placeholder package name).
- Tied Friend Layer explicitly to Night Guard's existing Presence Engine / Buddy Pair Live Presence Engine rather than describing what read like a second, parallel location-sharing pipeline.
- Resolved a real production risk: Add Place submissions now route through a moderation queue (`venue_submissions`) instead of writing directly to the live venue table.
- Resolved a real production risk: Overpass queries now route through a caching Edge Function rather than hitting the public API directly from client devices.
- Added the previously-unspecified **Venue Preview Overlay / Friend Details** screen, and explicitly reused Buddy Pair's presence card design for the friend-tap case instead of inventing a third card layout.
- Cross-referenced Route Engine and the Venue Web Experience Engine's Website action as shared implementations rather than parallel ones, consistent with how the other three specs in this set already reference shared engines.

## Changes in v5.1 (this revision — reconciled against the approved UI screenshot)

- **Top bar corrected**: replaced the invented "dynamic city status" text concept with what the design actually shows — the user's own personal FOMO Score. Added a new **FOMO Score Engine** section distinguishing personal score from venue score, since the source doc conflated them into one vague signal.
- **Nearest Venue Card rewritten** to match the real layout exactly: "HOT TONIGHT" ribbon, combined distance/walk-time line, expandable hours accordion, "View Reviews" link, vibe/amenity tag chips, and — most substantively — only **two** primary action buttons with subtitles instead of four icon-only buttons (Plan/Share moved to the venue detail surface instead of duplicated here).
- **Venue pins corrected** from glyph-icon pins with bitmask badges to circular photo-thumbnail pins with a single FOMO Score badge (or a "LIVE" ribbon that overrides it) — matching what's actually in the screenshot. Ring color is flagged as needing a real design rule rather than guessed at, since the sample doesn't show an obvious deterministic mapping.
- **Floating Actions reduced from 5 to 4**, matching the design: Add Location, Layers, Night Guard, Recenter. Heatmap is folded back into the Layers panel (it's already one of the 10 layers — having it as a second, standalone FAB was redundant in the original doc). Overpass has no user-facing button at all in the design, which further confirms the v5.0 decision to make it a server-side-only concern.
- **Nearby Venue Carousel** updated to show the FOMO Score badge and the embedded "Lobby"/"Route" quick-action buttons visible on each card in the design, in addition to the existing tap-to-open-overlay behavior.

