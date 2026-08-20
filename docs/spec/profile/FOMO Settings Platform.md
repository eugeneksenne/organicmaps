# FOMO Settings Platform

## Full Updated Production-Ready Settings & Control Center Specification

**Document:** `FOMO Settings Platform.md`
**System:** FOMO / Findlyts
**Purpose:** Personal command center for identity, security, privacy, nightlife discovery, creator operations, AI personalization, media, maps, safety, and application behavior.

This specification expands the uploaded FOMO Control Center into a complete **Settings Platform**, while preserving the sections and capabilities already defined in the source. The original specification establishes the Control Center as offline-first, realtime synchronized, role-aware, AI-assisted, and secure by default, with Supabase PostgreSQL/Realtimes and Telegram Media Engine integration. 

---

# 1. Product Definition

FOMO Settings is **not simply a list of toggles**.

It is the user's personal control center.

```text
                         FOMO APP
                            │
                            ▼
                    FOMO CONTROL CENTER
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
     Identity            Security            Privacy
        │                   │                   │
        ▼                   ▼                   ▼
   Experience           NightGuard          Discovery
        │                   │                   │
        ▼                   ▼                   ▼
      Creator             Media                AI
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                     SETTINGS MANAGER
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
           Local         Supabase       Device
           Store         Sync           APIs
              │             │             │
              └─────────────┼─────────────┘
                            ▼
                       SYNC ENGINE
                            │
                            ▼
                     REALTIME CONFIRMATION
```

---

# 2. Core Design Principles

The entire Settings Platform must be:

* **Offline-first**
* **Realtime synchronized**
* **Role-aware**
* **AI-assisted**
* **Secure by default**
* **Fully personalized**
* **Responsive**
* **Accessible**
* **Production-ready**

Every setting should update the interface immediately.

When the setting is cloud-backed, synchronization should happen automatically when connectivity is available. This preserves the uploaded specification's local-update → queue → Supabase → Realtime architecture. 

---

# 3. Settings Platform Architecture

```text
PROFILE SCREEN
      │
      ▼
CONTROL CENTER
      │
      ▼
SETTINGS MANAGER
      │
 ┌────┼───────────────┐
 ▼    ▼               ▼
Auth  Profile       Media
Manager Manager     Manager
 │      │             │
 └──────┼─────────────┘
        ▼
 SETTINGS REPOSITORY
        │
 ┌──────┼──────────────┐
 ▼      ▼              ▼
Local  Sync Queue    Device APIs
 DB       │
          ▼
      Supabase
          │
          ▼
       Realtime
```

---

# 4. Setting Ownership Model

Every setting must have an explicit owner.

```text
Authentication Manager
    ↓
Security / sessions / authentication

Profile Manager
    ↓
Identity / profile / account information

Settings Manager
    ↓
Application preferences

Media Manager
    ↓
Uploads / downloads / cache

Map Manager
    ↓
Map and location preferences

Notification Manager
    ↓
Notification preferences

NightGuard Manager
    ↓
Safety preferences

AI Personalization Manager
    ↓
AI preferences

Creator Manager
    ↓
Creator settings
```

Do not allow random screens to directly modify backend settings.

---

# 5. Setting Storage Scope

Every setting must specify where it belongs.

## Local Settings

Examples:

* Theme
* Autoplay
* Animation preferences
* Cache limits
* Download preferences
* Offline maps
* Camera preferences

## Cloud Settings

Examples:

* Profile visibility
* Online status
* Discoverability
* Notification preferences
* AI preferences
* Privacy settings

## Device Settings

Examples:

* Location permission
* Camera permission
* Microphone permission
* Notification permission
* Biometric availability

---

# 6. Universal Setting Model

Every setting should conceptually contain:

```text
Setting
├── id
├── category
├── type
├── value
├── defaultValue
├── scope
├── sensitivity
├── requiresConfirmation
├── requiresAuthentication
├── offlineSupported
├── syncStrategy
├── lastModified
├── syncStatus
└── source
```

Example:

```text
online_status

category:
privacy

type:
boolean

scope:
cloud

sensitive:
true

offlineSupported:
true

syncStrategy:
realtime
```

---

# 7. Control Center Home

## Header

```text
← Settings
```

Optional developer/debug access remains hidden behind the existing gesture.

---

## Profile Card

The uploaded specification calls for:

* Online Status
* Verification Badge
* Creator Badge
* Membership Level
* Current City
* Last Active
* Profile Completion %
* Edit Profile
* Share Profile
* QR Code
* Copy Profile Link

Avatar remains loaded through the Telegram Media Engine and profile updates use Supabase Realtime. 

### UI

```text
┌─────────────────────────────────────┐
│                                     │
│       [ AVATAR ]                    │
│                                     │
│       Koketso                       │
│       @username                     │
│                                     │
│       ● Online                      │
│       ✓ Verified                    │
│                                     │
│       Profile 85% complete          │
│       ━━━━━━━━━━━━━━━━━━━           │
│                                     │
│ [ Edit Profile ]   [ QR ]          │
│                                     │
│ Share Profile    Copy Link          │
└─────────────────────────────────────┘
```

---

# 8. Settings Search

Search should sit near the top of the Control Center.

```text
┌─────────────────────────────────────┐
│ 🔍 Search settings                  │
└─────────────────────────────────────┘
```

Support natural-language searches such as:

```text
Turn on dark mode
Change upload quality
Disable autoplay
Find NightGuard
Manage connected accounts
Clear Telegram cache
```

These examples are already part of the uploaded specification. 

---

# 9. Smart Recommendations

Display contextual recommendations beneath search.

Examples from the existing specification:

```text
Complete your profile
85% complete

Connect Google for easier sign-in

Clear 2.1 GB cached media

Retry 3 failed uploads

Download offline maps

Enable NightGuard

Verify your email
```

Recommendations must support:

```text
Shown
↓
Opened
↓
Completed
```

or:

```text
Shown
↓
Dismissed
↓
Permanently dismissed
```

The existing specification explicitly requires dismissed recommendations to remain dismissed. 

---

# 10. Quick Actions

Quick Actions should provide direct access to frequently used controls.

Existing:

* NightGuard
* Saved Places
* Downloads
* Blocked Users
* Privacy Checkup
* Connected Accounts

Add:

* QR Code
* My Plans
* Offline Maps
* AI Assistant
* Media Upload Queue

These additions preserve the uploaded specification. 

---

# 11. Account

```text
ACCOUNT
│
├── Account Overview
├── Personal Information
├── Authentication & Security
├── Connected Accounts
├── Active Sessions
└── Account Management
```

---

# 12. Account Overview

Display:

* Email Verified
* Phone Verified
* Account Created
* Last Login
* Current Device
* Subscription, when supported

The uploaded specification already defines these account overview fields. 

---

# 13. Authentication & Security

```text
AUTHENTICATION & SECURITY

Password
Passkeys
Biometric Login
Two-Factor Authentication
Active Sessions
Session History
Trusted Devices
Connected Accounts
Login Activity
Security Alerts
```

Authentication-related settings must be managed by the **Authentication Manager**.

They should not be independently implemented inside Settings.

---

# 14. Active Sessions

Display:

```text
Active Sessions

● This device
  Windows / Android / iOS
  Current session

● Galaxy device
  Active 12 min ago

● iPhone
  Active yesterday
```

Actions:

```text
Sign out
Sign out all other devices
```

For security-sensitive actions:

```text
Confirm identity
        ↓
Perform action
```

---

# 15. Connected Accounts

Providers from the uploaded specification:

* Google
* Apple
* Facebook
* TikTok
* Instagram

Additional:

* Spotify
* X
* Phone
* Email

Each provider:

```text
Google
Connected ✓

Last used:
Yesterday

[ Manage ]
```

Possible states:

```text
Connected
Disconnected
Connecting
Disconnecting
Error
```

Do not expose provider credentials.

---

# 16. Privacy & Safety

```text
PRIVACY & SAFETY
│
├── Privacy Checkup
├── Profile Visibility
├── Activity Visibility
├── Online Status
├── Read Receipts
├── Typing Indicators
├── Location Privacy
├── AI Data Sharing
├── Discoverability
├── Search Visibility
├── Blocked Accounts
├── Muted Accounts
└── Restricted Accounts
```

The uploaded specification explicitly calls for these privacy controls. 

---

# 17. Privacy Checkup

Provide a guided overview.

```text
Privacy Checkup

Profile
Public

Activity
Friends

Online Status
Visible

Location
Approximate

AI Personalization
Enabled

Search Visibility
Enabled

[ Review Privacy ]
```

The user should be able to change each category directly.

---

# 18. Location Privacy

Separate:

```text
Precise Location
Approximate Location
Location Sharing
Background Location
Venue Visibility
Friend Location Visibility
```

Location settings must distinguish **app permission** from **FOMO preference**.

For example:

```text
Device permission:
Location allowed

FOMO preference:
Share approximate location
```

---

# 19. NightGuard

NightGuard should be treated as a dedicated safety subsystem.

```text
NIGHTGUARD
│
├── Status
├── Trusted Contacts
├── Trusted Locations
├── Safe Route Preference
├── Automatic SOS Countdown
├── Emergency Information
├── Emergency QR Card
├── Background Safety Monitoring
└── Battery Emergency Mode
```

All of these are derived from the uploaded NightGuard specification. 

---

# 20. NightGuard Status Card

```text
┌─────────────────────────────────────┐
│ NIGHTGUARD                          │
│                                     │
│ ● Ready                             │
│                                     │
│ Trusted contacts       3            │
│ Emergency info         Complete     │
│ Trusted locations      2            │
│                                     │
│ [ Manage NightGuard ]               │
└─────────────────────────────────────┘
```

---

# 21. Notifications

```text
NOTIFICATIONS
│
├── Notification Permissions
├── Notification Schedule
├── Quiet Hours
├── Priority Notifications
├── Smart Summaries
├── Live Activity Alerts
├── Nearby Friends
├── Trending Venues
├── AI Recommendations
├── Notification Preview
└── Notification Sound Packs
```

These additions are already specified in the uploaded document. 

---

# 22. Location & Maps

```text
LOCATION & MAPS
│
├── Location Permission
├── Live Crowd Layer
├── Friends Layer
├── Venue Density Layer
├── Traffic Layer
├── Weather Layer
├── Offline Routing
├── Offline Maps
├── Map Download Priority
├── Preferred Navigation App
└── GPS Diagnostics
```

Display diagnostics:

```text
Current Location
Latitude / Longitude

GPS Accuracy
±8 m

Location Source
GPS

Map Status
Offline-ready ✓
```

The uploaded specification includes these map layers and GPS diagnostics. 

---

# 23. Appearance

```text
APPEARANCE
│
├── Theme
├── Material You
├── Dynamic Colors
├── Adaptive Icons
├── App Icon Packs
├── Custom Accent Gradient
├── Font Scaling
├── Theme Scheduler
└── OLED Mode
```

Preview theme changes immediately.

---

# 24. Experience

```text
EXPERIENCE
│
├── Feed
├── Camera
├── Discover
├── Chats
└── AI Personalization
```

---

# 25. Feed Settings

```text
Feed

Video Autoplay
ON

Feed Quality
Auto

Motion Effects
ON

Data Saver
OFF

Preload Media
ON
```

---

# 26. Camera Settings

The uploaded specification calls for:

* AI Enhancement
* Night Mode
* Lens Correction
* HDR
* RAW Capture, future



Recommended structure:

```text
Camera
│
├── AI Enhancement
├── Night Mode
├── Lens Correction
├── HDR
├── Video Quality
├── Photo Quality
├── Upload Quality
└── RAW Capture
```

Future capabilities should be clearly marked rather than presented as working features before implementation.

---

# 27. Discover Settings

```text
Discover

Venue Ranking
Recommendation Engine
Personal Interests
Age Preferences
Search Radius
```

These control discovery behavior rather than directly changing venue data.

---

# 28. Chat Settings

```text
Chats

Message Requests
Message Translation
Voice Notes
Media Compression
Typing Indicators
Read Receipts
```

These should integrate with the Chats system rather than becoming isolated Settings-only implementations.

---

# 29. AI Personalization

Dedicated subsection:

```text
AI PERSONALIZATION

Personalized Recommendations
AI Camera Suggestions
AI Captions
AI Event Discovery
AI Smart Notifications
AI Search
AI Profile Suggestions
AI Memory
Clear AI History
AI Data Controls
Opt Out
```

These are explicitly supported by the uploaded specification. 

---

# 30. AI Permission Boundary

Hard rule:

```text
AI recommendation
       ↓
User review
       ↓
Explicit confirmation
       ↓
Setting changes
```

Never:

```text
AI observes behavior
       ↓
AI silently changes settings
```

The uploaded specification explicitly requires that AI never change user preferences without explicit confirmation. 

---

# 31. Storage & Data

```text
STORAGE & DATA
│
├── Storage Overview
├── Telegram Media
├── Cache
├── Downloads
└── Data Usage
```

---

# 32. Storage Overview

Example:

```text
Storage

6.8 GB used

Telegram Media
2.8 GB

Cache
2.1 GB

Offline Maps
1.2 GB

Downloads
700 MB

[ Manage Storage ]
```

---

# 33. Telegram Media

The existing specification identifies Telegram as the media backend. 

```text
TELEGRAM MEDIA

Storage Backend
Telegram

Upload Queue
340 MB

Pending Uploads
12

Failed Uploads
3

Downloaded Media
1.7 GB

Cached Thumbnails
420 MB

[ Retry Failed ]
[ Clear Upload Queue ]
```

---

# 34. Upload Queue

State model:

```text
Queued
 ↓
Uploading
 ↓
Uploaded
```

Failure:

```text
Uploading
 ↓
Failed
 ↓
Retry
```

Offline:

```text
Queued
 ↓
Waiting for connection
 ↓
Resume automatically
```

---

# 35. Cache

```text
CACHE

Images
Videos
Maps
Stories
Feed
Chats
AI Cache

[ Clear Selected ]
[ Clear All Cache ]
```

Before clearing significant cached data:

```text
Clear 2.1 GB cache?

Your original cloud media won't be deleted.

[ Cancel ]
[ Clear Cache ]
```

---

# 36. Data Usage

```text
DATA USAGE

Upload Quality
Auto

Download Quality
Auto

Streaming Quality
High

Cellular Restrictions
Enabled

Roaming Restrictions
Enabled
```

---

# 37. Accessibility

```text
ACCESSIBILITY

TalkBack
VoiceOver
Keyboard Navigation
Touch Target Scaling
Screen Reader Labels
Captions
Audio Descriptions
Reduced Flashing
High Visibility Focus
```

These accessibility additions are explicitly included in the source specification. 

---

# 38. Creator Studio

Role-aware section:

```text
CREATOR STUDIO

Creator Dashboard
Analytics
Performance Insights
Media Library
Promotion Manager
Business Settings
Verification Status
Moderation Alerts
Revenue Dashboard
```

Revenue Dashboard should remain marked **future** until implemented.

The section should not appear as a creator control center for ordinary users unless the user's role supports it.

---

# 39. Support

```text
SUPPORT

Help Center
Live Chat
Diagnostic Report
Upload Logs
System Health
Connection Test
Telegram Upload Test
Supabase Status
Report a Problem
```

The uploaded specification already includes the diagnostic and service-health components. 

---

# 40. Diagnostic Report

Provide a user-safe diagnostic bundle.

```text
Diagnostic Report

App version
Build
Device
OS
Network
Realtime status
Media backend
Map status

[ Generate Report ]
```

Never include:

* passwords
* access tokens
* refresh tokens
* Telegram credentials
* private authentication secrets

---

# 41. Legal

```text
LEGAL

Terms of Service
Privacy Policy
Community Guidelines
Creator Terms
Community Moderation Policy
AI Usage Policy
Open Source Licenses
Third-Party Services
```

---

# 42. About

```text
ABOUT

FOMO

App Build
API Version
Database Schema Version
Realtime Connection Status
Media Backend
Current Region
Device Model
Operating System
```

---

# 43. Developer

Hidden behind the existing developer gesture.

```text
DEVELOPER
│
├── Authentication State Viewer
├── Current Session
├── Current User
├── Profile Cache
├── Settings Cache
├── Realtime Monitor
├── Media Upload Queue
├── Telegram Diagnostics
├── Supabase Diagnostics
├── SQL Sync Queue
├── Offline Queue
├── Edge Function Logs
├── Performance Graph
├── Memory Usage
└── Battery Usage
```

The source explicitly requires Developer Mode to remain hidden behind the existing tap gesture. 

Production builds must additionally enforce appropriate developer access controls.

---

# 44. Settings Synchronization Engine

Every cloud-backed setting follows:

```text
USER CHANGES SETTING
        ↓
UPDATE LOCAL DATABASE
        ↓
UPDATE UI IMMEDIATELY
        ↓
CREATE SYNC OPERATION
        ↓
SYNC QUEUE
        ↓
SUPABASE
        ↓
REALTIME CONFIRMATION
        ↓
MARK SYNCHRONIZED
```

This directly extends the offline/synchronization architecture already specified. 

---

# 45. Sync States

Every synchronizable setting can have:

```text
SYNCED
PENDING
SYNCING
FAILED
CONFLICT
OFFLINE
```

UI examples:

```text
✓ Saved
```

```text
↻ Syncing...
```

```text
Offline
Will sync when you're back online.
```

```text
! Couldn't save
Retry
```

---

# 46. Conflict Resolution

If two devices change the same setting:

```text
DEVICE A
    │
    ├── Change A
    │
    ▼
 SERVER
    ▲
    │
    ├── Change B
    │
DEVICE B
```

The Settings Manager must resolve conflicts deterministically.

For simple preferences:

```text
Latest valid server revision wins
```

Do not silently overwrite a newer server state with stale local data.

---

# 47. Settings Persistence

App restart:

```text
Local Settings
 ↓
Restore
 ↓
UI
```

Online:

```text
Local
 ↓
Reconcile
 ↓
Server
```

Offline:

```text
Local
 ↓
UI
 ↓
Queue
```

Network restored:

```text
Queue
 ↓
Sync
 ↓
Confirm
 ↓
Remove queue item
```

---

# 48. Security Architecture

All settings must:

* use encrypted local storage where appropriate
* synchronize through authenticated Supabase sessions
* respect Row Level Security
* never expose authentication tokens
* never expose Telegram credentials
* audit sensitive account changes

These requirements come directly from the existing specification. 

Additional rule:

**Settings UI is never an authorization boundary.**

The backend must independently enforce permissions.

---

# 49. Sensitive Settings

Require additional confirmation/authentication for actions such as:

```text
Change password
Disable security features
Remove trusted device
Sign out all devices
Disconnect critical account
Delete account
Change sensitive privacy controls
Modify emergency information
```

The exact confirmation mechanism should use the application's existing Authentication Manager.

---

# 50. Settings Audit Log

Sensitive changes should generate audit events:

```text
SECURITY ACTIVITY

Password changed
Today

Google account connected
Today

Online status changed
Yesterday

New device signed in
Yesterday
```

This supports the uploaded requirement to audit sensitive account changes. 

---

# 51. Global Settings Search Architecture

Search index:

```text
Setting ID
Title
Description
Category
Keywords
Synonyms
Current Value
Navigation Target
```

Example:

```text
"autoplay"

→ Feed
→ Video Autoplay
```

```text
"nightguard"

→ NightGuard
→ Trusted Contacts
→ SOS
→ Emergency QR
```

```text
"google"

→ Connected Accounts
→ Google
```

---

# 52. Smart Settings Assistant

The AI/settings assistant can interpret:

```text
"Make FOMO quieter at night"

```

It should respond:

```text
I can adjust:

Quiet Hours
Priority Notifications
Live Activity Alerts

Review changes
[ Apply ]
[ Cancel ]
```

Never apply changes without confirmation.

---

# 53. Settings Navigation Rules

No dead ends.

Example:

```text
Settings
 ↓
Privacy
 ↓
Online Status
 ↓
Toggle
 ↓
Back
 ↓
Privacy
```

Search:

```text
Search
 ↓
Result
 ↓
Setting
 ↓
Change
 ↓
Back to results
```

Deep links must remain valid.

---

# 54. Loading States

Every cloud-backed settings page needs:

```text
Loading
Loaded
Saving
Saved
Error
Offline
```

Avoid blank screens.

---

# 55. Error States

## Generic

```text
Something went wrong.

Try again.
```

## Network

```text
You're offline.

Changes will sync when you're back online.
```

## Server

```text
FOMO couldn't save this setting right now.

Try again.
```

## Permission

```text
You don't have permission to change this setting.
```

---

# 56. Optimistic UI

For ordinary preferences:

```text
Tap toggle
 ↓
UI changes immediately
 ↓
Persist locally
 ↓
Sync server
```

If synchronization fails:

```text
UI
 ↓
Rollback or mark pending
```

The strategy must depend on the setting type.

Never leave the UI permanently showing a value the server rejected.

---

# 57. Device Permission Boundary

Settings must distinguish:

```text
FOMO preference
```

from:

```text
OS permission
```

Example:

```text
Location

FOMO:
Share approximate location

Device:
Location permission
Allowed
```

Tapping Device permission should open the operating system's permission/settings flow.

---

# 58. Role-Based Settings

Settings visibility should depend on role.

```text
USER
 ↓
Standard settings
```

```text
CREATOR
 ↓
Standard
+
Creator Studio
```

```text
BUSINESS
 ↓
Standard
+
Business Settings
```

Do not merely hide functionality visually. Backend authorization must also enforce role access.

---

# 59. Final Control Center

```text
FOMO CONTROL CENTER
│
├── Profile
│
├── Search
│
├── Smart Recommendations
│
├── Quick Actions
│
├── Account
│   ├── Overview
│   ├── Security
│   ├── Sessions
│   └── Connected Accounts
│
├── Privacy & Safety
│   ├── Privacy Checkup
│   ├── Profile Privacy
│   ├── Activity Privacy
│   ├── Location Privacy
│   ├── AI Privacy
│   └── Blocked / Muted / Restricted
│
├── NightGuard
│
├── Notifications
│
├── Location & Maps
│
├── Appearance
│
├── Experience
│   ├── Feed
│   ├── Camera
│   ├── Discover
│   ├── Chats
│   └── AI Personalization
│
├── Storage & Data
│   ├── Telegram Media
│   ├── Cache
│   ├── Downloads
│   └── Data Usage
│
├── Accessibility
│
├── Creator Studio
│
├── Support
│
├── Legal
│
├── About
│
└── Developer
```

---

# 60. Production Implementation Loop

The implementation agent must process every settings feature using:

```text
DISCOVER
   ↓
UNDERSTAND
   ↓
IMPLEMENT
   ↓
CONNECT
   ↓
TEST
   ↓
VERIFY
   ↓
HARDEN
   ↓
RECHECK
   ↓
DOCUMENT
```

### DISCOVER

Inspect existing implementation before changing anything.

### UNDERSTAND

Identify:

* current architecture
* data source
* ownership
* dependencies
* existing components

### IMPLEMENT

Build the missing functionality.

### CONNECT

Connect:

```text
UI
 ↓
State
 ↓
Repository
 ↓
Sync
 ↓
Backend
```

### TEST

Test:

* happy path
* offline
* failure
* retry
* restart
* process death
* multiple devices
* permission denial

### VERIFY

Check visual fidelity and functionality.

### HARDEN

Fix:

* race conditions
* duplicate requests
* stale state
* sync conflicts
* navigation problems
* security problems

### RECHECK

Ensure existing functionality still works.

---

# 61. Definition of Done

The FOMO Settings Platform is **not complete** because every menu item exists.

It is complete when:

```text
EVERY SETTING
      │
      ├── Has an owner
      ├── Has a source of truth
      ├── Has a persistence strategy
      ├── Has loading state
      ├── Has error state
      ├── Has offline behavior
      ├── Has synchronization behavior
      ├── Has security rules
      ├── Has accessibility
      ├── Has navigation
      └── Has been tested
```

And:

```text
AUTHENTICATION
      ↓
PROFILE
      ↓
SETTINGS
      ↓
LOCAL STORE
      ↓
SYNC ENGINE
      ↓
SUPABASE
      ↓
REALTIME
```

all operate as a coherent platform.

---

# 62. Final Production Checklist

### Control Center

* [ ] Settings home
* [ ] Profile card
* [ ] Quick actions
* [ ] Search
* [ ] Smart recommendations
* [ ] Sync status

### Account

* [ ] Account overview
* [ ] Authentication
* [ ] Security
* [ ] Sessions
* [ ] Connected accounts

### Privacy

* [ ] Profile visibility
* [ ] Activity visibility
* [ ] Online status
* [ ] Discoverability
* [ ] Search visibility
* [ ] Location privacy
* [ ] AI privacy
* [ ] Blocked
* [ ] Muted
* [ ] Restricted

### NightGuard

* [ ] Status
* [ ] Trusted contacts
* [ ] Trusted locations
* [ ] Safe route
* [ ] SOS
* [ ] Emergency information
* [ ] Emergency QR
* [ ] Background safety
* [ ] Battery emergency mode

### Notifications

* [ ] Permissions
* [ ] Quiet hours
* [ ] Priority
* [ ] Smart summaries
* [ ] Live activity
* [ ] Nearby friends
* [ ] Trending venues
* [ ] AI recommendations

### Maps

* [ ] Location
* [ ] Crowd layer
* [ ] Friends layer
* [ ] Venue density
* [ ] Traffic
* [ ] Weather
* [ ] Offline routing
* [ ] Offline maps
* [ ] Navigation app
* [ ] GPS diagnostics

### Experience

* [ ] Feed
* [ ] Camera
* [ ] Discover
* [ ] Chats
* [ ] AI personalization

### Storage

* [ ] Telegram media
* [ ] Upload queue
* [ ] Failed uploads
* [ ] Downloads
* [ ] Cache
* [ ] Data usage

### Accessibility

* [ ] TalkBack
* [ ] VoiceOver
* [ ] Keyboard navigation
* [ ] Touch scaling
* [ ] Captions
* [ ] Audio descriptions
* [ ] Reduced flashing
* [ ] Focus visibility

### Creator

* [ ] Creator dashboard
* [ ] Analytics
* [ ] Media library
* [ ] Promotion
* [ ] Business settings
* [ ] Verification
* [ ] Moderation

### Support

* [ ] Help
* [ ] Live chat
* [ ] Diagnostics
* [ ] Connection test
* [ ] Telegram test
* [ ] Supabase status

### Developer

* [ ] Auth state
* [ ] Session
* [ ] User
* [ ] Profile cache
* [ ] Settings cache
* [ ] Realtime
* [ ] Media queue
* [ ] Telegram diagnostics
* [ ] Supabase diagnostics
* [ ] Offline queue
* [ ] Edge functions
* [ ] Performance
* [ ] Memory
* [ ] Battery

### Platform

* [ ] Offline-first
* [ ] Optimistic updates
* [ ] Sync queue
* [ ] Conflict handling
* [ ] Realtime confirmation
* [ ] Secure local persistence
* [ ] RLS
* [ ] Audit logging
* [ ] Role-aware access
* [ ] Accessibility
* [ ] Error recovery
* [ ] Process-death recovery
* [ ] Multi-device consistency

---

## The resulting architecture

The key upgrade is that **FOMO Settings is now a platform layer**, not a screen:

```text
                     FOMO APP
                        │
                        ▼
                CONTROL CENTER
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
       PROFILE       SECURITY       PRIVACY
          │             │             │
          ▼             ▼             ▼
      EXPERIENCE    NIGHTGUARD       AI
          │             │             │
          ▼             ▼             ▼
       CREATOR        MEDIA          MAPS
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                 SETTINGS MANAGER
                        │
             ┌──────────┼──────────┐
             ▼          ▼          ▼
           LOCAL      SYNC       DEVICE
            DB        QUEUE       APIs
             │          │
             └────┬─────┘
                  ▼
              SUPABASE
                  │
                  ▼
              REALTIME
```

That structure preserves the capabilities in your uploaded FOMO settings specification while giving them a proper ownership, persistence, synchronization, security, and state-management architecture. 
