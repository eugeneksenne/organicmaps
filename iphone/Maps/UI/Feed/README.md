# FOMO Feed (iOS)

UIKit adapters for the shared Feed / Ripple contract.

- `RippleLogic.swift` — keep in lockstep with `shared/feed/src/commonMain/kotlin/app/organicmaps/feed/RippleLogic.kt`
- Display and offline-queue rules only. Momentum bands come from `ripple_snapshot` / `set_moment_reaction`.
- Never send or render Who's Here, live location, or Moment Invitation settings through Ripple.

These files are not yet in `project.pbxproj`.
