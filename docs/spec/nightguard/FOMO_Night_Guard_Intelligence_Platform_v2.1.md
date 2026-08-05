# FOMO Night Guard Intelligence Platform v2.1

Night Guard is a real-time safety intelligence platform integrated across the FOMO ecosystem. It proactively protects users while preserving privacy, requiring minimal interaction, and feeling like a natural extension of the nightlife experience.

## Core Mission

Protect users before, during, and after every journey while maintaining privacy, simplicity, and trust.

Night Guard is designed to be:
- Intelligent instead of reactive
- Temporary instead of permanent
- Context-aware instead of manual
- Privacy-first instead of surveillance
- Social instead of intimidating
- Fast enough for emergencies
- Reliable in poor connectivity

## Platform Architecture

```
                    Night Guard
                         │
 ┌───────────────────────┼────────────────────────┐
 │                       │                        │
Personal Safety      Social Safety          Emergency Safety
 │                       │                        │
Safety Check        Buddy Pair              SOS
Walk Me Home        Group Buddy Pair        Emergency Calls
Ride Companion      Friend Watch            Live Emergency Session
Recovery Center      Trusted Circle          Incident Report
```

## Intelligence Engine

```
Night Guard Intelligence
├── Context Engine
├── Risk Intelligence Engine
├── Session Engine
├── Route Intelligence Engine
├── Presence Engine
├── Trusted Circle Engine
├── Escalation Engine
├── Notification Engine
├── Privacy Engine
├── Offline Engine
├── Recovery Engine
└── Analytics Engine
```

### 1. Context Engine
Determines what the user is currently doing without requiring manual input.

Detects: Walking, Running, Driving, Ride-share, Public transport, At venue, Leaving venue, Home, Hotel, Parking, Festival, Campus, Shopping centre, Airport.

Automatically recommends Walk Me Home, Buddy Pair, or Safety Check instead of forcing users to search through menus.

### 2. Risk Intelligence Engine
Continuously calculates a live Safety Confidence Score.

Signals: time of day, route deviation, battery level, GPS confidence, internet connectivity, walking alone, trusted buddy proximity, device inactivity, venue status, emergency triggers.

Safety Levels: 🟢 Safe · 🟡 Attention · 🟠 Elevated Risk · 🔴 Emergency

Only the user sees this score unless escalation occurs.

### 3. Session Engine
Every protection feature is managed as a session.

`Created → Accepted → Active → Paused → Extended → Completed → Archived → Deleted`

Supports: Buddy Pair, Walk Me Home, Safety Check, Ride Companion. Sessions automatically expire.

### 4. Route Intelligence Engine
Powers Walk Me Home.

Capabilities: OSRM routing, live rerouting, route deviation detection, arrival detection, ETA prediction, offline routing, safe route recommendations, temporary destination changes.

Visuals: neon animated route; completed path glows brighter, remaining path fades.

### 5. Presence Engine
Instead of exposing raw GPS, friends see meaningful status:
🟢 At Venue · 🚶 Walking · 🚗 Driving · 🚕 In Ride · 🏠 Home · 🛑 Idle · 📴 Offline · 🔋 Low Battery · 🚨 Emergency

### 6. Trusted Circle Engine
Users configure reusable trust groups (Family, Partner, Close Friends, Festival Crew, Work Friends), each with configurable permissions: live location, last known location, battery level, ETA, arrival, emergency alerts, voice call, video call.

### 7. Buddy Pair Engine
Temporary mutual safety sessions.

Supports: one-to-one pairing, group pairing, venue pairing, festival pairing.

Features: live map, battery sharing, presence, temporary permissions, automatic expiry. No permanent tracking.

### 8. Walk Me Home Engine
Destination-based protection.

Supports: walking, driving, ride-share, parking walks, campus routes.

Capabilities: live progress, ETA, route deviation, arrival detection, emergency escalation, temporary sharing, smart destination suggestions. Automatically ends on arrival.

### 9. Safety Check Engine
Scheduled intelligent check-ins.

Quick presets: +30 min, +1 hour, After Event, Leaving Venue, Custom.

Responses: I'm Safe, Need Help, Snooze. Missed responses trigger adaptive escalation after a grace period.

### 10. Ride Companion Engine
Automatically supports Uber, Bolt, taxis, and rides from friends.

Shows: driver (when shared by user), vehicle description (optional), ETA, route progress, arrival. Trusted contacts receive updates without requiring continuous interaction.

### 11. Emergency Engine
SOS is accessible from anywhere in the app.

Activation options: hold Night Guard button, triple-tap map, shake gesture (optional), chat emergency shortcut.

Emergency actions: notify Trusted Circle, start emergency live session, open emergency chat, launch emergency voice/video call, share live location, show battery level, record evidence locally (optional, with explicit user consent).

### 12. Recovery Engine
Every completed emergency enters Recovery Mode. Users can: confirm safety, save trip summary, report venue, report user, block contact, contact venue management, contact emergency services, submit anonymous safety feedback.

### 13. Notification Engine
Intelligent notification prioritization across levels: Information, Reminder, Warning, Critical, Emergency. Reduces notification fatigue by escalating only when needed.

### 14. Privacy Engine
Core principles: temporary sharing only, end-to-end encrypted safety messages, automatic permission revocation, automatic data deletion after retention period, clear transparency dashboard, one-tap revoke access.

Users always know: who can see them, what is shared, how long sharing lasts, when data will be deleted.

### 15. Offline Engine
Works even with poor connectivity.

Supports: offline country maps, cached routes, local encrypted session storage, automatic sync when online, last known location, background location updates.

### 16. Analytics Engine
Measures platform health using anonymized metrics: session success rate, average response time, emergency resolution rate, battery impact, crash rate, GPS accuracy, realtime latency, notification delivery. No live user monitoring by administrators.

## Night Guard Dashboard

Accessible from: Map, Chats, Profile, Tonight.

Displays:
- 🛡 Night Guard — Status: 🟢 Protected
- Quick Actions: 🤝 Buddy Pair · 🚶 Walk Me Home · 🛡 Safety Check · 🚗 Ride Companion
- Trusted Circle — 6 Members
- Upcoming Checks — 21:30 Tonight
- Recent Sessions — Walk Home ✅ · Buddy Pair ✅
- Emergency — 🚨 SOS

## Cross-Platform Integration

- **Map** — launch and monitor safety sessions; visualize routes and buddies
- **Chats** — invite buddies; share sessions; emergency messaging
- **Groups** — Group Buddy Pair; Group Safety Check; event coordination
- **Calls** — emergency voice/video; one-tap contact access
- **Events** — recommend protection before and after events
- **Venue Pages** — offer Walk Me Home or Ride Companion when leaving
- **Camera** — optional evidence recording during emergencies with explicit user consent
- **Feed** — no public exposure of Night Guard activity

## Backend Architecture

### Client Layer (Kotlin Multiplatform — shared logic, native Android & iOS)
- Night Guard Dashboard
- Map Controls
- Chats
- Calls
- Groups
- Notifications

### API Layer
- Session Service
- Context Service
- Risk Service
- Presence Service
- Route Service
- Emergency Service
- Trusted Circle Service
- Notification Service
- Privacy Service
- Recovery Service
- Analytics Service

### Infrastructure
- Supabase Auth
- Supabase Postgres + PostGIS
- Supabase Realtime
- OSRM Routing
- Native background location (Android `FusedLocationProviderClient` / iOS `CoreLocation`, via KMP `expect`/`actual`)
- Native push notifications (Android `FCM` / iOS `APNs`, via KMP `expect`/`actual`)
- Local encrypted storage (SQLDelight + platform keystore, shared across Android/iOS)
- Offline country map packs
- Telegram Bot API — single private channel, bot as admin — for optional emergency evidence storage


