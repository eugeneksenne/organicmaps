# FOMO Channels — "See All" Screen & Inside-the-Channel Spec (v1)

*Reviewing and extending `FOMO_Channels_System__Full_Updated_Architecture`*

---

## 1. What's missing from the current doc

The existing architecture nails the **single-channel experience** (hero → AI brief → quick actions → trending → live → tabs). But it treats channel *discovery/navigation* as a throwaway dropdown:

> "Tapping opens: Province Channels / City Channels / Club Channels / Pinned Channels / Recent Channels / Search. No clutter."

That's a feature list, not a screen spec. Gaps that will bite an autonomous coding agent:

1. **No layout, states, or interaction model** for the "See All Channels" screen — it's currently just six bullet labels.
2. **No membership model.** Can a user view a channel without joining/following it? Is there a follow/pin distinction, or one action?
3. **No cold-start / empty states.** What does a province with 2 active cities look like? What does a club channel show at 3pm with zero live activity?
4. **No real-time mechanics.** "Updated now," "24,200 people exploring," live viewer counts — none of these specify polling vs. Supabase Realtime channel subscriptions, refresh cadence, or staleness fallback.
5. **No platform split.** This is KMP — shared view-models are fine, but Compose (Android) and SwiftUI (iOS) render differently; hero video autoplay, horizontal scroll physics, and pull-to-refresh need per-platform notes.
6. **No deep-link / notification entry.** A push notification ("DBN Gogo starts in 40 min") needs to land inside a specific channel + tab + scroll position.
7. **No moderation/report entry point** inside the channel screen, despite Community Moderation being a named system elsewhere.
8. **No accessibility notes** — this is a highly visual, gesture-heavy screen (horizontal swipes, image-first cards) with no VoiceOver/TalkBack equivalents specified.

Everything below fills these gaps. Sections 2–3 are new specs; Section 4 lists open decisions for you.

---

## 2. "See All Channels" Screen — Full Spec

### 2.1 Purpose
One screen answers: *which channels exist, which am I in, and how do I get into a new one fast.* It is reached from the Channel Switcher chevron on the Top Navigation, and (optionally) from a "Browse Channels" entry in the Discover tab.

### 2.2 Structure (top to bottom)

```
Top Bar
  ← Back        "Channels"        🔍 Search

Your Channels (horizontal, pinned first)
  [Johannesburg Nights ✓ pinned]  [Cape Town Nights]  [+ Add]

Recommended For You
  Based on location + past activity
  "Pretoria Nights — 12,400 exploring tonight"

Browse by Region  (progressive disclosure, not a full tree upfront)
  Province → tap → City list → tap → Club list
  Each row: name, live indicator dot if any club inside is live,
            member/exploring count, chevron

Recently Viewed
  Last 5 channels visited, most recent first

Search (activated state)
  Replaces the whole screen content, not a modal —
  fuzzy match across province/city/club names,
  results grouped by type with the same row style as Browse
```

### 2.3 Row anatomy (used consistently in Recommended / Browse / Recently Viewed)
- Channel name + verified badge if official
- Small live-red dot if anything inside is currently live
- One-line context string (exploring count, or "Quiet tonight," or "3 events this week")
- Pin icon (tap to pin without entering) / Join or Following state chip

### 2.4 States
| State | Behavior |
|---|---|
| Cold city (new market, <500 users) | Suppress "X exploring" count; show "Just getting started here" + a CTA to invite friends instead of a number that reads as dead |
| No pinned channels yet | "Your Channels" section collapses to a single "Pin your city to get here faster" prompt |
| Search, no results | "No channels match '{query}' — request we add your city" (feeds ops backlog) |
| Offline | Show last cached list with a "You're offline — showing saved channels" banner, no live dots |
| Loading | Skeleton rows (not spinners) matching row height, staggered fade-in per section |

### 2.5 Membership model (needed before this can be built — see §4.1)
Proposed default: **auto-membership by geolocation** for Province/City channels (no explicit join needed to view), but **explicit Follow** required for Club channels, since that's what drives notification opt-in and the "Your Channels" pinned rail. Pin is a subset of Follow (must follow to pin).

### 2.6 Platform notes
- **Android (Compose):** `LazyColumn` with sticky section headers; region drill-down uses Compose Navigation with shared-element transition on the row → detail hero image.
- **iOS (SwiftUI):** `List` with `Section`; drill-down via `NavigationStack`; matched geometry effect for the same shared-element transition, so the row image morphs into the Channel Hero photo on entry.
- Shared KMP view-model exposes `ChannelListState` (loading/loaded/empty/offline) — platforms only own rendering, not data shape.

---

## 3. Inside the Channel — Gaps Closed

Keeping the existing hierarchy (Hero → AI Brief → Quick Actions → Trending → Live Now → Tabs), here's what needs to be added per section:

### 3.1 Real-time data contract
- Channel Hero exploring-count, Trending Tonight energy %, and Live Now viewer counts subscribe to a **Supabase Realtime channel** scoped to `channel_id`, not polling.
- Fallback: if Realtime connection drops, fall back to a 30s poll and show a small "Updated {n}s ago" instead of "Updated now" so the UI never lies about freshness.
- AI Night Brief regenerates server-side on a schedule (proposed: every 15 min or on significant event, e.g., a venue crossing a capacity threshold) — client just fetches latest, doesn't compute.

### 3.2 Cold-start / low-activity state (this is the biggest gap)
The mockups assume a buzzing Friday night in Johannesburg. Most channel-loads will not look like that. Define a **Quiet Mode** layout:
- Hero copy shifts from "Peak nightlife has just begun" to something honest, e.g. "It's early — here's what's coming up tonight."
- AI Brief shows upcoming events instead of live momentum lines when there's nothing live yet.
- Trending Tonight and Live Now sections **hide entirely** (don't render empty carousels) rather than show "No live venues" placeholders — reduces perceived deadness.
- Quick Actions stays, but "Live Now" quick action shows a muted/disabled state if literally nothing is live.

### 3.3 First-time visitor vs. returning member
- First visit to any channel: brief 1-time coach mark on the Channel Switcher chevron only (not a full tutorial).
- Friend Layer ("Friends Tonight") only renders if the user has ≥1 friend with location/activity sharing on for that channel; otherwise section is omitted, not shown empty.

### 3.4 Deep links / notifications
Every push notification referencing a channel must resolve to: `channel_id` + `tab` (default Overview) + optional `scroll_target` (e.g., a specific Live Now card). Cold-start app opens land on Overview with a one-time loading skeleton; warm app opens animate the tab/scroll transition.

### 3.5 Moderation entry point
Every card in Feed/Trending/Live carries the existing 🚮/🛡 controls; add a long-press → "Report" sheet that reuses the Community Moderation weighting (trust/activity/verification/reputation) already defined elsewhere — this wasn't wired into the channel screen before.

### 3.6 Accessibility
- All horizontal-swipe carousels (Trending Tonight, Live Now) need a non-gesture fallback: VoiceOver/TalkBack users get a linear list order via `accessibilityElements`/`contentDescription` ordering rather than relying on swipe.
- Live pulse animations must respect `prefers-reduced-motion` equivalents on both platforms.
- Minimum contrast for white text over hero photography — enforce a gradient scrim, not just opacity, since photo content is unpredictable.

### 3.7 Business-type variance
Club Channels change the hero (already specified). Extend the same override pattern to **Event Channels** (festival/pop-up) — hero shows countdown-to-doors instead of live crowd stats — since FOMO's Food/Prep/Event business types already diverge elsewhere in the product.

---

## 4. Open Decisions (carrying these forward, per usual)

1. **Membership model:** auto-follow by geolocation for Province/City vs. explicit follow for Club — confirm or override (§2.5).
2. **Pin limit:** how many channels can a user pin to the "Your Channels" rail before it needs its own overflow/reorder UI?
3. **Cross-listing with Discover:** does "Browse Channels" live only inside Channels, or does Discover also surface a "Browse Channels" entry point? Risk of two navigation paths to the same list.
4. **Realtime cost:** every open channel screen holding a live Supabase Realtime subscription — confirm this is acceptable at scale vs. a shorter-lived subscription that closes after N minutes of inactivity.
5. **Quiet Mode threshold:** what exploring-count or event-count triggers Quiet Mode vs. full Trending/Live rendering?

---

*Next step, once you confirm §4: I can turn this into a loop-based agent prompt with PROGRESS.md/DECISIONS.md/ASSUMPTIONS.md scaffolding the same way as the other engine specs.*
