# FOMO Buddy Pair — Full Spec v2.0

Unlike apps such as Life360, which focus on persistent family tracking, Buddy Pair is event-based, consent-driven, and temporary. It exists only for the duration of a night out, festival, trip, or journey, then automatically disappears.

## Core Mission

Stay together. Stay informed. Stay safe.

Buddy Pair enables trusted people to temporarily share their live presence, safety status, and journey during social activities without creating a permanent tracking relationship.

## Architecture

```
Buddy Pair
├── Pair Invitation Engine
├── Session Engine
├── Live Presence Engine
├── Group Pair Engine
├── Meet-Up Engine
├── Lost Friend Engine
├── Emergency Engine
├── Smart Alerts Engine
├── Privacy Engine
├── Offline Sync Engine
└── Analytics Engine
```

## Lifecycle

`Invite → Accept → Session Created → Live Presence → Smart Monitoring → Session Complete → Auto Delete`

Everything is temporary. No permanent tracking.

## Entry Points

Chat attachment menu, Group chat, Map floating action, Event page, Venue page, Night Guard Dashboard, Friend profile.

---

## Screens

### Screen 1 — Buddy Pair Home
```
🤝 Buddy Pair

No Active Sessions
Stay connected with trusted friends
during your night out.

＋ Pair With Friend
👥 Start Group Pair
📅 Scheduled Sessions
🕒 Recent Sessions

Trusted Circle — 6 Friends
```
If a session is active, the empty state is replaced by a live status card:
```
🤝 Active Session
Sarah, James
Remaining: 2h 14m
[ View Session → ]
```
Only one active session card shows at a time — if the user is in more than one session (e.g. a 1:1 pair nested inside a group pair), the most recently active one surfaces here, with a badge count for the rest.

### Screen 2 — Pair Invitation
*"Select one or more people."*
- Search: Friends, Recent chats, Trusted Circle
- Duration: 30 min · 1 hour · 2 hours · Until Event Ends · Until I Leave Venue · Custom
- Optional message (prefilled suggestion: *"Let's stay together tonight."*)
- Send

### Screen 3 — Invitation Received
```
🤝 Buddy Pair Request
Alfred wants to pair with you.

Duration: Until Event Ends
Message: "Let's stay together tonight."

[ Accept ]  [ Decline ]
```
Declining is silent — the sender sees "Alfred didn't respond" rather than an explicit rejection, avoiding social friction in a group setting.

### Screen 4 — Waiting
```
Invitation Sent
Waiting for Sarah...
[ Cancel ]
```
Expires after **5 minutes** by default (configurable in Settings, 1–30 min range) — long enough to allow for a bar/venue's spotty signal, short enough that the sender isn't left hanging all night.

### Screen 5 — Session Dashboard
The control center.
```
🤝 Festival Crew
Status: 🟢 Active
Time Remaining: 2h 16m

Members
🟢 Alfred
🟢 Sarah
🟡 James

[ Open Map ]  [ Chat ]  [ Call ]  [ End Session ]  [ Emergency ]
```

### Screen 6 — Group Pair Setup *(new)*
Creating a Group Pair is distinct enough from a 1:1 invite to need its own flow rather than overloading Screen 2:
- Group name (freeform, with suggested presets: Festival Crew, Birthday Group, Road Trip, University Friends)
- Add members (same picker as Screen 2, multi-select, 2–20 people)
- Group leader toggle (optional — if set, that member gets member-removal and session-extension authority; if skipped, all members have equal control)
- Duration (same options as Screen 2)
- Create Group

### Screen 7 — Live Map *(new — the primary interaction surface)*
Purpose-built for social coordination, not a generic map with pins:
- Your avatar, buddy avatars, meet-up point, venue marker, optional walking routes
- Distance-to-buddy list overlay, sorted nearest-first:
  ```
  Sarah   18 m away
  James   120 m away
  Peter   450 m away
  [ Navigate to Sarah ]
  ```
- Floating action buttons: **Meet Here**, **Find Friend**, **Emergency**

### Screen 8 — Meet-Up *(new)*
Triggered by "Meet Here" from Screen 7. Creates a temporary rendezvous point; every member receives:
```
📍 Meet-Up
[ Open map ]
ETA: 6 min
Arrived: 2/4
Countdown: 14:00
```
The point auto-expires (default 30 min after creation, or when all members arrive, whichever is first) and is removed from the map — it's a moment, not a permanent pin.

### Screen 9 — Find Friend (Lost Friend) *(new)*
Triggered by "Find Friend." Full-screen focused view, since this is used in high-stress crowded moments and shouldn't compete with other map clutter:
- Map zooms and centers between the two users
- Direction arrow + distance + estimated walk time
- Large "Call" and "Navigate" actions pinned to the bottom

### Screen 10 — Privacy Controls *(new)*
Elevated from a settings list to its own screen since it's changed mid-session, not just at setup:
```
Share
☑ Live location   ☑ Battery   ☑ ETA   ☑ Arrival   ☑ Movement

Hide
☐ Exact location (shares general area only)   ☐ Battery   ☐ Route
```
Changes apply immediately and are reflected to other members within one realtime tick — no "save" step, since a privacy change is exactly the kind of action that should never feel delayed.

### Screen 11 — End Session / Summary *(new)*
Closes the loop the original timeline implied but never gave a screen to:
```
Session Ended
Festival Crew · 21:03–23:50 (2h 47m)

[ Save Summary ]  [ Discard ]
```
Deleted automatically unless the user explicitly saves it — consistent with the "temporary by default" principle running through the rest of Night Guard.

---

## Live Presence Engine

Each participant displays: profile photo, movement, battery, connection, status.
States: 🟢 Walking · 🟢 At Venue · 🟢 Dancing · 🚕 In Ride · 🏠 Home · 🟡 Idle · 🔴 Offline

## Smart Distance Awareness

Distance-to-buddy shown in meters, not raw coordinates, with a one-tap "Navigate to [name]" action — see Screen 7.

## Meet-Up Engine

One tap ("Meet Here") creates a temporary rendezvous point. See Screen 8. Ideal for festivals, clubs, malls, sporting events.

## Lost Friend Engine

One tap ("Find Friend") highlights the selected friend: map zooms, calculates route, shows distance, direction arrow, estimated walk time. See Screen 9. One of the most valuable additions for crowded nightlife environments.

## Group Pair

Supports 2–20 people (hard cap enforced server-side to keep realtime presence channels performant), optional group leader, individual privacy controls, live member list. See Screen 6.

## Smart Alerts

Only notify when meaningful: *"Sarah has arrived." "James left the venue." "Peter disconnected." "Sarah's battery is low." "James is 500 m away." "Everyone reached the meet-up point."*

## Session Timeline

Private to participants:
```
21:03  Buddy Pair Started
21:08  Sarah Arrived
21:20  James Joined
22:04  Meet-Up Created
22:18  Everyone Together
23:50  Session Ended
```
Deleted when the session expires unless explicitly saved (Screen 11).

## Communication Hub

Within the session, without leaving it: group chat shortcut, voice call, video call, share photo, share live moment, emoji reaction, "I'm Here" quick reply.

## Session Controls

Participants can: pause sharing, hide precise location temporarily, extend session, invite another friend, remove themselves, end session, leave silently.

## Privacy Controls

See Screen 10. Applies immediately, per-participant.

## Smart Auto Actions

Automatically: end at destination, end after event, end after timer, extend if everyone agrees, prompt if users separate significantly, prompt Walk Me Home when leaving together, offer Safety Check if someone continues alone.

## Emergency Integration

Emergency button always visible: SOS, call participant, share emergency location, open Walk Me Home, start live emergency session, notify Trusted Circle.

## Offline Behaviour

If internet drops, show last location, last update, connection status. Automatically resync when online.

---

## Backend Data Model

```
buddy_sessions
--------------
id
creatorId
type              -- OneToOne | Group
status            -- Created | Active | Paused | Ended | Deleted
createdAt
expiresAt
endedAt

buddy_members
--------------
sessionId
userId
role              -- Member | Leader
permissions
joinedAt
leftAt

buddy_presence
--------------
sessionId
userId
latitude
longitude
heading
speed
battery
movementState
connectionState
updatedAt

meetup_points
--------------
id
sessionId
latitude
longitude
title
expiresAt         -- default createdAt + 30 min

buddy_events
--------------
sessionId
eventType
timestamp
metadata

emergency_events
----------------
sessionId
triggerType
resolvedAt
```

## Realtime Stack

- Supabase Realtime for session state and presence
- PostGIS for location and proximity calculations
- OSRM for walking routes and friend navigation
- Native background location — Android `FusedLocationProviderClient` / iOS `CoreLocation`, via KMP `expect`/`actual` — for efficient updates
- Native push notifications — Android FCM / iOS APNs, via KMP `expect`/`actual` — for smart alerts
- Local encrypted storage (SQLDelight + platform keystore) for offline resilience

## Production UX Principles

- Join a Buddy Pair in under 10 seconds.
- Never require more than three taps for common actions.
- Make the map the primary interaction surface.
- Keep sessions temporary and privacy-first.
- Prioritize meaningful alerts over constant notifications.
- Seamlessly integrate with Walk Me Home, Safety Check, Calls, Chats, Events, and Venues.

## Why This Is a Flagship Feature

This transforms Buddy Pair from simple live location sharing into a real-time social coordination platform. Meet-Up Engine, Lost Friend Engine, smart distance awareness, session intelligence, adaptive privacy controls, and deep integration with the rest of Night Guard make it feel purpose-built for nightlife and events — festivals, clubs, concerts, campus life, and nights out — rather than a copy of existing family-tracking apps.

---

## Changes from v1.0

- Replaced Expo Background Location / Expo Notifications with native Android (`FusedLocationProviderClient`, FCM) / iOS (`CoreLocation`, APNs) via KMP `expect`/`actual`, matching FOMO's actual native client architecture.
- Resolved the invitation timeout to a concrete default (5 min, configurable 1–30 min) instead of leaving it open-ended.
- Added six screens the original spec implied but didn't mock: Group Pair Setup, Live Map, Meet-Up, Find Friend, Privacy Controls, and End Session/Summary — each pulled out because it has distinct interaction needs (crowded/high-stress use for Find Friend, mid-session realtime edits for Privacy Controls, closing the loop for End Session).
- Made group size cap (2–20) an explicit server-enforced limit rather than just a UI suggestion, since it bounds realtime channel/presence load.
- Clarified declined invitations fail silently to the sender ("didn't respond") rather than showing an explicit rejection, reducing social friction in group contexts.
- Specified meet-up point default expiry (30 min or all-arrived, whichever first) since the original left this open.

