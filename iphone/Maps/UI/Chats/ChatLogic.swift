import Foundation

/// Port of `shared/chats/.../ChatLogic.kt`. Keep merge/filter/preview rules identical.
enum ChatLogic {
  static let reactions = ["👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉"]
  static let storyAudienceFriends = "friends"
  static let storyAudiencePrivate = "private"
  static let storyAudiences = [storyAudienceFriends, storyAudiencePrivate]
  static let filterAll = "all"
  static let filterUnread = "unread"
  static let filterPersonal = "personal"
  static let filterGroup = "group"
  static let filterVenue = "venue"

  static func relativeTime(epochMs: Int64, nowMs: Int64) -> String {
    if epochMs <= 0 { return "" }
    let delta = max(0, nowMs - epochMs)
    if delta < 15_000 { return "Now" }
    if delta < 60_000 { return "\(delta / 1000)s" }
    if delta < 3_600_000 { return "\(delta / 60_000)m" }
    if delta < 86_400_000 { return "\(delta / 3_600_000)h" }
    if delta < 7 * 86_400_000 { return "\(delta / 86_400_000)d" }
    return isoDate(epochMs)
  }

  static func presenceLabel(online: Bool, lastSeen: Int64, username: String?, nowMs: Int64, status: String? = nil) -> String {
    if status == "busy" { return "Busy" }
    if status == "in_call" { return "In a call" }
    if status == "recording" { return "Recording…" }
    if online || status == "online" { return "Online" }
    if lastSeen > 0 { return "Last seen \(relativeTime(epochMs: lastSeen, nowMs: nowMs))" }
    guard let username, !username.isEmpty else { return "" }
    return "@\(username)"
  }

  static func deliveryTick(state: String, mine: Bool) -> String {
    if !mine { return "" }
    switch state {
    case "queued", "sending": return "  ○"
    case "delivered", "read": return "  ✓✓"
    default: return "  ✓"
    }
  }

  static func typingLabel(_ count: Int) -> String {
    if count <= 0 { return "" }
    if count == 1 { return "Typing…" }
    return "\(count) people typing…"
  }

  static func previewText(_ message: ChatMessage) -> String {
    if message.deleted || message.body == nil { return "This message was deleted" }
    let first = message.body?.split(separator: "\n").first.map(String.init) ?? ""
    switch message.kind {
    case "venue": return "📍 \(first)"
    case "event": return "🎟️ \(first)"
    case "location": return "📍 Location"
    case "image": return "📷 Photo"
    case "video": return "🎬 Video"
    case "voice_note": return "🎤 Voice note"
    case "audio": return "🎵 Audio"
    case "document": return "📄 \(first)"
    case "moment": return "Shared a Moment"
    case "route": return "🗺️ Shared a route"
    default: return message.body ?? ""
    }
  }

  static func matchesCategory(_ conversation: ChatConversation, category: String) -> Bool {
    switch category {
    case filterUnread: return conversation.unread > 0
    case filterPersonal: return conversation.kind == "direct"
    case filterGroup: return conversation.kind == "group"
    case filterVenue: return ["venue", "event", "nightguard"].contains(conversation.kind)
    default: return true
    }
  }

  /// Stories are friends (mutual) or only-me. Followers/public are not allowed.
  static func normalizeStoryAudience(_ audience: String?) -> String {
    if audience?.lowercased() == storyAudiencePrivate { return storyAudiencePrivate }
    return storyAudienceFriends
  }

  static func matchesQuery(_ conversation: ChatConversation, query: String) -> Bool {
    if query.isEmpty { return true }
    let needle = query.lowercased()
    return conversation.title.lowercased().contains(needle)
      || (conversation.preview?.lowercased().contains(needle) ?? false)
      || (conversation.peerUsername?.lowercased().contains(needle) ?? false)
  }

  static func mergeQueued(server: [ChatMessage], local: [ChatMessage]) -> [ChatMessage] {
    let ids = Set(server.map(\.id))
    let operations = Set(server.compactMap(\.clientOperationId))
    let extras = local.filter { message in
      (message.state == "queued" || message.state == "sending")
        && !ids.contains(message.id)
        && (message.clientOperationId == nil || !operations.contains(message.clientOperationId!))
    }
    return (server + extras).sorted { $0.sentAt < $1.sentAt }
  }

  static func demoInbox(now: Int64) -> [ChatConversation] {
    [
      ChatConversation(id: "demo-nightguard", kind: "nightguard", title: "NightGuard",
                       preview: "Your safety check is ready", lastMessageAt: now, pinned: true, verified: true),
      ChatConversation(id: "demo-alfred", kind: "direct", title: "Alfred M.", peerId: "demo-alfred-user",
                       peerUsername: "alfredm", preview: "I’m at Cocoon — pull through 🔥",
                       lastMessageAt: now - 120_000, unread: 1, verified: true, peerOnline: true),
      ChatConversation(id: "demo-group", kind: "group", title: "Joburg Fridays",
                       preview: "Nomsa: Who’s joining after dinner?", lastMessageAt: now - 3_600_000)
    ]
  }

  static func demoMessages(conversationId: String, selfId: String?, now: Int64) -> [ChatMessage] {
    let me = selfId ?? "me"
    return [
      ChatMessage(id: "\(conversationId)-1", conversationId: conversationId, senderId: "peer",
                  senderName: "Alfred", kind: "text", body: "I’m at Cocoon — pull through 🔥",
                  sentAt: now - 180_000, mine: false),
      ChatMessage(id: "\(conversationId)-3", conversationId: conversationId, senderId: me,
                  senderName: "You", kind: "text", body: "I’ll be there soon",
                  sentAt: now - 60_000, mine: true, state: "read")
    ]
  }

  private static func isoDate(_ epochMs: Int64) -> String {
    let date = Date(timeIntervalSince1970: TimeInterval(epochMs) / 1000)
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd"
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    return formatter.string(from: date)
  }
}

struct ChatConversation {
  var id: String
  var kind: String
  var title: String
  var peerId: String? = nil
  var peerUsername: String? = nil
  var preview: String? = nil
  var lastMessageId: String? = nil
  var lastMessageKind: String = "text"
  var lastMessageAt: Int64 = 0
  var unread: Int = 0
  var pinned: Bool = false
  var verified: Bool = false
  var muted: Bool = false
  var peerOnline: Bool = false
  var peerLastSeen: Int64 = 0
}

struct ChatStory {
  var id: String
  var authorName: String
  var authorUsername: String
  var viewed: Bool = false
  var authorId: String = ""
  var caption: String = ""
  var audience: String = ChatLogic.storyAudienceFriends
  var mine: Bool = false
}

struct ChatContact {
  var id: String
  var name: String
  var phone: String? = nil
  var email: String? = nil
}

struct ChatProfile {
  var id: String
  var username: String
  var displayName: String
  var verified: Bool = false
}

struct ChatMessage {
  var id: String
  var conversationId: String
  var senderId: String
  var senderName: String
  var kind: String
  var body: String?
  var clientOperationId: String? = nil
  var replyToId: String? = nil
  var replyPreview: String? = nil
  var sentAt: Int64
  var editedAt: Int64 = 0
  var mine: Bool
  var deleted: Bool = false
  var state: String = "sent"
  var reactions: String = ""
  var ownReaction: String? = nil
}
