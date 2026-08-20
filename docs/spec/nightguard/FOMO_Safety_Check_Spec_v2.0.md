# FOMO "Safety Check" Full Spec

Safety Check is the quiet intelligence layer of Night Guard. Unlike Buddy Pair (social safety) and Walk Me Home (journey safety), it protects users when they don't need live tracking 〞 but still want someone to know they're okay.

| Feature | Promise |
|---|---|
| ? Buddy Pair | We're together. |
| ? Walk Me Home | Watch me until I arrive. |
| ? Safety Check | Check that I'm okay. |

## Mission

Automatically verify that users are safe at the right time, with the least amount of interruption. No live location sharing is required unless the user chooses it or an escalation occurs.

## Platform

```
Safety Check
* Check Scheduler Engine
* Smart Trigger Engine
* Check Session Engine
* Reminder Engine
* Escalation Engine
* Trusted Circle Engine
* Emergency Engine
* Privacy Engine
* Recovery Engine
* Analytics Engine
* Offline Engine
```

## Lifecycle

`Create Check ↙ Schedule ↙ Waiting ↙ Reminder ↙ User Response ↙ Completed`
`                                              沛 Expired ↙ Escalation (if needed) ↙ Recovery ↙ Auto Delete`

A check can also complete silently before the scheduled time 〞 see **Silent Wellness Check** below 〞 without ever entering the reminder path.

## Entry Points

Users can create a Safety Check from: Night Guard Dashboard, Map, Chats, Group Chats, Buddy Pair, Walk Me Home, Event Page, Venue Page, Profile, My Moves. It can also be suggested automatically.

### Smart Suggestions

Triggered by context, not menus. Examples: leaving a venue alone, meeting someone new, going on a first date, going for a run, travelling, working a night shift, studying late, battery getting low, late-night commute.

Instead of asking *"Start Safety Check?"*, FOMO states the outcome: **"We'll check in with you in one hour."** The suggestion is dismissible with one tap and never blocks the current screen.

---

## Screens

### Screen 1 〞 Safety Check Home
Entry hub. Shows current protection status at a glance, not a settings menu.

- Header: ? Safety Check 〞 *"Stay protected without continuous tracking."*
- Primary CTA: **Start Safety Check**
- Sections: Scheduled Checks (upcoming), Recent Checks (last 5, resolved), Trusted Circle (member count, shortcut to manage), Settings (gear icon, top-right)
- If a check is currently active, the primary CTA is replaced by a live status card (see Screen 5) 〞 a user is never shown "Start" while one is already running.

### Screen 2 〞 Create Check
*"Choose when FOMO should check on you."*

- Quick duration chips: 15 min ﹞ 30 min ﹞ 1 hour ﹞ 2 hours ﹞ After Event Ends ﹞ After Leaving Venue ﹞ On Arrival ﹞ Custom
- Below the chips: **Templates** row (Night Out, First Date, Long Drive, Travel, Campus Walk, Gym, Work Shift, Custom) 〞 selecting one pre-fills duration, contacts, and permissions from Screen 3/4, skipping straight to Review.
- Selecting a context-based option (After Event Ends / After Leaving Venue / On Arrival) shows a one-line confirmation of the trigger it's bound to, e.g. *"Triggers when you leave [Venue Name]."*

### Screen 3 〞 Choose Contacts
*"Share with:"*

- Contact groups: Trusted Circle, Recent Buddies, Family, Partner, Friends (multi-select chips, pulled from Trusted Circle Engine)
- Permissions (all default **on** except live location, consistent with Privacy Engine defaults):
  - ? Notify if missed
  - ? Share location only if emergency
  - ? Share ETA
  - ? Call me if I don't respond *(off by default 〞 opt-in, since it implies a phone call interruption for the contact)*
- Empty state (no Trusted Circle yet): inline prompt to add a contact, not a dead end.

### Screen 4 〞 Review
Single-glance confirmation before commitment 〞 no additional taps required after this screen.

```
Safety Check
In 1 hour

Notify
Sarah ﹞ James

Emergency
Location shared only if needed

[ Start ]
```

- Tapping any field jumps back to the relevant screen (Screen 2 or 3) with state preserved.
- "Start" is disabled (not hidden) until at least one contact is selected, with an inline hint rather than a silent failure.

### Screen 5 〞 Waiting
No background interaction required; this screen can be left at any time.

- Countdown (large, central) to next check
- "Next Check" timestamp
- Actions: **Cancel**, **Edit**, **Extend**
- If Smart Trigger context changes (e.g. user starts driving), the countdown adjusts automatically and the screen shows a one-line note: *"Adjusted 〞 you're driving."*

### Check Notification (system-level, not a full screen)
Fires at the scheduled time:

```
? Safety Check
How are you?

[ ? I'm Safe ]  [ ? Snooze ]  [ ? Need Help ]
```

Responds in one tap, from the lock screen where the OS allows it.

### Screen 6 〞 Trusted Contact View
What a contact sees if a check is missed (deep link from their own notification):

```
Safety Check Missed
Alfred has not responded.

Last Status: 21 minutes ago

[ Call ]  [ Message ]  [ Navigate ]  [ Request Live Location ]  [ Start Emergency Session ]
```

- "Navigate" only appears if Alfred has an active or last-known location to navigate to.
- "Request Live Location" sends Alfred a one-tap consent prompt 〞 it never silently reveals location.

### Screen 7 〞 Recovery
Shown only if an escalation occurred, once the user is confirmed safe or reachable again.

```
Recovery
Were you okay?

[ Yes ]  [ Need Assistance ]  [ Report Incident ]  [ Block User ]  [ Submit Feedback ]
```

- "Report Incident" and "Submit Feedback" route to the Recovery Engine, shared with Night Guard's broader Recovery Center.

### Screen 8 〞 Settings
Configure: default duration, Trusted Circle, reminder frequency, grace period, maximum snooze, escalation timing, emergency permissions, automatic suggestions, recurring schedules, templates.

### Screen 9 〞 Check Timeline (private detail view)
Accessible from Recent Checks. Read-only audit trail of one check, visible only to the user:

```
20:00  Check Created
21:00  Reminder Sent
21:02  Snoozed
21:12  Safe Confirmed ↙ Completed (deleted automatically)
```

---

## Smart Trigger Engine

Checks trigger on events, not only elapsed time: leaving venue, arriving home, phone offline, battery under threshold, journey completed, Buddy Pair ended, entering hotel, entering home, festival finished, custom geofence.

## Response Handling

**I'm Safe** ↙ trusted contacts optionally notified ↙ session completes ↙ auto-deletes. No further action.

**Snooze** ↙ presets 5 / 10 / 15 / 30 min / Custom, capped by the user's configured maximum snooze. Contacts are **not** notified on snooze alone 〞 only if snoozing continues past the missed-check flow below.

**Need Help** ↙ immediately opens Emergency Mode: Call Trusted Contact, SOS, Start Walk Me Home, Start Buddy Pair, Emergency Chat, Emergency Call, Share Location.

## Missed Check Flow 〞 resolved defaults

The original spec left grace period and escalation timing as "configurable"; shipping needs a sane default even before the user ever opens Settings:

| Step | Default timing | Notes |
|---|---|---|
| Grace period | 10 min after scheduled time, no response | Configurable 5每30 min in Settings |
| Reminder 1 | At grace period expiry | Silent push, no contact notification |
| Reminder 2 | +5 min after Reminder 1 | Silent push |
| Push Notification (urgent) | +5 min after Reminder 2 | Elevated priority, bypasses notification batching |
| Phone Call Prompt (optional) | +5 min after urgent push | Only if user enabled call-based reminders |
| Trusted Contact Notification | +5 min after phone call prompt (or immediately after urgent push if calls disabled) | Contacts see Screen 6 |
| Emergency Escalation | If no response 15 min after contact notification | Escalation Engine takes over 〞 see below |

Total worst case from missed check to contact notification: **~25 minutes**, tunable per-check via templates (e.g. a "First Date" template can tighten this to 10 minutes total).

## Escalation Engine

Adaptive, not a single fixed timer 〞 severity is read from the Risk Intelligence Engine's live Safety Confidence Score, not from elapsed time alone:

`Low Risk ↙ Reminder` ↙ `Medium Risk ↙ Trusted Contact` ↙ `High Risk ↙ Location Shared` ↙ `Critical ↙ Emergency Session` ↙ `Resolved ↙ Recovery`

A user who's stationary at a known-safe location (home, a venue they checked into) escalates more slowly than one who's off-route and battery-critical, even at the same elapsed time. This avoids unnecessary panic while still compressing the timeline when real risk signals stack up.

## Context Awareness

- If Walk Me Home is active, Safety Check pauses automatically (no duplicate protection).
- If Buddy Pair is active, Safety Check isn't duplicated.
- If the user arrives home, Safety Check completes automatically.
- If the user starts driving, the countdown adjusts.

## Silent Wellness Check

If sensors indicate normal movement and the user reaches a trusted destination before the scheduled time, the check quietly completes itself 〞 no interruption 〞 while still notifying trusted contacts who opted into arrival notifications.

## Recurring Checks

For night shift workers, security guards, healthcare workers, delivery drivers, students. Schedules: Daily, Weekdays, Weekends, Custom (e.g. *"Every Friday, check at 02:00"*).

## Smart Templates

Night Out, First Date, Long Drive, Travel, Campus Walk, Gym, Work Shift, Custom. Each stores: duration, contacts, permissions, escalation rules 〞 so returning users skip Screens 2每4 entirely.

## Privacy Engine

Shares nothing unless configured. Default: no live tracking, no continuous GPS, no route history 〞 only time, status, and emergency data if required. Users always know who can see them, what's shared, how long it lasts, and when it's deleted.

## Team Safety Checks (billion-dollar differentiator)

For security teams, medical staff, event crews, nightlife staff. Supervisors see each member's check status only (Scheduled / Safe / Missed / Escalated) 〞 never continuous location 〞 preserving privacy while improving operational safety.

## Unified Night Guard Intelligence

Safety Check shares context with Buddy Pair, Walk Me Home, Ride Companion, and the Risk Intelligence Engine so users are never asked to activate overlapping protection. Night Guard automatically selects the most appropriate protection level for the situation.

---

## Backend Data Model

```
safety_checks
-------------
id
userId
status              -- CheckStatus enum
scheduledAt
completedAt
templateId
durationMinutes
trigger             -- Timer | SmartTrigger(event) | Recurring
escalationLevel
gracePeriodMinutes  -- default 10
maxSnoozeMinutes    -- default 30

check_contacts
--------------
checkId
userId
permissions         -- notifyIfMissed, shareLocationOnlyIfEmergency, shareEta, callMeIfNoResponse

check_events
------------
checkId
eventType           -- Created | ReminderSent | Snoozed | SafeConfirmed | Completed | Escalated | Expired
timestamp
metadata

check_reminders
---------------
checkId
sentAt
type                -- FirstReminder | SecondReminder | PushNotification | PhoneCallPrompt

check_escalations
-----------------
checkId
level               -- None | LowRisk | MediumRisk | HighRisk | Critical | Resolved
trigger
timestamp

check_templates
---------------
id
name
icon
durationMinutes
defaultContacts
permissions
escalationRules     -- gracePeriodMinutes, reminderIntervalMinutes, maxSnoozeMinutes
```

## Integration Across FOMO

- **Night Guard** 〞 central dashboard for all active checks.
- **Buddy Pair** 〞 ends automatically if a Buddy Pair session already provides protection.
- **Walk Me Home** 〞 suppressed during an active journey, resumed afterward if needed.
- **Map** 〞 create location-aware Safety Checks.
- **Chats** 〞 invite trusted contacts, receive notifications.
- **Events & Venues** 〞 one-tap Safety Checks before leaving or after an event ends.
- **Trusted Circle** 〞 uses saved contacts and permission profiles.

## UX Principles

- Create a Safety Check in under 5 seconds.
- One-tap responses: I'm Safe, Snooze, Need Help.
- No unnecessary location sharing.
- Adaptive escalation instead of fixed timers.
- Automatic cleanup after completion.
- Smart coordination with Buddy Pair and Walk Me Home to avoid duplicate protection.

---

## Implementation Status

UI is being implemented natively in **Kotlin Compose Multiplatform** (shared Android/iOS), matching FOMO's actual client architecture:

| Layer | File | Status |
|---|---|---|
| Design tokens | `theme/NightGuardTheme.kt` | ? Done |
| Data models | `model/SafetyCheckModels.kt` | ? Done |
| ViewModel/state | `viewmodel/SafetyCheckViewModel.kt` | ? In progress |
| Navigation | `navigation/SafetyCheckNavigation.kt` | ? In progress |
| Screens 1每9 | `screens/*.kt` | ? In progress |

## Changes from v1.0
- Resolved "configurable" grace period and escalation timing into concrete shipping defaults (10 min grace, ~25 min worst-case to contact notification), while keeping them user-tunable in Settings.
- Added a 9th screen (Check Timeline) to formalize the private audit trail mentioned in the original doc's "Check Timeline" section.
- Clarified "Call me if I don't respond" as off-by-default (opt-in), since it's the one permission that interrupts the *contact*, not just shares data about the user.
- Tied the Review screen's "Start" button to explicit validation (disabled + inline hint) rather than an implied always-enabled state.
- Cross-referenced Screens 1每9 directly to the Compose implementation files being built alongside this spec.

