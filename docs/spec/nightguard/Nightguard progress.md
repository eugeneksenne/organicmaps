# Night Guard — Implementation Progress Tracker

This file is read and written by the agent every run, per `NIGHTGUARD_AGENT_LOOP.md`. Do not hand-edit statuses without leaving a note — the agent trusts this file as ground truth for what's already done.

Status values: `Not Started` · `In Progress` · `Blocked` · `Complete`

---

## Session Log

*(Newest run at top. Each run prepends its summary here before ending.)*

| Date | Completed this run | Assumptions logged | Open blockers | Recommended next |
|---|---|---|---|---|
| 2026-08-05 | Imported the canonical Night Guard specs and initialized the implementation loop. | Existing Android Java/XML and iOS native project are the current client baseline. | `nightguard-core` is blocked: the repository has no Kotlin Multiplatform/Compose/SwiftUI shared-client foundation, and this environment has no JDK (`java`/`JAVA_HOME`) to compile or validate a new KMP module. | Restore/provide JDK tooling and approve/create the KMP shared module integration path; then begin `nightguard-core`. |
| — | *(no runs yet)* | — | — | Start with Phase 0 |

---

## Phase 0 — Foundation

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| nightguard-core | Blocked | The canonical specification requires a KMP `commonMain` module, Compose Android UI, SwiftUI bridge, and verified tests; this native Java/XML checkout has no shared KMP module and no JDK is available in the execution environment. | See Blocker Detail 2026-08-05-A. | 2026-08-05 |
| security | Blocked | Depends on `nightguard-core` shared encryption/key-management boundary. | `nightguard-core` blocked. | 2026-08-05 |
| permissions | Blocked | Depends on core capability model and unavailable Android/iOS build verification. | `nightguard-core` blocked; JDK unavailable. | 2026-08-05 |
| background-services | Blocked | Depends on permissions and KMP expect/actual platform contracts. | `nightguard-core` and permissions blocked; JDK unavailable. | 2026-08-05 |
| nightguard-core | Not Started | | | |
| security | Not Started | | | |
| permissions | Not Started | | | |
| background-services | Not Started | | | |

## Phase 1 — Shared Intelligence Engines

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| context-engine | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| risk-engine | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| session-engine | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| presence-engine | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| route-intelligence | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| geofencing | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| battery-awareness | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| offline | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| sync | Blocked | Depends on verified Phase 0 core/security/runtime foundation. | `nightguard-core` blocked. | 2026-08-05 |
| context-engine | Not Started | | | |
| risk-engine | Not Started | | | |
| session-engine | Not Started | | | |
| presence-engine | Not Started | | | |
| route-intelligence | Not Started | | | |
| geofencing | Not Started | | | |
| battery-awareness | Not Started | | | |
| offline | Not Started | | | |
| sync | Not Started | | | |

## Phase 2 — Trust & Identity

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| trusted-circle | Blocked | Depends on verified Phase 0 and Phase 1 foundations. | Phase 0 blocked. | 2026-08-05 |
| privacy | Blocked | Depends on verified Phase 0 and Phase 1 foundations. | Phase 0 blocked. | 2026-08-05 |
| trusted-circle | Not Started | | | |
| privacy | Not Started | | | |

## Phase 3 — Feature Engines

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| safety-check | Blocked | Depends on verified Phase 1 intelligence and Phase 2 privacy/trust modules. | Phases 0–2 blocked. | 2026-08-05 |
| buddy-pair | Blocked | Depends on verified Phase 1 intelligence and Phase 2 privacy/trust modules. | Phases 0–2 blocked. | 2026-08-05 |
| walk-me-home | Blocked | Depends on verified Phase 1 intelligence and Phase 2 privacy/trust modules. | Phases 0–2 blocked. | 2026-08-05 |
| ride-companion | Blocked | Depends on verified Phase 1 intelligence and Phase 2 privacy/trust modules. | Phases 0–2 blocked. | 2026-08-05 |
| safety-check | Not Started | | | |
| buddy-pair | Not Started | | | |
| walk-me-home | Not Started | | | |
| ride-companion | Not Started | | | |

## Phase 4 — Emergency Platform

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| emergency | Not Started | | | |
| sos | Not Started | | | |
| emergency-chat | Not Started | | | |
| emergency-calls | Not Started | | | |
| evidence | Not Started | | | |
| recovery | Not Started | | | |

## Phase 5 — Surfaces & Delivery

| Module | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| nightguard-dashboard | Not Started | | | |
| notifications | Not Started | | | |
| analytics | Not Started | | | |

## Phase 6 — Store Readiness Pass

| Item | Status | Assumptions | Blockers | Last Updated |
|---|---|---|---|---|
| Android manifest + permissions audit | Not Started | | | |
| Android Play Data Safety form mapping | Not Started | | | |
| iOS Info.plist usage-string audit | Not Started | | | |
| iOS App Privacy nutrition label mapping | Not Started | | | |
| Store listing copy vs. actual data use | Not Started | | | |

---

## Blocker Detail Log

*(Full detail for anything marked `Blocked` above — the table cell should be a one-line pointer here.)*

*(none yet)*
