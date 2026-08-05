# FOMO Walk Me Home ¡ª Full Spec v2.0

Walk Me Home shouldn't feel like simple location sharing or turn-by-turn navigation. It should feel like having trusted people quietly walking home with you, even when they aren't physically there.

## Core Mission

Get users safely from where they are to where they want to be ¡ª with intelligent monitoring, trusted companions, and automatic protection.

Not limited to walking. Supports: ? Walking ¡¤ ? Cycling ¡¤ ? Driving ¡¤ ? Ride-share (Uber/Bolt) ¡¤ ? Motorcycle ¡¤ ? Public Transport ¡¤ ? Train ¡¤ ? Ferry (future).

## Architecture

```
Walk Me Home
©À©¤©¤ Journey Engine
©À©¤©¤ Destination Engine
©À©¤©¤ Route Intelligence Engine
©À©¤©¤ Companion Engine
©À©¤©¤ Live Journey Engine
©À©¤©¤ Safety Intelligence Engine
©À©¤©¤ Journey Timeline Engine
©À©¤©¤ Smart Alert Engine
©À©¤©¤ Arrival Engine
©À©¤©¤ Emergency Engine
©À©¤©¤ Recovery Engine
©À©¤©¤ Privacy Engine
©À©¤©¤ Offline Engine
©¸©¤©¤ Analytics Engine
```

## Journey Lifecycle

`Destination ¡ú Choose Companions ¡ú Journey Created ¡ú Journey Starts ¡ú Live Monitoring ¡ú Arrival Detected ¡ú Safe Arrival ¡ú Journey Summary ¡ú Auto Delete`

## Entry Points

Night Guard Dashboard, Floating Map Button, Chat Attachment, Group Chat, Club Lobby, Venue Page, Event Page, Flash Drop, Discover Screen, My Moves.

### Smart Suggestions

Leaving a nightclub, leaving an event, walking after midnight, battery under 15%, walking alone, leaving your Buddy Pair, friends leaving separately. Proactive, not reactive.

---

## Screens

### Screen 1 ¡ª Walk Me Home Home
```
? Walk Me Home
You're not walking alone.

Start Journey
Recent Journeys
Trusted Companions
Saved Destinations
Journey Settings
```

### Screen 2 ¡ª Select Destination
```
Search
? Home   ? Hotel   ? Parking   ? My Car   ? Custom   ? Saved Places

Recent Destinations
Map Pin ¡¤ Venue Exit ¡¤ Club Parking
```
The nearest saved destination appears first. If the user is leaving a venue (detected via Context Engine), the destination list is reordered so **Venue-Aware suggestions** (see Differentiators) rank above generic recents.

### Screen 3 ¡ª Journey Type
Automatically detected; user can override.
```
? Walking   ? Ride Share   ? Driving   ? Cycling   ? Public Transport
```
Each mode adjusts monitoring behaviour ¡ª see **Mode-Specific Monitoring** below, which the original spec referenced ("adjusts monitoring behaviour") without defining.

### Screen 4 ¡ª Choose Trusted Companions
Displays: Trusted Circle, Recent Buddies, Current Buddy Pair, Groups, Partner, Family.
```
Share
? Live Progress   ? ETA   ? Arrival   ? Battery   ? Route
```
If the user is already in an active Buddy Pair, that session's members are pre-selected (not re-asked) ¡ª see **Buddy Handoff**.

### Screen 5 ¡ª Journey Preview
```
Destination   Home
Distance      1.8 km
ETA           18 min
Companions    Sarah, James
Safety        ? Protected

[ Start Journey ]
```

### Screen 6 ¡ª Active Journey
The primary experience. Map occupies most of the screen; UI chrome stays minimal.
```
Walking Home ¡ú Home
©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥ (glowing route, progress-lit)
ETA        12 min
Remaining  1.1 km
Safety     ? Safe

[ Pause ]  [ Call ]  [ Emergency ]  [ End ]
```

### Screen 7 ¡ª Companion View *(what a trusted companion sees, not the user)*
```
Alfred's Journey
Progress: 62%   Distance remaining: 0.7 km   ETA: 9 min
Battery: 41%   Status: ? Walking

[ Call ]  [ Chat ]  [ Navigate to Alfred ]  [ Start Buddy Pair ]  [ Emergency Assist ]
```
Read-only otherwise ¡ª a companion can offer support (Screen 8's quick actions) but can never alter the user's route or settings.

### Screen 8 ¡ª Companion Quick Actions *(overlay on Screen 7, surfaces as floating cards on Screen 6)*
```
? Still with you   ?? Almost there   ? Call Me   ? Meet You   ? Need Help?
```
Sent without interrupting the user's navigation ¡ª appears as a dismissible floating card on Screen 6, never a modal.

### Screen 9 ¡ª Emergency Mode
```
Emergency
©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥©¥
[ Call Trusted Companion ]
[ Emergency SOS ]
[ Share Live Journey ]
[ Emergency Call ]
[ Record Evidence (optional) ]
```
Reachable from Screen 6 in one tap at all times, not nested in a menu.

### Screen 10 ¡ª Arrival
Automatic; no interaction required.
```
Safe Arrival
Welcome Home

Journey Time   18 minutes
Distance       1.8 km
```
Companions receive *"Alfred arrived safely."* Live sharing ends immediately ¡ª not after a delay, since continuing to share post-arrival would contradict the temporary-by-default principle.

### Screen 11 ¡ª Journey Summary
```
Duration        18 min
Distance        1.8 km
Average speed   6 km/h
Companions      Sarah, James
Timeline        [expandable]
Safety events   0
Journey rating  [ 1¨C5 stars, optional ]

[ Save ]  [ Delete ]
```
No permanent route history is kept unless explicitly saved.

### Screen 12 ¡ª Recovery *(shown only if an emergency occurred)*
```
Need assistance?
[ Report Incident ]  [ Block User ]  [ Report Venue ]
[ Contact Trusted Companion ]  [ Contact Authorities ]  [ Submit Anonymous Feedback ]
```

### Screen 13 ¡ª Settings
Auto-suggest Walk Me Home, automatic destination detection, battery alerts, route deviation alerts, stop detection sensitivity, arrival radius, offline behaviour, trusted companion defaults. See **Resolved Defaults** below for shipping values.

---

## Mode-Specific Monitoring

The original spec said each travel mode "adjusts monitoring behaviour" without saying how ¡ª resolved here since it drives real threshold logic in the Route Intelligence Engine:

| Mode | Route deviation sensitivity | Stop detection | Notes |
|---|---|---|---|
| Walking | High (10¨C15 m off-route) | Flags after 90 s stationary | Most sensitive ¡ª a stopped pedestrian at night is the highest-signal anomaly |
| Cycling | Medium (20¨C30 m) | Flags after 60 s stationary | Higher route tolerance for bike-lane routing quirks |
| Driving | Low (uses road-snapped route, not pedestrian path) | Flags after 3 min stationary | Traffic lights and jams are expected; threshold widened accordingly |
| Ride-share | Matches driver's route via Ride Companion integration | N/A ¡ª driver controls stops | Deviation compares against the ride's expected route, not the user's own |
| Public Transport | Route deviation disabled between stops | Flags only if journey exceeds expected transit time by 2x | GPS inside vehicles/tunnels is unreliable; timeline-based monitoring instead |

## Live Map

Displays current location, destination, animated route, progress, trusted companion avatars, meet-up points, emergency markers (if any). Uses FOMO's signature glowing route: completed path glows brighter, remaining path fades.

## Journey Timeline

Private. Example:
```
22:03  Journey Started
22:06  Left Venue
22:09  Crossed Main Street
22:12  Battery 20%
22:18  Destination Nearby
22:20  Arrived Safely
```
Auto-deleted when the journey expires unless explicitly saved (Screen 11).

## Route Intelligence

Detects: wrong direction, unexpected stops, detours, long inactivity, leaving mapped route, destination changes. Automatically recalculates ETA. Thresholds per mode ¡ª see table above.

## Smart Safety Intelligence

Monitors movement, connectivity, battery, speed, journey progress, arrival confidence. Risk increases gradually ¡ª mirrors Night Guard's Risk Intelligence Engine rather than a separate scoring system, so a journey's safety state stays consistent with the rest of the app.

## Smart Alerts

Only meaningful alerts: *"Sarah has started watching your journey." "Battery is below 15%." "You have stopped moving." "You've left the planned route." "Destination updated." "Almost home." "Journey completed."*

## Smart Meet-Up

If companions are nearby: meet halfway, navigate to friend, share live destination. Shares the same Meet-Up mechanics as Buddy Pair rather than a parallel implementation. Excellent for campuses and festivals.

## Ride Companion

When travelling by Uber/Bolt: driver name (optional), vehicle, ETA, route, arrival. Trusted companions monitor the trip until completion.

---

## Resolved Defaults (Settings)

The original spec listed these as configurable without shipping values ¡ª needed for a usable first-run experience:

| Setting | Default |
|---|---|
| Arrival radius | 30 m from destination pin |
| Stop detection sensitivity | Mode-dependent ¡ª see table above |
| Route deviation alert threshold | Mode-dependent ¡ª see table above |
| Battery alert threshold | 15% |
| Offline behaviour | Continue monitoring via last-known location + cached route; show companions "Last update: Xm ago"; auto-resync on reconnect |
| Auto-suggest Walk Me Home | On |
| Automatic destination detection | On, always confirmable before journey starts (never auto-starts without Screen 5 confirmation) |

---

## Backend Data Model

```
journeys
---------
id
userId
status            -- Created | Active | Paused | ArrivalDetected | Completed | Deleted
mode              -- Walking | Cycling | Driving | RideShare | PublicTransport
destinationId
startedAt
endedAt

journey_watchers
----------------
journeyId
userId
permissions       -- liveProgress, eta, arrival, battery, route

journey_updates
---------------
journeyId
latitude
longitude
speed
heading
battery
movementState
updatedAt

journey_events
--------------
journeyId
eventType
metadata
timestamp

journey_alerts
--------------
journeyId
alertType
severity
createdAt

journey_summary
---------------
journeyId
distance
duration
averageSpeed
completedAt
```

## Realtime Stack

- Supabase Realtime
- PostGIS
- OSRM Routing
- Native background location ¡ª Android `FusedLocationProviderClient` / iOS `CoreLocation`, via KMP `expect`/`actual`
- Native push notifications ¡ª Android FCM / iOS APNs, via KMP `expect`/`actual`
- Local encrypted storage (SQLDelight + platform keystore)
- Offline map packs
- Telegram Bot API ¡ª single private channel, bot as admin ¡ª for optional emergency evidence storage

## UX Principles

- Start a journey in under 10 seconds.
- One-handed operation throughout.
- Map-first experience with minimal UI chrome.
- Temporary sharing by default.
- Automatic completion and cleanup.
- Intelligent prompts instead of excessive notifications.
- Consistent visual language with the rest of FOMO.

## Billion-Dollar Differentiators

1. **Venue-Aware Journeys** ¡ª leaving a club, event, or restaurant automatically suggests the most relevant destination (home, parking, hotel, ride pick-up point).
2. **Buddy Handoff** ¡ª if a Buddy Pair session ends because everyone is splitting up, FOMO automatically offers to transition the remaining person into Walk Me Home without creating a new session ¡ª companions and permissions carry over rather than re-prompting Screen 4.
3. **Live Venue Exit Intelligence** ¡ª for large venues, festivals, or stadiums, guide users to the safest exit and then begin the journey from there.
4. **Journey Companion Cards** ¡ª trusted companions can encourage, call, navigate to the user, or switch into Buddy Pair without interrupting the journey (Screens 7¨C8).
5. **Unified Journey Engine** ¡ª walking, ride-sharing, driving, and public transport all use the same underlying journey engine (mode only changes thresholds, not architecture), giving users one consistent experience instead of separate features per travel mode.

---

## Changes from v1.0

- Replaced Expo Background Location / Expo Notifications with native Android/iOS equivalents via KMP `expect`/`actual`, matching FOMO's actual native client architecture.
- Added a **Mode-Specific Monitoring** table resolving what "adjusts monitoring behaviour" (Screen 3) actually means per travel mode ¡ª this was asserted but never defined.
- Numbered and separated **Screen 7 (Companion View)** from **Screen 8 (Companion Quick Actions)** ¡ª the original bundled "Companion Screen" and "Journey Companion Mode" together, but one is a read-only monitoring view and the other is an interaction overlay; conflating them made it unclear where the quick-action cards actually render.
- Resolved all previously-"configurable" values (arrival radius, stop detection sensitivity, battery threshold, offline behaviour) into concrete shipping defaults in a dedicated table.
- Clarified that automatic destination detection never auto-starts a journey without the Screen 5 confirmation step ¡ª important given how much of this spec emphasizes automation; automation should suggest, not act unilaterally, when it comes to sharing a user's live location.
- Tied Smart Safety Intelligence explicitly to Night Guard's shared Risk Intelligence Engine rather than describing a separate scoring system, avoiding two different "safety score" implementations across the platform.

