# FOMO — Tonight Screen (V2) — UI/UX Design Specification

**Status:** Design-ready spec
**Platforms:** Android (Jetpack Compose) + iOS (SwiftUI), shared layout logic via KMP
**Purpose:** Precise enough to hand directly to a designer or design-to-code agent — every card, spacing value, color, icon, and motion behavior is defined.

---

## 0. Product Framing

The Tonight screen must answer four questions within 3 seconds of opening:

1. Am I safe?
2. What's happening tonight?
3. What are my plans?
4. What's my next move?

Reference feel: **Apple Wallet** (card physicality, transitions) + **Uber Live Activities** (realtime status) + **Netflix** (dark, cinematic, large type) + **Tesla** (minimal, high-contrast, confident).

It is not a settings screen. It is not a dashboard. It is the command center for the night.

---

## 1. Design Tokens

### 1.1 Color

| Token | Hex | Usage |
|---|---|---|
| `bg.base` | `#000000` | Screen background |
| `surface.glass` | `#FFFFFF` @ 6% opacity | Card fills |
| `surface.glass.elevated` | `#FFFFFF` @ 9% opacity | Elevated/active card fills (e.g. active plan) |
| `border.glass` | `#FFFFFF` @ 10% opacity | Card borders, 1px hairline |
| `border.glass.hover` | `#FFFFFF` @ 16% opacity | Pressed/focused border |
| `accent.primary` (Electric Purple) | `#8B5CF6` | Primary CTAs, My Plans card, active-state glows |
| `accent.primary.glow` | `#8B5CF6` @ 35% opacity, 24px blur | Ambient glow behind purple elements |
| `accent.nightguard` (Blue) | `#3B82F6` | NightGuard card, shield iconography |
| `accent.nightguard.glow` | `#3B82F6` @ 30% opacity, 24px blur | NightGuard ambient glow |
| `status.success` (Emerald) | `#10B981` | Protected, Enabled, Arrived states |
| `status.warning` (Amber) | `#F59E0B` | Queue building, price rising, attention states |
| `status.danger` (Red) | `#EF4444` | Emergency SOS, alerts, decline actions |
| `text.primary` | `#FFFFFF` @ 95% opacity | Titles, primary content |
| `text.secondary` | `#FFFFFF` @ 60% opacity | Subtitles, metadata |
| `text.tertiary` | `#FFFFFF` @ 38% opacity | Placeholder, disabled, timestamps |

**Rule:** No rainbow gradients. Hierarchy and depth come from **lighting** (glow, blur, opacity layering), not additional hues.

### 1.2 Typography

System fonts (SF Pro on iOS / Roboto Flex or Inter on Android) — treat sizes as tokens shared across platforms.

| Token | Size | Weight | Line height | Usage |
|---|---|---|---|---|
| `type.display` | 34sp/pt | Bold (700) | 40 | "Tonight" header title |
| `type.title` | 22sp/pt | Semibold (600) | 28 | Card titles (NightGuard, My Plans) |
| `type.headline` | 18sp/pt | Semibold (600) | 24 | Plan names, hero summary line |
| `type.body` | 15sp/pt | Regular (400) | 20 | Card body copy |
| `type.caption` | 13sp/pt | Medium (500) | 18 | Status labels, timestamps |
| `type.micro` | 11sp/pt | Medium (500) | 14 | Feature-grid tiny status, badges |

### 1.3 Spacing & Grid

Base unit: **4dp**. Screen horizontal margin: **20dp**. Inter-card vertical gap: **16dp**. Card internal padding: **20dp**.

### 1.4 Radius

| Token | Value | Usage |
|---|---|---|
| `radius.card` | 30dp | Outer card containers |
| `radius.card.compact` | 20dp | Quick action pills, plan sub-cards, invitation cards |
| `radius.pill` | 999dp | Status capsules, buttons, badges |
| `radius.icon.button` | 14dp | Top bar icon touch targets |

### 1.5 Elevation / Blur

| Token | Value |
|---|---|
| `blur.glass` | 24px backdrop blur on all glass cards |
| `shadow.card` | y: 8dp, blur: 24dp, color: `#000000` @ 40% |
| `shadow.card.pressed` | y: 2dp, blur: 8dp, color: `#000000` @ 30% (on touch-down) |

### 1.6 Motion

| Token | Duration | Curve |
|---|---|---|
| `motion.card.enter` | 420ms | spring(damping: 0.8, stiffness: 90) |
| `motion.card.press` | 120ms | ease-out |
| `motion.pulse` (LIVE badge, glow) | 1800ms loop | ease-in-out, opacity 0.5↔1.0 |
| `motion.shimmer` (live-updating text) | 900ms | linear, one-shot on data change |
| `motion.sheet.present` | 380ms | spring(damping: 0.86, stiffness: 110) |
| `motion.timeline.fill` | tied to real elapsed time | linear |

---

## 2. Screen Structure

Single vertical scroll, no tabs, no pull-to-refresh gimmicks (realtime data updates in place).

```
Top Bar (pinned, scroll-aware)
 ↓
Tonight Overview (hero card)
 ↓
NightGuard
 ↓
My Plans
 ↓
Create Plan
 ↓
Plan Invitations   (conditional)
 ↓
AI Suggestions
 ↓
Quick Actions
```

Top bar background transitions from fully transparent (at scroll-top, sitting over the hero card) to `bg.base` @ 85% opacity with blur once the user scrolls past the hero card — same behavior as Apple Wallet's nav bar.

---

## 3. Top Bar

**Height:** 56dp + safe-area inset. **Horizontal padding:** 20dp.

### 3.1 Layout

```
[ Tonight                                    🔔₃  ✉️₂  👤+₁ ]
[  Everything is ready.                                     ]
```

Left-aligned: title + dynamic subtitle stack (see §4).
Right-aligned: icon cluster, evenly spaced 12dp apart, each in a 40dp × 40dp touch target (`radius.icon.button`), icon glyph itself 22dp.

### 3.2 Icon Cluster (new in V2)

Three tappable icons, right to left in priority order:

| Icon | Glyph | Badge | Tap destination | Notes |
|---|---|---|---|---|
| **Notifications** | Bell (outline) | Red dot or count, top-right of glyph | Notifications sheet (system + activity feed) | Existing icon, kept from V1 |
| **Invites** | Envelope / paper-plane outline | Purple count badge (`accent.primary`) | Invitations sheet — same content as the conditional "Plan Invitations" card (§8), but always reachable regardless of scroll position | Shows only when count > 0; icon still renders at 40% opacity with no badge when 0, never fully disappears (keeps layout stable) |
| **Requests** | Person with plus (person-plus outline) | Emerald count badge | Requests sheet: Buddy Pair requests (NightGuard), follow requests, plan-join requests | Distinct from Invites — Invites are "you were invited to a plan," Requests are "someone wants to connect/pair with you" |

**Badge spec:** 16dp diameter, `type.micro` numeral (white text on colored fill), positioned at top-right corner of the icon glyph, offset by -2dp/-2dp so it slightly overlaps the icon boundary. Max display "9+" beyond 9. Badge pulses once (`motion.card.enter`-style scale bounce, 1.0→1.15→1.0 over 300ms) when a new item arrives while the screen is open.

**Behavior when tapped:**
- **Invites icon →** opens a bottom sheet listing all pending plan invitations (same card component as §8), scrollable if more than one.
- **Requests icon →** opens a bottom sheet with two segments: "Buddy Pair Requests" (NightGuard) and "Connection Requests" (follow/friend), each row showing avatar, name, mutuals/context, and Accept/Decline buttons.
- **Notifications icon →** opens the general notification feed (arrivals, plan updates, system messages) — not a new component, existing pattern.

**Empty state for icon cluster:** if there are zero invites, zero requests, and zero notifications, all three icons render at reduced opacity (40%) with no badges — never hidden, so the top bar layout never shifts.

---

## 4. Header Title Block

Sits below the top bar icon row, left-aligned, part of the same pinned region.

- **Title:** "Tonight" — `type.display`, `text.primary`.
- **Subtitle:** `type.body`, `text.secondary`. Dynamic, single line, cross-fades (200ms) when the underlying state changes. Examples:
  - "Everything is ready."
  - "2 plans tonight."
  - "NightGuard Active."
  - "Let's make tonight unforgettable."

Priority order when multiple states are true: Safety status > active plan count > generic greeting.

---

## 5. Tonight Overview (Hero Card)

**Height:** 220dp fixed. **Radius:** `radius.card`. **Fill:** `surface.glass.elevated` with an ambient background glow that shifts hue based on state (purple when plans active, blue if NightGuard is the dominant concern, i.e. late night).

### Layout (top to bottom, 20dp padding)

```
Tonight                                          [ LIVE ]
✓ Protected      🎉 2 Active Plans      👥 8 Friends
📍 Sandton
Next Stop: Saint · 8:30 PM
─────────────────────────────────────────────
Continue →
```

- Row 1: card label "Tonight" (`type.caption`, `text.secondary`) + `LIVE` capsule top-right (see §5.1).
- Row 2: three inline stat chips, each icon + label (`type.body`, `text.primary`), horizontally distributed with 16dp gaps, wrapping to two rows on narrow screens.
- Row 3: current location line, icon + `type.caption`.
- Row 4: next-stop line, bolded venue name + time, `type.headline`.
- Divider: 1px `border.glass`, 16dp top/bottom margin.
- Footer: `Continue →` — tappable full-width row, `type.body` + chevron icon, right-aligned chevron, opens the full-day timeline view.

### 5.1 LIVE Capsule

`radius.pill`, height 24dp, horizontal padding 10dp. Fill: `accent.primary` @ 18% with 1px border `accent.primary` @ 50%. Text: "LIVE", `type.micro`, `accent.primary` at full opacity. A 6dp dot to the left of the text pulses per `motion.pulse`.

---

## 6. NightGuard Card

**Radius:** `radius.card`. **Fill:** `surface.glass` with `accent.nightguard.glow` positioned behind the shield illustration (top-right corner bleed, 40% card width). **Border:** `border.glass`, but tinted blue at 14% when NightGuard is actively monitoring.

### Layout

```
🛡  NightGuard                                  ⌵ (glow icon)
    You're protected tonight.
    Monitoring until 4 AM

┌───────────┬───────────┬───────────┬───────────┐
│ Trusted   │ Emergency │ Live      │ Safe      │
│ Contacts  │ SOS       │ Location  │ Arrival   │
│ ✓ Enabled │  —        │ 3 Contacts│ Active    │
├───────────┼───────────┼───────────┼───────────┤
│ Route     │ Group     │ Medical   │ Emergency │
│ Awareness │ Separation│ Info      │ Notes     │
│ Off       │ Updated   │  —        │  —        │
└───────────┴───────────┴───────────┴───────────┘

Manage NightGuard →
```

- Header row: shield glyph (24dp, `accent.nightguard`) + title `type.title`, floating/slightly bobbing (subtle 2dp vertical loop, 3s period).
- Status line: `type.body`, `text.primary`.
- Sub-status: `type.caption`, `text.secondary`.
- **Feature Grid:** 4 columns × 2 rows, each cell is a mini tile:
  - Icon (18dp) top-left of tile.
  - Label `type.caption`, 2-line max.
  - Status `type.micro`, `text.secondary`, or `status.success` color when the status word implies positive state (e.g. "✓ Enabled", "Active", "Updated").
  - Tile fill: `surface.glass` @ additional 3% (subtly distinguishable from card background), radius 14dp, 10dp internal padding, 8dp gap between tiles.
  - Tapping a tile deep-links directly into that NightGuard sub-setting (e.g. tapping "Medical Info" opens the medical info editor).
- Footer: `Manage NightGuard →` full-width tap row, opens the NightGuard module home.

---

## 7. My Plans Card

**Radius:** `radius.card`. **Fill:** `surface.glass`, with `accent.primary.glow` ambient behind the card (soft, 24px blur, centered top).

### 7.1 Card Header

```
My Plans                                    [ + New Plan ]
```

- Title `type.title`.
- `+ New Plan` — pill button, `accent.primary` fill @ 90%, white text `type.body` medium weight, icon "+" 16dp leading. Height 36dp, `radius.pill`. Always visible, no overflow menu.

### 7.2 Individual Plan Card (repeats, stacked, 12dp gap between)

Each plan renders as its own nested card: `surface.glass.elevated`, radius `radius.card.compact` (20dp), 16dp padding.

```
Birthday Night                              [ ACTIVE ]
7:30 PM · 4 Stops · 6 Friends

Marble ─●─ Saint ─●─ LIV ─○─ Home
        (filled = visited, hollow = upcoming)

👤👤👤 +3                              5/8 arrived

[ Open ]                    [ Edit ]
```

- **Title row:** plan name `type.headline`, status badge right-aligned (`ACTIVE` in emerald pill, `UPCOMING` in white/10% pill, `PAST` in tertiary-text pill).
- **Meta row:** `type.caption`, `text.secondary`, separated by "·".
- **Vertical/horizontal timeline:** venue nodes connected by a line that fills (solid `accent.primary`) up to the current stop, remaining segment rendered as a dotted/hollow line. Node dot: 8dp, filled = passed/current, hollow = upcoming. Animates fill per `motion.timeline.fill` as the night progresses.
- **Avatar row:** overlapping circular avatars (28dp, 6dp overlap), "+N" overflow chip if more than 4 friends. Avatars animate a subtle position shift when a friend moves venues (per Motion Design, §11).
- **Arrival counter:** right-aligned, `type.caption`, updates live with `motion.shimmer` flash on change.
- **Buttons:** `Open` (primary, fills remaining width minus Edit) and `Edit` (secondary, icon-only pencil on narrow layouts). Both 40dp height, `radius.pill`.
- **Swipe actions** (swipe left on the plan card): reveals `Duplicate` (purple), `Leave Plan` (amber), `Delete` (red) — 72dp wide action buttons, icon + micro-label.

Multiple plans stack directly below one another with a 12dp gap — no carousel, no pagination.

---

## 8. Create Plan Card

Always visible, standalone card below My Plans — never hidden behind a menu.

**Radius:** `radius.card`. **Fill:** `surface.glass`.

```
Create Tonight's Plan
Solo Date Night · Friends Celebration · Bar Crawl · Festival · Road Trip

○ Solo    ○ Duo    ○ Group

[        Create Plan        ]
```

- Title `type.title`.
- Template chip row: horizontally scrollable pills, `type.caption`, tap to prefill the creation flow's title step.
- Type selector: three large segmented buttons (equal width, 48dp height, radius 14dp), single-select, selected state gets `accent.primary` @ 20% fill + 1px `accent.primary` border.
- Primary CTA: full-width button, `accent.primary` solid fill, white bold text, height 52dp, `radius.pill`. Tapping opens the Plan Creation bottom sheet.

### 8.1 Plan Creation Flow — Bottom Sheet

Presented via `motion.sheet.present`, draggable, snap points at 60%/95% of screen height.

| Step | Screen content | Key interactions |
|---|---|---|
| 1. Type | Solo / Duo / Group large tappable cards with illustration | Single select, auto-advances |
| 2. Title | Suggested titles (Friday Vibes, Birthday, After Work, Girls Night, Boys Night) + custom text field | Chip select or free text |
| 3. Venues | Search bar, tabs: Recommended / Saved / Trending, map picker toggle | Multi-select, reorderable list forms the stop order |
| 4. Time | Timeline editor — each selected venue shown as a draggable block on a vertical time axis | Drag & drop to set arrival times; snaps to 15-min increments |
| 5. Invite | Followers / Contacts tabs, plus external share row (WhatsApp, Instagram, Copy Link icons) | Multi-select invitees; share sheet for external |
| 6. Finish | Full-screen success animation — card "becomes Live," confetti-free (keep premium, use light burst/glow instead of confetti) | Auto-dismiss into the My Plans stack, new card enters with `motion.card.enter` |

Sheet header always shows step progress as a thin 2px purple progress bar under the top handle, not numbered dots (keeps it minimal).

---

## 9. Plan Invitations Card

**Conditional:** renders only when at least one pending invitation exists. (Reachable at all times via the top bar Invites icon regardless of this card's visibility.)

**Radius:** `radius.card.compact`. **Fill:** `surface.glass`.

```
Tonight Invitations

Amanda invited you
Girls Night · Starts 8 PM · 4 friends attending

[ Accept ]   [ Decline ]   [ View ]
```

- Section title `type.title`.
- Each invitation is its own row/sub-card: inviter avatar (32dp) + name bolded inline with "invited you", plan name `type.headline`, meta line `type.caption`.
- Three buttons: `Accept` (primary, emerald fill), `Decline` (secondary, outline, red text), `View` (tertiary, text-only, opens plan detail without responding).
- Multiple invitations stack with 8dp gap; card auto-collapses to a "1 of 3 invitations" summary row if more than 2 are pending, expandable on tap.

---

## 10. AI Suggestions Card

**Radius:** `radius.card.compact`. **Fill:** `surface.glass`, no ambient glow (kept quiet/utilitarian on purpose — this card should never compete visually with NightGuard or My Plans).

```
💡  Leave now. Queue increasing at Saint.
```

- Single icon (lightbulb/sparkle, 18dp, `text.secondary`) + one line of `type.body` text.
- **Hard rule: only one suggestion visible at any time.** Additional suggestions are reachable by horizontal swipe (card content cross-fades + slides, 250ms), with a subtle dot indicator (3 small dots, 4dp, bottom-center) only if more than one suggestion is queued.
- No CTA button by default — suggestion text itself is often actionable context (e.g. tapping the card opens the relevant venue or ride app deep link where applicable).

---

## 11. Quick Actions Row

Below AI Suggestions, final element before end of scroll.

```
 ( 🆘 )   ( 💸 )   ( 📍 )   ( 🔗 )   ( 🚕 )
Emergency  Split    Find    Share    Ride
           Fare     Friends  Plan    Home
```

- Five circular icon buttons, 56dp diameter, `surface.glass` fill, 1px `border.glass`.
- Icon 24dp, centered. Label below in `type.micro`, `text.secondary`, 4dp gap from circle.
- **Emergency** button only: red-tinted fill (`status.danger` @ 15%) and red icon — always visually distinct from the other four regardless of adaptive state.
- Row is horizontally scrollable if it doesn't fit 5 across on smaller screens; Emergency is always the leftmost/pinned item so it's never scrolled out of view.
- In the **Late Night** adaptive state, **Ride Home** expands to a wider pill (spanning 2 slot-widths) with a live ETA/price subtitle, per §13.

---

## 12. Empty State

Replaces the My Plans card content (not the whole screen) when the user has zero plans.

```
      [ illustration: quiet skyline / empty map pin ]

      Nothing planned tonight.

      [    Create First Plan    ]

      Suggested venues near you →
```

- Illustration: centered, ~120dp height, monochrome/line-art style consistent with the dark theme (no bright colors).
- Headline `type.headline`, `text.secondary`.
- Primary CTA identical styling to the Create Plan button in §8.
- Below: a horizontally scrollable row of 3–4 suggested-venue chips (venue photo thumbnail + name), sourced from `SmartSuggestionsEngine`.

---

## 13. Live State & Adaptive States

### 13.1 Live State (applies once a plan is underway)

Tonight Overview hero card and the active Plan card both switch to "live" copy:

```
Currently at Saint
Next: LIV · 12 min away
```

- Timeline node line animates continuously toward the next stop.
- Friend avatars animate a small position/opacity change when their presence updates (per `motion.shimmer` on the arrival counter).
- Ride ETAs and queue predictions refresh in place with a soft cross-fade, no jarring re-layout.

### 13.2 Adaptive States (driven by `ContextEngine`)

| State | Trigger | Behavior |
|---|---|---|
| **Before Going Out** | No active plan yet today, evening approaching | Create Plan card promoted directly under Tonight Overview (above NightGuard); AI Suggestions biased toward venue recommendations |
| **During the Night** | Active plan in progress | Standard order; My Plans and NightGuard get equal visual weight; Live State copy active |
| **Late Night** | After a configurable local-time threshold (e.g. 1:00 AM) | NightGuard card reorders to appear directly below Tonight Overview (above My Plans); Ride Home quick action expands (§11); Safe Arrival prompts may appear as a transient banner above Quick Actions |

Reordering between states uses a shared-element reflow animation (each card's position interpolates over 400ms, spring easing) rather than an abrupt jump.

---

## 14. Motion Design Summary

- Cards float: 2–3dp ambient vertical drift, slow (6–8s) loop, imperceptible but adds "alive" quality.
- Ambient glow color/intensity shifts gradually (2s cross-fade) when dominant state changes (e.g. purple → blue when NightGuard becomes priority).
- `LIVE` badge pulses continuously per `motion.pulse`.
- Timeline lines fill in real time as the night progresses (`motion.timeline.fill`).
- Friend avatars animate between venue nodes when presence updates.
- All buttons use a spring press-down (`motion.card.press`) — scale to 0.97 on touch-down, back to 1.0 on release.
- Sheet transitions (`motion.sheet.present`) always spring-based, never linear/ease.

---

## 15. Accessibility Notes

- All status-conveying color (emerald/amber/red) must be paired with text or icon shape — never color alone.
- Minimum touch target 40dp × 40dp for all icons, including the new top-bar Invites/Requests icons.
- Text contrast: `text.secondary` (60% white on black) meets AA for body text at 15sp+; do not go lower than 60% opacity for any readable line.
- Badge counts must have an accessible label (e.g. "3 pending invitations") for screen readers, not just the numeral.
- Motion: respect system "reduce motion" setting — disable ambient float/pulse loops, keep only functional transitions (sheet present/dismiss, screen navigation).

---

## 16. Open Items for Engineering Handoff

- Confirm exact Late Night time threshold (default proposed: 1:00 AM local, configurable per user later).
- Confirm whether Plans Chat / Group Voting (present in backend spec) surface inside the plan's "Open" detail view — not addressed by this screen-level spec, needs its own detail-view spec.
- Confirm badge count source-of-truth polling/subscription strategy (Supabase Realtime channel per user for invites + requests + notifications, or a single aggregated channel).
