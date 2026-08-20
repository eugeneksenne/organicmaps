# Findlyts Onboarding Flow UI/UX Specification

## Production-Ready Mobile Specification

**Document:** `Findlyts Onboarding Flow UI UX Specification.md`
**Scope:** First-launch onboarding experience only
**Platform:** Native Android + iOS, shared business logic
**Visual source of truth:** Supplied Findlyts onboarding reference images
**Brand:** Findlyts

---

# 1. Purpose

The Findlyts onboarding experience introduces the product before authentication.

The onboarding should answer three questions in sequence:

1. **What is Findlyts?**
2. **What makes Findlyts different?**
3. **Why should the experience become personal to me?**

The experience consists of **three onboarding pages** followed by the existing authentication flow.

```text
App Launch
    ↓
Session Check
    ↓
Authenticated?
    ├── YES → Existing authenticated flow
    │
    └── NO
         ↓
   Onboarding State
         ↓
    Page 1
         ↓
    Page 2
         ↓
    Page 3
         ↓
   Authentication
         ↓
   Sign In / Sign Up
```

---

# 2. Design Direction

The onboarding must feel like **nightlife before the user even enters the app**.

### Visual personality

* Premium
* Energetic
* Social
* Dark
* Immersive
* Youthful
* Nightlife-focused
* Technologically polished

### Core visual language

* Near-black backgrounds
* Purple
* Magenta
* Pink
* Red
* Orange
* White typography
* Subdued gray secondary text
* Neon accent glows
* Rounded surfaces
* Gradient CTAs
* Large immersive imagery
* Subtle depth and shadows

Avoid:

* Generic SaaS aesthetics
* Bright white onboarding
* Excessive glassmorphism
* Excessive gradients everywhere
* Corporate illustrations
* Generic stock imagery
* Cluttered layouts

---

# 3. Onboarding Architecture

There are exactly three product-introduction pages.

| Page | Purpose                        | Primary CTA |
| ---- | ------------------------------ | ----------- |
| 1    | Introduce Findlyts             | Get Started |
| 2    | Explain live/social experience | Next        |
| 3    | Explain personalization        | Let's Go    |

### Navigation

```text
Page 1
   │
   ├── Get Started → Page 2
   │
   └── Sign in → Sign In

Page 2
   │
   ├── Back → Page 1
   ├── Next → Page 3
   └── Skip → Authentication

Page 3
   │
   ├── Back → Page 2
   ├── Let's Go → Authentication
   └── Sign in → Sign In
```

---

# 4. Critical Skip Behavior

**Skip must never mean "log in automatically" or "enter the app."**

Skip means:

> Skip the product introduction and continue to authentication.

```text
Skip
 ↓
Authentication Entry
 ↓
Sign In / Sign Up
```

This prevents an unauthenticated user from accidentally bypassing account creation.

---

# 5. Global Screen Structure

Every onboarding screen should use a consistent vertical composition.

```text
┌──────────────────────────────┐
│                              │
│       SYSTEM / SAFE AREA     │
│                              │
│        HERO / CONTENT        │
│                              │
│                              │
│                              │
│        SUPPORT CONTENT       │
│                              │
│                              │
│        PAGINATION            │
│                              │
│        PRIMARY CTA           │
│                              │
│       SECONDARY ACTION       │
│                              │
└──────────────────────────────┘
```

The exact proportions should adapt to device size.

Do not hard-code the reference image's pixel dimensions.

---

# 6. Page 1: Findlyts Introduction

## Purpose

Introduce the Findlyts proposition immediately.

The user should understand:

> Findlyts helps me discover what's happening, connect with people, and experience nightlife.

---

## 6.1 Hero

Display:

**Findlyts logo + wordmark**

Below:

**FIND YOUR LEVEL,**

**LIVE THE MOMENT**

### Logo

Use the official Findlyts logo asset.

Do not redraw the logo using typography.

The logo should be visually dominant without consuming excessive vertical space.

---

# 6.2 Hero Background

Use the supplied nightlife crowd visual.

Visual characteristics:

* Large crowd
* Raised hands
* Club lighting
* Purple light
* Pink light
* Red light
* Orange highlights
* Dark silhouettes
* High-energy atmosphere

The image should transition naturally into the dark lower portion of the screen.

Avoid a hard rectangular image boundary.

---

# 6.3 Value Proposition Row

Three feature blocks:

### DISCOVER

Icon:

Ticket / discovery

Text:

**DISCOVER**

The hottest events
near you

---

### CONNECT

Icon:

People/group

Text:

**CONNECT**

See who's going
and join

---

### EXPERIENCE

Icon:

Lightning

Text:

**EXPERIENCE**

Live club lobbies
and real vibes

---

## Layout

```text
       DISCOVER     CONNECT     EXPERIENCE

          🎟           👥            ⚡

       DISCOVER      CONNECT     EXPERIENCE

      The hottest   See who's    Live club
       events       going        lobbies
       near you     and join     and real vibes
```

Maintain equal visual weight.

---

# 6.4 Pagination

Three indicators.

Page 1:

```text
━━━━   ━━━   ━━━
```

Active indicator uses the Findlyts gradient.

Inactive indicators are muted.

The active indicator should be clearly distinguishable without being oversized.

---

# 6.5 Primary CTA

```text
┌──────────────────────────────────────┐
│             Get Started          →   │
└──────────────────────────────────────┘
```

Gradient:

```text
Purple → Magenta → Pink/Red → Orange
```

The CTA should have:

* rounded corners
* high contrast
* white text
* arrow icon
* press feedback
* disabled state
* accessibility label

---

# 6.6 Secondary Action

```text
Already have an account? Sign in
```

Only **Sign in** is accent-colored.

Tap:

```text
Page 1
 ↓
Sign In
```

---

# 7. Page 2: Live Social Experience

## Purpose

Explain the core live/social experience.

The user should understand:

> Findlyts lets me see what is happening at venues in real time.

---

# 7.1 Heading

```text
Stay in the loop.
```

The phrase:

**in the loop.**

uses the Findlyts gradient.

Example visual hierarchy:

```text
Stay
in the loop.
```

The actual line wrapping should adapt to device width.

---

# 7.2 Description

Display:

> Join live club lobbies, see what's happening in real time and never miss a moment.

Use secondary white/gray typography.

Keep the paragraph short and breathable.

---

# 7.3 Hero Device

The primary visual is a large tilted smartphone.

The phone demonstrates the Findlyts live lobby.

It should show:

* Venue
* Live indicator
* Venue imagery
* People in lobby
* User avatars
* Live conversation
* Reactions
* Message input

The device should be positioned dynamically rather than treated as a static screenshot.

---

# 7.4 Supporting Floating Elements

Around the phone:

### Community

People icon.

### Live activity

Audio/live activity icon.

### Reactions

Heart icon.

These should feel connected to the device through subtle visual paths/glows.

Avoid excessive animation.

---

# 7.5 Pagination

Page 2 active:

```text
━━━   ━━━━   ━━━
```

The middle indicator receives the active gradient.

---

# 7.6 Primary CTA

```text
Next →
```

Tap:

```text
Page 2
 ↓
Page 3
```

---

# 7.7 Skip

Display:

```text
Skip
```

Skip is a secondary text action.

It should be visually clear but less prominent than **Next**.

Tap:

```text
Page 2
 ↓
Authentication
```

---

# 8. Page 3: Personalized Nightlife

## Purpose

Explain that Findlyts adapts discovery to the user.

The user should understand:

> Findlyts isn't just showing everything. It helps surface things relevant to me.

---

# 8.1 Heading

```text
Your nightlife,
personalized.
```

The word:

**personalized.**

uses the Findlyts gradient.

---

# 8.2 Description

Display:

> Get event picks, updates and recommendations that match your vibe.

---

# 8.3 Hero Device

Display a large tilted smartphone.

The device demonstrates personalized discovery.

It should show:

* "For You"
* Event cards
* Event dates
* Venue names
* Distance
* Event imagery
* Recommendation indicators
* New event indicator

The phone should visually dominate the lower-middle portion of the screen.

---

# 8.4 Supporting Accent

Use the glowing star/recommendation visual.

The star represents:

**personalized discovery**

and should visually connect the concept to the phone.

---

# 8.5 Pagination

Page 3 active:

```text
━━━   ━━━   ━━━━━
```

---

# 8.6 Primary CTA

```text
Let's Go →
```

This is the completion CTA.

Tap:

```text
Let's Go
 ↓
Persist onboarding completion
 ↓
Authentication
```

---

# 8.7 Sign-In Shortcut

Display:

```text
Already have an account? Sign in
```

Tap:

```text
Page 3
 ↓
Sign In
```

---

# 9. Onboarding State

Separate **product introduction state** from **authenticated profile onboarding state**.

Recommended architecture:

```text
Local Onboarding State
        │
        ├── onboarding_not_started
        ├── onboarding_in_progress
        └── onboarding_completed
```

This controls the three-page introduction.

The existing profile field:

```text
onboarding_completed
```

should not automatically be repurposed unless the existing architecture explicitly defines it as the same concept.

If both concepts are required, keep them separate.

Example:

```text
has_seen_intro = true
profile.onboarding_completed = false
```

This can mean:

> User has seen the marketing/product introduction but hasn't completed
> authenticated profile onboarding.

---

# 10. First Launch

For a new unauthenticated installation:

```text
Launch
 ↓
Session restore
 ↓
No authenticated session
 ↓
Check intro state
 ↓
Not completed
 ↓
Page 1
```

---

# 11. Returning Unauthenticated User

If onboarding has already been completed:

```text
Launch
 ↓
No session
 ↓
Intro completed
 ↓
Authentication
```

Do not replay the onboarding carousel every launch.

---

# 12. Returning Authenticated User

If a valid authenticated session exists:

```text
Launch
 ↓
Restore session
 ↓
Authenticated
 ↓
Load profile
 ↓
Existing authenticated flow
```

Do not force the user through the marketing onboarding again.

---

# 13. Back Navigation

### Page 1

Back behavior depends on how the onboarding flow was entered.

### Page 2

```text
Back → Page 1
```

### Page 3

```text
Back → Page 2
```

### Authentication

Back behavior follows the existing authentication navigation architecture.

Never create navigation loops.

---

# 14. Touch Targets

All interactive controls must have accessible touch targets.

Minimum target:

**44–48 dp**

Recommended:

**48 dp**

Even when the visible icon is smaller, the clickable area should remain comfortably tappable.

---

# 15. Typography

Use the existing Findlyts typography system if available.

Hierarchy:

### Hero title

Large / bold.

### Supporting title

Medium-large / bold.

### Body

Regular.

### Feature labels

Medium / semibold.

### CTA

Medium / semibold.

### Secondary links

Medium.

Avoid excessive font weights.

---

# 16. Color Tokens

Suggested semantic tokens:

```text
Background
#03040A

Surface
#090B13

Surface Elevated
#10121B

Text Primary
#FFFFFF

Text Secondary
#B8B8C2

Text Muted
#777783

Accent Purple
#6A00FF

Accent Magenta
#C000FF

Accent Pink
#FF1493

Accent Red
#FF315B

Accent Orange
#FF8A00
```

If the project already has a design-token system, use its equivalent tokens rather than duplicating values.

---

# 17. Gradient

Primary Findlyts gradient:

```text
Purple
    ↓
Magenta
    ↓
Pink / Red
    ↓
Orange
```

Use primarily for:

* CTA buttons
* active pagination
* selected accent text
* selected interactive elements
* logo where appropriate

Do not cover the entire UI in gradients.

The gradient should feel like nightclub lighting, not wallpaper.

---

# 18. Motion Design

Onboarding should feel alive but remain fast.

### Page transition

Horizontal slide with subtle fade.

### Hero

Subtle entrance animation.

### Floating icons

Very subtle movement/glow.

### CTA

Small press-scale feedback.

### Pagination

Smooth active-state transition.

Avoid:

* aggressive bouncing
* continuous spinning
* excessive particle effects
* long transitions

Recommended transition duration:

**200–350 ms**

---

# 19. Image Loading

Core onboarding visuals should ideally be locally bundled.

Requirements:

* no visible layout shift
* no blank hero region
* no flashing placeholder
* preload next page where practical
* avoid unnecessary network dependency

If remote assets are used:

```text
Loading
 ↓
Cached asset
 ↓
Fallback asset
```

Never allow the onboarding experience to become unusable because a marketing image failed to load.

---

# 20. Accessibility

Each interactive element requires:

* semantic label
* sufficient contrast
* accessible touch target
* screen-reader description
* focus state where applicable

Example:

```text
Get Started button
```

not:

```text
Image button
```

Hero images should not overwhelm screen-reader navigation.

Decorative imagery should be marked decorative.

---

# 21. Analytics

If the existing application has analytics infrastructure, onboarding events may be tracked through it.

Recommended events:

```text
onboarding_started
onboarding_page_viewed
onboarding_page_2_viewed
onboarding_page_3_viewed
onboarding_skipped
onboarding_completed
onboarding_sign_in_clicked
```

Do not introduce a new analytics SDK merely for onboarding.

Respect the application's privacy architecture.

---

# 22. Error/Recovery Behavior

Onboarding itself should have almost no network dependency.

If an asset fails:

```text
Use bundled fallback.
```

If persistence fails:

```text
Keep onboarding usable.
Retry persistence.
```

Do not prevent the user from reaching authentication because a non-critical onboarding-state write failed.

---

# 23. Authentication Transition

When onboarding ends:

```text
Onboarding
 ↓
Authentication Entry
```

The transition should feel intentional.

Avoid suddenly changing:

* background color
* typography
* button styling
* logo treatment

Authentication should look like the next chapter of the same Findlyts experience.

---

# 24. Screen Inventory

The complete first-run UI should contain:

### Onboarding

1. `OnboardingPage1`
2. `OnboardingPage2`
3. `OnboardingPage3`

### Authentication

4. `SignIn`
5. `SignUp`

### Authentication support

6. `ForgotPassword`
7. `EmailVerification`
8. `AuthenticationLoading`
9. `AuthenticationError`

### Post-sign-up

10. `ProfileSetup`
11. `WelcomeSuccess`

These authentication screens are outside the core onboarding flow but must visually continue the same design system.

---

# 25. Component Architecture

Recommended reusable components:

```text
FindlytsLogo
FindlytsGradientButton
OnboardingHero
OnboardingPagination
OnboardingFeatureItem
OnboardingPhoneMockup
OnboardingFloatingIcon
OnboardingFooterAction
```

Avoid duplicating these components across pages.

---

# 26. Suggested Screen Model

```text
OnboardingState

currentPage
introCompleted
isTransitioning
```

Events:

```text
Next
Back
Skip
GetStarted
Let'sGo
SignIn
```

State machine:

```text
PAGE_1
  │
  ├── GetStarted → PAGE_2
  └── SignIn → SIGN_IN

PAGE_2
  │
  ├── Back → PAGE_1
  ├── Next → PAGE_3
  └── Skip → AUTH_ENTRY

PAGE_3
  │
  ├── Back → PAGE_2
  ├── Let'sGo → AUTH_ENTRY
  └── SignIn → SIGN_IN
```

---

# 27. UI/UX Acceptance Criteria

The onboarding is complete only when:

### Page 1

* [ ] Findlyts branding correct
* [ ] Hero image correct
* [ ] Tagline correct
* [ ] Discover feature correct
* [ ] Connect feature correct
* [ ] Experience feature correct
* [ ] Pagination correct
* [ ] Get Started correct
* [ ] Sign-in shortcut correct

### Page 2

* [ ] Heading correct
* [ ] Gradient typography correct
* [ ] Description correct
* [ ] Live lobby phone visual correct
* [ ] Floating social icons correct
* [ ] Pagination correct
* [ ] Next correct
* [ ] Skip correct
* [ ] Back correct

### Page 3

* [ ] Heading correct
* [ ] Gradient typography correct
* [ ] Description correct
* [ ] Personalized phone visual correct
* [ ] Star/recommendation visual correct
* [ ] Pagination correct
* [ ] Let's Go correct
* [ ] Sign-in shortcut correct
* [ ] Back correct

### System

* [ ] First-launch state works
* [ ] Completed state persists
* [ ] Skip persists correctly
* [ ] App restart works
* [ ] Authenticated users bypass introduction
* [ ] Authentication transition works
* [ ] Responsive layouts work
* [ ] Keyboard/accessibility behavior is correct
* [ ] Animations are smooth
* [ ] No layout jumps
* [ ] No unnecessary network dependency
* [ ] No duplicate onboarding state
* [ ] No navigation loops

---

# 28. Final Experience

The intended user journey is:

```text
             FINDLYTS
                 │
                 ▼
       ┌───────────────────┐
       │   PAGE 1          │
       │                   │
       │ Discover          │
       │ Connect           │
       │ Experience        │
       │                   │
       │  Get Started →    │
       └─────────┬─────────┘
                 │
                 ▼
       ┌───────────────────┐
       │   PAGE 2          │
       │                   │
       │ Stay in the loop. │
       │                   │
       │ Live Club Lobby   │
       │                   │
       │  Next →           │
       └─────────┬─────────┘
                 │
                 ▼
       ┌───────────────────┐
       │   PAGE 3          │
       │                   │
       │ Your nightlife,   │
       │ personalized.     │
       │                   │
       │ Recommendations   │
       │                   │
       │  Let's Go →       │
       └─────────┬─────────┘
                 │
                 ▼
       ┌───────────────────┐
       │ AUTHENTICATION     │
       │                   │
       │ Sign In           │
       │ Create Account    │
       └─────────┬─────────┘
                 │
                 ▼
             SUPABASE
                 │
                 ▼
              PROFILE
                 │
                 ▼
            FINDLYTS APP
```

**Core UX principle:** onboarding sells the *experience*, authentication creates the *identity*, and profile setup creates the *personal Findlyts experience*.
