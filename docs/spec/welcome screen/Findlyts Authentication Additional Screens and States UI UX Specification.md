# Findlyts Authentication: Additional Screens & States UI/UX Specification

**Document:** `Findlyts Authentication Additional Screens and States UI UX Specification.md`
**Scope:** All secondary authentication screens, edge states, transition states, and recovery states that surround the core **Sign In** and **Create Account** screens.

This specification extends the existing Findlyts Authentication Flow specification. It does **not** replace the core Sign In / Sign Up designs.

---

# 1. Additional Authentication Screen Map

The complete authentication system should now cover:

```text
                         AUTHENTICATION
                               │
              ┌────────────────┴────────────────┐
              │                                 │
           SIGN IN                           SIGN UP
              │                                 │
      ┌───────┼────────┐              ┌─────────┼─────────┐
      │       │        │              │         │         │
      ▼       ▼        ▼              ▼         ▼         ▼
 Forgot    OAuth     Auth Error    Verify    Profile    Auth Error
 Password  Flow                    Email     Setup
      │       │                     │
      ▼       ▼                     ▼
 Reset     Provider              Verification
 Password   Result                 Success
      │
      ▼
 Reset Success
```

Additional global states:

* Authentication loading
* Network unavailable
* Session expired
* Session restoration
* Account disabled
* Rate limited
* OAuth cancelled
* OAuth failed
* Email verification pending
* Email verification success
* Email verification failed
* Password reset success
* Password reset expired
* Profile creation failure
* Profile synchronization failure
* Authentication success
* Logout confirmation

---

# 2. Design Principle

Every state must feel like the **same Findlyts application**.

Do not create separate visual designs for errors, recovery, or verification.

The system should retain:

* dark nightlife background
* Findlyts logo
* rounded surfaces
* white primary typography
* muted secondary typography
* purple/magenta/pink/orange accents
* Findlyts gradient CTA
* subtle neon glow
* generous spacing

The error state should feel like a controlled detour, not an application crash.

---

# 3. Authentication Loading Screen

## Purpose

Used when the application is restoring or establishing authentication.

### Layout

```text
┌──────────────────────────────┐
│                              │
│                              │
│            [ F ]             │
│                              │
│        Welcome to            │
│        Findlyts              │
│                              │
│             ◌                │
│                              │
│      Getting things ready    │
│                              │
└──────────────────────────────┘
```

### Behavior

Use when:

* restoring Supabase session
* completing OAuth
* creating authentication session
* loading authentication state

Do not display the full authentication form while the application is still resolving an existing session.

---

# 4. Session Restoration State

This is a **transient state**, not a permanent screen.

```text
APP LAUNCH
    ↓
Restore Session
    ↓
┌────────────────────┐
│ Checking session... │
└────────────────────┘
    ↓
Authenticated?
   / \
 YES  NO
  ↓    ↓
Profile Auth
  ↓
App
```

The user should normally see this only briefly.

---

# 5. OAuth Provider Loading

When a user taps Google, Apple, Spotify, X, or Facebook:

```text
Provider Button
      ↓
Provider Loading
      ↓
OAuth
```

The selected provider button becomes unavailable temporarily.

Example:

```text
┌─────────────┐
│     ◌       │
│   Google    │
└─────────────┘
```

Other provider buttons should also be disabled while the OAuth operation is active.

This prevents multiple simultaneous authentication attempts.

---

# 6. OAuth Cancelled

If the user closes or cancels the provider authentication flow:

Return to the authentication screen.

Do not show a dramatic error.

Optional lightweight message:

> Sign-in was cancelled.

The user remains unauthenticated.

---

# 7. OAuth Failure

If the provider authentication fails:

```text
┌──────────────────────────────┐
│                              │
│        Couldn't sign in      │
│                              │
│  We couldn't complete your   │
│  sign-in with Google.        │
│                              │
│       [ Try Again ]          │
│                              │
│       Back to Sign In        │
│                              │
└──────────────────────────────┘
```

Provider name should be dynamically inserted.

Do not expose raw provider/backend errors.

---

# 8. Account Already Exists

If registration detects an existing authentication identity, provide a useful recovery path.

```text
Account already exists

It looks like you already have a
Findlyts account.

[ Sign In ]

[ Forgot Password? ]
```

Do not create a duplicate profile.

---

# 9. Email Verification Pending

If email confirmation is required:

```text
┌──────────────────────────────┐
│ ←                            │
│                              │
│            [ F ]             │
│                              │
│      Check your email        │
│                              │
│ We sent a verification link  │
│ to your email address.       │
│                              │
│         ✉                    │
│                              │
│ [ Open Email ]               │
│                              │
│ Resend email                 │
│                              │
│ Already verified?            │
│ Check again                  │
│                              │
│ Back to Sign In              │
└──────────────────────────────┘
```

---

# 10. Resend Verification

The user can request another verification email.

Initial:

```text
Resend email
```

Loading:

```text
Sending...
```

Success:

```text
Verification email sent.
```

Cooldown:

```text
Resend available in 45s
```

The cooldown prevents repeated requests.

---

# 11. Email Verification Success

When verification succeeds:

```text
┌──────────────────────────────┐
│                              │
│            ✓                 │
│                              │
│      Email verified          │
│                              │
│ Your email has been          │
│ successfully verified.      │
│                              │
│       [ Continue ]           │
│                              │
└──────────────────────────────┘
```

Continue should move into the correct authenticated/profile flow.

---

# 12. Email Verification Failure

If the verification link is invalid:

```text
Verification link unavailable

This verification link is invalid
or can no longer be used.

[ Send New Link ]

[ Back to Sign In ]
```

---

# 13. Expired Verification Link

Separate expired-link state:

```text
This link has expired

For your security, verification
links expire after a period of time.

[ Send New Verification Link ]
```

Avoid technical terminology such as JWT or token expiration.

---

# 14. Forgot Password Screen

## Layout

```text
┌──────────────────────────────┐
│ ←                            │
│                              │
│            [ F ]             │
│                              │
│     Forgot your password?    │
│                              │
│ Enter your email or phone    │
│ number to reset your         │
│ password.                   │
│                              │
│ ┌──────────────────────────┐ │
│ │ ✉ Email or phone number  │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │       Send Reset Link     │ │
│ └──────────────────────────┘ │
│                              │
│        Back to Sign In       │
└──────────────────────────────┘
```

---

# 15. Password Reset Sent

```text
┌──────────────────────────────┐
│                              │
│            ✉                 │
│                              │
│       Check your email       │
│                              │
│ If an account exists for     │
│ that address, we've sent     │
│ instructions to reset your   │
│ password.                   │
│                              │
│ [ Open Email ]               │
│                              │
│ Didn't receive it?           │
│ Resend                       │
│                              │
│ Back to Sign In              │
└──────────────────────────────┘
```

Use wording that does not unnecessarily reveal whether an account exists.

---

# 16. Reset Password Screen

When the user opens a valid password-reset flow:

```text
┌──────────────────────────────┐
│ ←                            │
│                              │
│            [ F ]             │
│                              │
│       Create new password    │
│                              │
│ Enter a new password for     │
│ your Findlyts account.       │
│                              │
│ ┌──────────────────────────┐ │
│ │ 🔒 New password       ◉  │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │ 🔒 Confirm password   ◉  │ │
│ └──────────────────────────┘ │
│                              │
│ ┌──────────────────────────┐ │
│ │     Update Password      │ │
│ └──────────────────────────┘ │
└──────────────────────────────┘
```

---

# 17. Password Requirements

If the backend has defined password requirements, surface them clearly.

Example:

```text
Password requirements

✓ Minimum required length
✓ Contains required character types
✓ Passwords match
```

Do not invent requirements that the backend does not enforce.

The UI and backend validation must always agree.

---

# 18. Password Reset Success

```text
┌──────────────────────────────┐
│                              │
│            ✓                 │
│                              │
│      Password updated        │
│                              │
│ Your password has been       │
│ successfully changed.       │
│                              │
│       [ Sign In ]            │
│                              │
└──────────────────────────────┘
```

---

# 19. Password Reset Expired

```text
Reset link expired

This password reset link is no
longer valid.

[ Request New Link ]

[ Back to Sign In ]
```

---

# 20. Password Reset Invalid

```text
Unable to reset password

This reset link is invalid or
cannot be used.

[ Request New Link ]

[ Back to Sign In ]
```

---

# 21. Generic Authentication Error

Use when the specific cause cannot safely or reliably be exposed.

```text
Something went wrong

We couldn't complete that request.
Please try again.

[ Try Again ]

[ Back ]
```

Do not display:

```text
PostgREST error 500
JWTException
AuthApiError
database constraint failed
```

Those belong in internal logs, not the UI.

---

# 22. Network Offline

If authentication requires network connectivity:

```text
┌──────────────────────────────┐
│                              │
│            ⌁                 │
│                              │
│      You're offline          │
│                              │
│ Check your internet           │
│ connection and try again.    │
│                              │
│       [ Try Again ]          │
│                              │
└──────────────────────────────┘
```

The UI should not falsely claim authentication failed when the actual problem is connectivity.

---

# 23. Server Temporarily Unavailable

```text
Findlyts is having trouble

We couldn't reach the
authentication service right now.

[ Try Again ]
```

Use retry with appropriate backoff internally.

---

# 24. Rate Limited

If the backend indicates that too many attempts have occurred:

```text
Too many attempts

For your security, please wait
a little before trying again.

Try again later.
```

If a countdown is provided by the backend, display it.

Do not expose internal rate-limit implementation details.

---

# 25. Account Disabled

If the backend explicitly indicates the account is disabled:

```text
Account unavailable

Your Findlyts account is currently
unavailable.

Please contact support if you
believe this is a mistake.

[ Contact Support ]

[ Back to Sign In ]
```

Only show this state when the backend actually provides this condition.

---

# 26. Profile Creation State

After successful registration:

```text
Authentication
      ↓
Account Created
      ↓
Creating Profile
      ↓
Profile Ready
```

Transient UI:

```text
Creating your Findlyts profile...
```

The user should not be dumped onto the main application while profile creation is incomplete.

---

# 27. Profile Creation Failure

If authentication succeeds but profile creation fails:

```text
Your account is secure,
but we couldn't finish setting up
your Findlyts profile.

[ Try Again ]

[ Sign Out ]
```

Important:

The authenticated session must not be silently abandoned.

Retry should use the authenticated identity.

---

# 28. Profile Synchronization Failure

For an existing user:

```text
Couldn't load your profile

You're signed in, but we couldn't
load your Findlyts profile.

[ Try Again ]

[ Sign Out ]
```

This is different from authentication failure.

The authentication session remains valid.

---

# 29. Authentication Success

A full success screen is not always necessary.

Prefer:

```text
Sign In
 ↓
Authentication
 ↓
Profile Sync
 ↓
Application
```

rather than adding an unnecessary "Success!" page.

A very short branded transition can be used:

```text
[F]

Welcome back.
```

Then transition into the application.

---

# 30. New User Profile Setup

After registration, if required by the product's profile architecture:

```text
Create Account
      ↓
Email Verification
      ↓
Profile Setup
```

Profile setup should collect only information required to establish the initial Findlyts identity.

Recommended conceptual structure:

```text
Profile Setup

Profile photo
Display name
Username
Bio
```

Additional personalization should be handled by the dedicated profile/onboarding system rather than bloating authentication.

---

# 31. Username Availability

If username creation is part of profile setup:

```text
Username

@koketsoe

✓ Username available
```

States:

```text
Checking...
Available
Unavailable
Invalid
```

Use debounce before checking availability.

Do not issue a network request on every keystroke.

---

# 32. Profile Photo State

Profile photo selection:

```text
┌──────────────┐
│              │
│      +       │
│              │
└──────────────┘

Add profile photo
```

States:

```text
Empty
Selecting
Uploading
Uploaded
Upload failed
Retry
```

If profile photo upload is optional, failure must not prevent account creation unless the product explicitly requires it.

---

# 33. Logout Confirmation

Logout should use a confirmation surface.

```text
Sign out?

You'll need to sign in again
to access your Findlyts account.

[ Cancel ]

[ Sign Out ]
```

Do not make logout visually resemble a destructive account deletion action.

---

# 34. Session Expired

If the user is already inside Findlyts and the authentication session expires:

```text
Your session has expired

Please sign in again to continue.

[ Sign In ]
```

Do not silently send the user to the Sign In screen without context.

---

# 35. Session Conflict

If the application detects an invalid or inconsistent session:

```text
We need you to sign in again

Your session is no longer valid.

[ Sign In ]
```

Clear only the invalid local session state.

Do not delete the user's profile.

---

# 36. Account Deletion Boundary

Account deletion is **not** an authentication error state.

It should be handled by the account/profile settings architecture.

Authentication must distinguish:

```text
Sign Out
```

from:

```text
Delete Account
```

They must never share destructive behavior.

---

# 37. Universal State Model

Recommended authentication state model:

```text
AuthState

UNKNOWN
RESTORING_SESSION

UNAUTHENTICATED

SIGNING_IN
SIGNING_UP

OAUTH_LOADING
OAUTH_CANCELLED
OAUTH_ERROR

EMAIL_VERIFICATION_REQUIRED
VERIFYING_EMAIL
EMAIL_VERIFIED
EMAIL_VERIFICATION_ERROR

PASSWORD_RESET_REQUESTING
PASSWORD_RESET_SENT
PASSWORD_RESETTING
PASSWORD_RESET_SUCCESS
PASSWORD_RESET_ERROR
PASSWORD_RESET_EXPIRED

AUTHENTICATED
PROFILE_LOADING
PROFILE_CREATING
PROFILE_READY
PROFILE_ERROR

NETWORK_ERROR
RATE_LIMITED
ACCOUNT_DISABLED

SESSION_EXPIRED
```

---

# 38. State Transition Architecture

```text
UNKNOWN
   ↓
RESTORING_SESSION
   │
   ├───────────────┐
   │               │
   ▼               ▼
AUTHENTICATED   UNAUTHENTICATED
   │               │
   ▼               ▼
PROFILE_LOADING   AUTH UI
   │
   ├── ERROR → PROFILE_ERROR
   │
   ▼
PROFILE_READY
   │
   ▼
APP
```

Sign-up:

```text
SIGN_UP
  ↓
SIGNING_UP
  ↓
AUTHENTICATED
  ↓
EMAIL_VERIFICATION_REQUIRED?
  │
  ├── YES → VERIFY
  │
  └── NO
        ↓
PROFILE_CREATING
        ↓
PROFILE_READY
```

---

# 39. Error Presentation Rules

Use three levels.

## Level 1: Inline

For field-specific errors.

Example:

```text
Password
[________________]

Passwords don't match.
```

## Level 2: Form Message

For authentication failure.

Example:

```text
The details you entered couldn't
be verified. Please try again.
```

## Level 3: Full State

For system-level failures.

Example:

```text
You're offline
```

Do not use full-screen error screens for simple form mistakes.

---

# 40. Toast/Snackbar Usage

Use transient messages sparingly.

Good uses:

* Verification email sent
* Password reset email sent
* Link copied
* Network restored

Avoid using a snackbar for critical authentication errors that the user must understand.

---

# 41. Accessibility States

Every state must remain accessible.

Examples:

### Loading

Screen reader:

> Signing in. Please wait.

### Error

Screen reader:

> Sign-in failed. Please check your details and try again.

### Success

Screen reader:

> Password updated successfully.

Do not communicate important states through color alone.

---

# 42. Animation Rules

Additional states should animate consistently with core authentication.

### Success

Subtle scale/fade.

### Error

Short shake or border transition.

Do not excessively shake fields.

### Loading

Smooth indeterminate progress.

### Screen transitions

200–350 ms.

---

# 43. Offline Strategy

Authentication remains network-dependent.

The app should distinguish:

```text
No Internet
```

from:

```text
Invalid Credentials
```

and:

```text
Authentication Server Error
```

This distinction dramatically improves user trust.

---

# 44. Production Acceptance Matrix

| State                | User Action | Expected Result           |
| -------------------- | ----------- | ------------------------- |
| Session restoring    | Wait        | Resolve session           |
| Sign in loading      | Wait        | Authenticate              |
| Invalid credentials  | Retry       | Remain on Sign In         |
| OAuth cancelled      | Return      | Remain on Auth            |
| OAuth failure        | Retry       | Reopen provider flow      |
| Sign-up success      | Continue    | Verification/profile      |
| Verification pending | Resend      | New verification email    |
| Verification success | Continue    | Profile/App               |
| Reset requested      | Open email  | Reset flow                |
| Reset success        | Sign in     | Authentication            |
| Reset expired        | Request new | New reset flow            |
| Offline              | Retry       | Re-attempt request        |
| Rate limited         | Wait        | Retry later               |
| Profile failure      | Retry       | Re-sync                   |
| Session expired      | Sign in     | New authenticated session |
| Logout               | Confirm     | Unauthenticated state     |

---

# 45. Final Authentication State Architecture

The finished Findlyts authentication system should therefore look like:

```text
                       FINDLYTS
                          │
                    ONBOARDING
                          │
                          ▼
                  ┌───────────────┐
                  │ AUTHENTICATION│
                  └───────┬───────┘
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
         SIGN IN                    SIGN UP
             │                         │
      ┌──────┼──────┐          ┌───────┼───────┐
      ▼      ▼      ▼          ▼       ▼       ▼
   Forgot  OAuth  Errors     Verify  OAuth   Errors
   Password        │         Email
      │            │            │
      ▼            ▼            ▼
   Reset       Recovery     Verification
   Password                   Success
      │                         │
      └────────────┬────────────┘
                   ▼
            AUTHENTICATED
                   │
                   ▼
             PROFILE SYNC
                   │
          ┌────────┴────────┐
          ▼                 ▼
     Profile Error      Profile Ready
          │                 │
          ▼                 ▼
        Retry              APP
```

The important architectural boundary is:

**Authentication proves who the user is. Profile integration establishes who that user is inside Findlyts.**

That separation will make the later **Profile, Friends/Circles, Chats, Discover, Feed, Map, and personalized recommendation systems** much easier to build without turning authentication into a giant tangled ball of string.
