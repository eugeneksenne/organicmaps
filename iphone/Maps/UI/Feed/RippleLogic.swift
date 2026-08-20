import Foundation

/// Port of `shared/feed/.../RippleLogic.kt`. Display and queue rules only.
/// Momentum, velocity, decay, and trust stay on the server.
enum RippleLogic {
  static let meaning = "More people should experience this."
  static let bands = ["quiet", "active", "heating", "hot", "viral"]
  static let publicFields: Set<String> = [
    "kind", "id", "subject_kind", "subject_id", "band",
    "ripple_count", "tonight_count", "ripple_hour",
    "like_count", "save_count", "comment_count",
    "updated_at", "moment_id", "venue_id", "event_id", "city_id", "creator_id"
  ]
  private static let forbidden: Set<String> = [
    "latitude", "longitude", "lat", "lng", "live_location", "location",
    "geofence", "accuracy", "heading",
    "whos_here", "who's here", "who_is_here", "whoshere", "presence",
    "invitation", "invitation_state", "invitation_until", "available_until",
    "who_is_present", "venue_presence"
  ]

  static func normalizeBand(_ band: String?) -> String {
    let value = band?.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    return bands.contains(value) ? value : "quiet"
  }

  static func bandLabel(_ band: String?) -> String {
    switch normalizeBand(band) {
    case "viral": return "🔴 Viral"
    case "hot": return "🟠 Hot"
    case "heating": return "🟡 Heating"
    case "active": return "🟢 Active"
    default: return ""
    }
  }

  static func isForbiddenField(_ name: String?) -> Bool {
    guard let name, !name.isEmpty else { return false }
    return forbidden.contains(name.lowercased().trimmingCharacters(in: .whitespacesAndNewlines))
  }

  static func publicFields(of raw: [String: String?]) -> [String: String] {
    var out: [String: String] = [:]
    for (key, value) in raw {
      let name = key.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
      if isForbiddenField(name) || !publicFields.contains(name) { continue }
      guard let value, !value.isEmpty, value != "null" else { continue }
      out[name] = value
    }
    return out
  }

  static func mergeQueue(_ pending: [PendingRipple], momentId: String, wantRippled: Bool, nowMs: Int64) -> [PendingRipple] {
    if momentId.isEmpty { return pending }
    return pending.filter { $0.momentId != momentId } + [PendingRipple(momentId: momentId, wantRippled: wantRippled, queuedAt: nowMs)]
  }
}

struct PendingRipple {
  var momentId: String
  var wantRippled: Bool
  var queuedAt: Int64 = 0
}

struct RippleSnapshot {
  var subjectKind: String = "moment"
  var subjectId: String = ""
  var band: String = "quiet"
  var rippleCount: Int = 0
  var tonightCount: Int = 0
}
