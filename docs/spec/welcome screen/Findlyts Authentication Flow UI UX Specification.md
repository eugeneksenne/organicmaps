# Findlyts Authentication Flow UI/UX Specification

**Document:** `Findlyts Authentication Flow UI UX Specification.md`
**Scope:** Authentication experience after onboarding
**Product:** Findlyts
**Platforms:** Native Android + iOS
**Visual source of truth:** Supplied Findlyts authentication reference screens
**Technical source of truth:** Existing authentication architecture and Supabase Auth

---

# 1. Purpose

The Findlyts authentication experience begins after the user completes or skips the three-page onboarding experience.

Its job is to:

1. Let existing users securely sign in.
2. Let new users create a Findlyts account.
3. Support the configured social authentication providers.
4. Recover forgotten passwords.
5. Handle authentication loading and errors.
6. Verify email when required.
7. Connect successful authentication to the user's Findlyts profile.
8. Never allow navigation into the authenticated application without a valid authenticated state and profile.

The authentication UI must feel like a continuation of the onboarding experience, not a separate application.

---

# 2. Authentication Entry

After onboarding:

```text
Onboarding
    ↓
Authentication Entry
    ↓
┌───────────────┐
│               │
│   Sign In     │
│               │
│  Create       │
│  Account      │
│               │
└───────────────┘
```

The existing reference design uses direct navigation between Sign In and Sign Up.

Therefore:

```text
Sign In
   ↕
Sign Up
```

No unnecessary authentication landing screen should be introduced if the existing navigation architecture already provides the correct entry point.

---

# 3. Global Authentication Design

Authentication must preserve the Findlyts visual identity established during onboarding.

## Background

Use:

* Near-black
* Very dark navy/black
* Extremely subtle tonal variation

Avoid a bright background.

---

## Typography

Primary:

**White**

Secondary:

**Light gray**

Interactive:

**Findlyts pink/magenta**

Gradient text may be used selectively for important branded elements.

---

## Surfaces

Input fields and provider buttons use:

* dark surfaces
* subtle borders
* rounded corners
* minimal elevation
* clear separation from the background

---

# 4. Authentication Screen Inventory

The authentication system should contain the following screens/states:

### Core

1. Sign In
2. Create Account

### Supporting

3. Forgot Password
4. Email Verification
5. Authentication Loading
6. Authentication Error

### Post-registration

7. Profile Setup, if required
8. Account Created / Welcome

---

# 5. SIGN IN

## 5.1 Screen Purpose

Allow an existing Findlyts user to securely authenticate.

---

# 5.2 Top Navigation

At the top:

```text
←                                      Skip
```

### Back

The back arrow returns to the previous authentication/onboarding destination according to the existing navigation stack.

### Skip

The authentication reference shows **Skip**.

However, Skip must follow the application's intended navigation behavior.

It must **not**:

* authenticate the user
* create an account
* bypass authentication security
* open protected application screens

If onboarding has already been completed and Skip is not meaningful in the current state, use the existing navigation behavior rather than inventing a new bypass.

---

# 5.3 Brand

Centered near the top:

**Findlyts logo**

Use the official existing Findlyts logo asset.

Do not recreate it with text.

---

# 5.4 Heading

```text
Welcome back
```

Large, bold, white.

---

# 5.5 Subtitle

```text
Sign in to continue your journey
```

Secondary light-gray text.

---

# 5.6 Identifier Field

Placeholder:

```text
Email or phone number
```

Leading icon:

Envelope/contact icon.

The field must support the identifier methods actually configured by the authentication backend.

Do not visually promise phone authentication if the backend does not support it.

---

# 5.7 Password Field

Placeholder:

```text
Password
```

Leading:

Lock icon.

Trailing:

Password visibility icon.

Behavior:

```text
Hidden
   ↕
Visible
```

The password must remain hidden by default.

---

# 5.8 Forgot Password

Display below the password field:

```text
Forgot password?
```

Accent color:

Findlyts pink/magenta.

Tap:

```text
Sign In
   ↓
Forgot Password
```

---

# 5.9 Primary CTA

```text
┌────────────────────────────────┐
│             Sign In             │
└────────────────────────────────┘
```

Use the Findlyts gradient:

**Purple → Magenta/Pink → Red → Orange**

States:

```text
Enabled
Disabled
Loading
Success
Error
```

While loading:

* prevent duplicate submission
* preserve entered values
* show progress
* prevent conflicting navigation

---

# 5.10 Social Authentication

Divider:

```text
────────── or continue with ──────────
```

Providers in the reference order:

1. Google
2. Apple
3. Spotify
4. X
5. Facebook

Each provider is represented by a compact rounded card.

```text
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│  G   │ │     │ │  ●   │ │  X   │ │  f   │
│Google│ │ Apple │ │Spotify│ │  X   │ │Facebook│
└──────┘ └──────┘ └──────┘ └──────┘ └──────┘
```

Use the existing provider authentication implementation.

Never simulate successful OAuth.

---

# 5.11 Sign-Up Footer

Bottom:

```text
Don't have an account? Sign up
```

Only:

**Sign up**

is accent-colored.

Tap:

```text
Sign In
 ↓
Sign Up
```

---

# 6. CREATE ACCOUNT

## 6.1 Purpose

Allow a new user to create their Findlyts account.

---

# 6.2 Top Navigation

```text
←                                      Skip
```

Back follows the existing authentication navigation stack.

Skip must never create an authenticated state or bypass required authentication.

---

# 6.3 Brand

Centered Findlyts logo.

---

# 6.4 Heading

```text
Create your account
```

Large, bold, white.

---

# 6.5 Subtitle

```text
Join findlyts and live the moment
```

Secondary text.

Use the Findlyts brand name consistently.

---

# 7. Registration Form

Fields must appear in this order.

```text
Full name

Email or phone number

Password

Confirm password
```

---

# 7.1 Full Name

Placeholder:

```text
Full name
```

Leading icon:

Person.

Validation:

* required
* meaningful value
* sensible length

Do not impose arbitrary restrictions that conflict with the existing profile system.

---

# 7.2 Email / Phone

Placeholder:

```text
Email or phone number
```

Leading icon:

Envelope/contact.

Use the authentication backend's supported identifier methods.

---

# 7.3 Password

Placeholder:

```text
Password
```

Leading:

Lock.

Trailing:

Visibility toggle.

---

# 7.4 Confirm Password

Placeholder:

```text
Confirm password
```

Leading:

Lock.

Trailing:

Visibility toggle.

Validation:

```text
password == confirmPassword
```

If they differ:

Display a clear inline validation error.

---

# 8. Terms Agreement

Below the form:

```text
○  I agree to the Terms of Service
   and Privacy Policy
```

The checkbox must be interactive.

Terms of Service:

* tappable
* opens the existing legal destination

Privacy Policy:

* tappable
* opens the existing legal destination

The Sign Up action must not proceed when acceptance is required and the checkbox is unchecked.

---

# 9. Sign Up CTA

```text
┌────────────────────────────────┐
│             Sign Up             │
└────────────────────────────────┘
```

Findlyts gradient.

States:

* enabled
* disabled
* loading
* success
* error

Prevent duplicate registration requests.

---

# 10. Social Registration

Divider:

```text
────────── or continue with ──────────
```

Providers:

1. Google
2. Apple
3. Spotify
4. X
5. Facebook

Use the same OAuth architecture as Sign In.

The authentication result must enter the same profile synchronization pipeline.

---

# 11. Sign-In Footer

Bottom:

```text
Already have an account? Sign in
```

Tap:

```text
Sign Up
   ↓
Sign In
```

---

# 12. FORGOT PASSWORD

## Purpose

Allow an existing user to recover access.

Screen structure:

```text
←

        [Findlyts / icon]

Reset your password

Enter your email or phone number
and we'll send you a link to reset it.

[ Email or phone number ]

[ Send Reset Link ]

Back to sign in
```

Use the existing Supabase password recovery flow.

Do not build a fake password reset mechanism.

---

# 13. Password Reset States

### Initial

```text
Reset your password
```

### Sending

```text
Sending...
```

### Success

```text
Reset link sent

Check your email for instructions.
```

### Error

```text
We couldn't send the reset link.
Please check your details and try again.
```

Provide:

**Try Again**

---

# 14. EMAIL VERIFICATION

If email verification is enabled by the existing authentication configuration:

```text
Account created
       ↓
Email verification required
       ↓
Verification screen
```

Suggested UI:

```text
        ✓

Email verified!

Your email has been successfully
verified. You can now sign in.

[ Continue to Sign In ]
```

The verification screen should use the same Findlyts visual system.

---

# 15. AUTHENTICATION LOADING

Authentication operations must have explicit loading states.

Example:

```text
Welcome back

[ email ]

[ password ]

┌────────────────────────────────┐
│       ◌  Signing in...         │
└────────────────────────────────┘
```

During loading:

* disable CTA
* prevent duplicate requests
* preserve input
* maintain navigation state
* allow cancellation only if supported safely

---

# 16. INVALID CREDENTIALS

When credentials are invalid:

Keep the form populated where safe.

Display an inline error.

Example:

```text
The email or password you entered
is incorrect.
```

Do not reveal whether a particular account exists when doing so could create an account-enumeration risk.

---

# 17. SIGN-UP VALIDATION

Examples:

### Empty name

```text
Please enter your full name.
```

### Invalid identifier

```text
Please enter a valid email or phone number.
```

### Password mismatch

```text
Passwords don't match.
```

### Terms not accepted

```text
Please accept the Terms of Service
and Privacy Policy to continue.
```

Errors should appear close to the affected control.

---

# 18. Authentication State Machine

The authentication layer should expose a centralized state.

```text
UNAUTHENTICATED
      │
      ▼
AUTHENTICATING
      │
      ├── ERROR
      │
      ▼
AUTHENTICATED
      │
      ▼
PROFILE_SYNC
      │
      ├── PROFILE_ERROR
      │
      ▼
READY
```

For sign-up:

```text
SIGN_UP
   ↓
AUTHENTICATING
   ↓
EMAIL_VERIFICATION_REQUIRED?
   ├── YES → EMAIL_VERIFICATION
   │
   └── NO
        ↓
    PROFILE_SYNC
        ↓
       READY
```

---

# 19. Profile Synchronization

Authentication success must never directly equal application readiness.

Correct flow:

```text
Supabase Authentication
        ↓
Authenticated User
        ↓
auth.users.id
        ↓
Findlyts Profile
        ↓
Profile Exists?
      /     \
    YES      NO
     ↓        ↓
 Load       Create
 Profile    Profile
     \        /
      \      /
       ↓    ↓
     Current Profile
          ↓
        READY
```

Only `READY` can enter the authenticated application.

---

# 20. New Account Profile

After successful registration:

```text
auth.users.id
      ↓
profiles.id
```

Populate the profile using the existing profile architecture.

Expected initial values where supported:

```text
id
email
display_name
username
avatar_url
bio
created_at
updated_at
last_login
account_status
onboarding_completed
```

Do not create a second user table.

---

# 21. Returning User

Existing user:

```text
Sign In
 ↓
Supabase Auth
 ↓
auth.user
 ↓
Find profile
 ↓
Profile exists
 ↓
Update last_login
 ↓
Load current profile
 ↓
READY
 ↓
Application
```

No duplicate profile should be created.

---

# 22. OAuth Flow

All social authentication must eventually converge on the same pipeline.

```text
Google / Apple / Spotify / X / Facebook
                ↓
              OAuth
                ↓
         Supabase Session
                ↓
         Authentication Manager
                ↓
          Profile Sync
                ↓
             READY
```

Do not create separate profile creation logic for each provider.

---

# 23. Provider Failure

If OAuth fails:

Remain unauthenticated.

Display a recoverable error.

Example:

```text
We couldn't sign you in with Google.
Please try again.
```

Never:

* fabricate authentication
* create an incomplete session
* navigate to Home
* create a profile without a valid authenticated identity

---

# 24. Navigation Guards

The application must enforce:

```text
Unauthenticated
      ↓
Authentication

Authenticated
but no profile
      ↓
Profile synchronization

Authenticated
+ valid profile
      ↓
Application
```

Protected screens must not be reachable through UI navigation alone.

The navigation layer must derive access from authentication state.

---

# 25. Back Navigation

Authentication navigation must not create loops.

Example:

```text
Onboarding
    ↓
Sign In
    ↓
Sign Up
```

Back:

```text
Sign Up
 ↓
Sign In
```

Back:

```text
Sign In
 ↓
Previous onboarding/auth entry
```

Use the existing navigation architecture rather than introducing a second navigation stack.

---

# 26. Keyboard Behavior

Authentication forms must remain usable when the keyboard appears.

Requirements:

* fields remain visible
* focused field scrolls into view
* CTA remains reachable
* keyboard dismissal works
* no content is permanently hidden
* scrolling is smooth

Support different device heights.

---

# 27. Input Behavior

### Email/phone

Use appropriate keyboard configuration.

### Password

Use secure text entry.

### Confirm password

Use secure text entry.

### Full name

Use standard text input.

Do not unnecessarily auto-capitalize email addresses.

---

# 28. Accessibility

Every field must have:

* semantic label
* correct input type
* meaningful error description
* accessible focus behavior

Buttons require:

* accessible label
* adequate touch target
* pressed state

Password visibility controls must have meaningful descriptions.

---

# 29. Responsive Design

Do not reproduce the reference using fixed screen coordinates.

The reference defines:

* proportions
* hierarchy
* spacing relationships
* visual weight

The implementation must adapt to:

* small phones
* standard phones
* large phones
* display cutouts
* different aspect ratios
* accessibility font scaling

---

# 30. Animation

Keep authentication animation subtle.

Recommended:

* screen transition: 200–350 ms
* button press feedback
* field focus transition
* error appearance
* loading indicator

Avoid excessive motion.

Authentication should feel fast and trustworthy.

---

# 31. Security UX

Never display:

* access tokens
* refresh tokens
* passwords
* sensitive backend errors
* internal database errors

Do not log credentials.

Do not expose account existence unnecessarily through error messages.

Use Supabase Auth as the authentication authority.

---

# 32. Visual Component System

Reuse existing components where possible.

Recommended reusable components:

```text
FindlytsAuthHeader
FindlytsLogo
AuthInputField
PasswordInputField
FindlytsGradientButton
SocialAuthButton
AuthDivider
AuthFooterLink
AuthErrorMessage
AuthLoadingState
AuthCheckbox
```

Do not create duplicates if equivalent components already exist.

---

# 33. Screen Layout Reference

## Sign In

```text
┌──────────────────────────────┐
│ ←                         Skip│
│                              │
│            [ F ]             │
│                              │
│        Welcome back          │
│                              │
│ Sign in to continue your     │
│ journey                      │
│                              │
│ ┌──────────────────────────┐ │
│ │ ✉  Email or phone number │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ 🔒 Password          ◉   │ │
│ └──────────────────────────┘ │
│                              │
│              Forgot password?│
│                              │
│ ┌──────────────────────────┐ │
│ │         Sign In          │ │
│ └──────────────────────────┘ │
│                              │
│ ────── or continue with ──── │
│                              │
│ G          ●     X     f    │
│                              │
│ Don't have an account? Sign up│
└──────────────────────────────┘
```

---

# 34. Create Account

```text
┌──────────────────────────────┐
│ ←                         Skip│
│                              │
│            [ F ]             │
│                              │
│     Create your account      │
│                              │
│ Join findlyts and live the   │
│ moment                       │
│                              │
│ ┌──────────────────────────┐ │
│ │ ♙  Full name             │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ ✉  Email or phone number │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ 🔒 Password          ◉   │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ 🔒 Confirm password  ◉   │ │
│ └──────────────────────────┘ │
│                              │
│ ○ I agree to the Terms of    │
│   Service and Privacy Policy │
│                              │
│ ┌──────────────────────────┐ │
│ │         Sign Up          │ │
│ └──────────────────────────┘ │
│                              │
│ ────── or continue with ──── │
│                              │
│ G          ●     X     f    │
│                              │
│ Already have an account?     │
│ Sign in                      │
└──────────────────────────────┘
```

---

# 35. Authentication → Profile → App

The final transition is:

```text
                    AUTHENTICATION
                         │
             ┌───────────┴───────────┐
             │                       │
          Sign In                 Sign Up
             │                       │
             └───────────┬───────────┘
                         ↓
                  Supabase Auth
                         ↓
                  Authenticated
                         ↓
                   Profile Sync
                         ↓
                 Profile Available
                         ↓
              ┌──────────┴──────────┐
              │                     │
       Profile onboarding      Already complete
              │                     │
              └──────────┬──────────┘
                         ↓
                    Findlyts App
```

---

# 36. Acceptance Criteria

The authentication experience is complete only when:

### Sign In

* [ ] Correct Findlyts branding
* [ ] Correct heading
* [ ] Correct subtitle
* [ ] Email/phone field
* [ ] Password field
* [ ] Password visibility toggle
* [ ] Forgot password
* [ ] Gradient Sign In button
* [ ] Social providers
* [ ] Sign-up navigation
* [ ] Back navigation
* [ ] Loading state
* [ ] Error state

### Sign Up

* [ ] Correct branding
* [ ] Full name
* [ ] Email/phone
* [ ] Password
* [ ] Confirm password
* [ ] Password visibility
* [ ] Terms checkbox
* [ ] Terms link
* [ ] Privacy link
* [ ] Gradient Sign Up button
* [ ] Social providers
* [ ] Sign-in navigation
* [ ] Validation
* [ ] Loading
* [ ] Error state

### Recovery

* [ ] Forgot-password screen
* [ ] Reset request
* [ ] Success state
* [ ] Error state
* [ ] Return to sign in

### Authentication

* [ ] Supabase authentication works
* [ ] OAuth works for configured providers
* [ ] Session restoration works
* [ ] Logout works
* [ ] Duplicate requests prevented

### Profile

* [ ] Profile loaded after authentication
* [ ] Profile created for new users
* [ ] Exactly one profile per auth user
* [ ] `last_login` updated
* [ ] Profile state cleared on logout

### Security

* [ ] RLS respected
* [ ] No credentials logged
* [ ] No tokens exposed
* [ ] No fake authentication
* [ ] No protected navigation before authentication/profile readiness

### UX

* [ ] Responsive
* [ ] Keyboard-safe
* [ ] Accessible
* [ ] Smooth transitions
* [ ] No layout jumps
* [ ] Authentication visually matches onboarding

---

# 37. Final Experience

The complete Findlyts first-run journey should now feel like one continuous story:

```text
┌──────────────────────┐
│ FINDLYTS ONBOARDING  │
│                      │
│ Discover             │
│ Connect              │
│ Experience           │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ LIVE EXPERIENCE      │
│                      │
│ Stay in the loop     │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ PERSONALIZATION      │
│                      │
│ Your nightlife,      │
│ personalized.        │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ AUTHENTICATION       │
│                      │
│ Welcome back         │
│ Create your account  │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ PROFILE              │
│                      │
│ Your Findlyts        │
│ identity             │
└──────────┬───────────┘
           ↓
┌──────────────────────┐
│ FINDLYTS             │
│                      │
│ Discover • Feed      │
│ Map • Chats • Profile│
└──────────────────────┘
```

This keeps **onboarding, authentication, profile creation, and entry into the app as one coherent UX system**, while keeping the underlying authentication and profile responsibilities properly separated.
