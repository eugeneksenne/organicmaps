import Foundation

/// iOS adapter for the shared chat RPC contract in `shared/chats/README.md`.
final class ChatClient {
  static let shared = ChatClient()

  var supabaseURL = ""
  var anonKey = ""
  var accessToken: String?
  var userId: String?

  private let session = URLSession(configuration: .default)

  var isConfigured: Bool { !supabaseURL.isEmpty && !anonKey.isEmpty }
  var isSignedIn: Bool { accessToken?.isEmpty == false }

  func loadInbox(completion: @escaping ([ChatConversation], [ChatStory], String) -> Void) {
    if !isConfigured {
      completion(ChatLogic.demoInbox(now: nowMs()), [], "Demo mode")
      return
    }
    rpc("chat_inbox", body: [:]) { [weak self] result in
      switch result {
      case .success(let json):
        let conversations = Self.parseInbox(json)
        self?.rpc("inbox_stories", body: [:]) { storiesResult in
          let stories = (try? storiesResult.get()).map(Self.parseStories) ?? []
          completion(conversations, stories, "")
        }
      case .failure:
        completion(ChatLogic.demoInbox(now: nowMs()), [], "Couldn't refresh chats. Showing saved conversations.")
      }
    }
  }

  func loadMessages(conversationId: String, selfId: String?, completion: @escaping ([ChatMessage]) -> Void) {
    if !isConfigured || conversationId.hasPrefix("demo-") {
      completion(ChatLogic.demoMessages(conversationId: conversationId, selfId: selfId, now: nowMs()))
      return
    }
    rpc("chat_messages", body: ["target": conversationId, "page_size": 80]) { result in
      switch result {
      case .success(let json):
        completion(Self.parseMessages(json, selfId: selfId))
      case .failure:
        completion([])
      }
    }
  }

  func sendText(conversationId: String, body: String, replyToId: String?, operationId: String,
                completion: @escaping (Bool) -> Void) {
    rpc("apply_chat_operation", body: [
      "operation_id": operationId,
      "operation_type": "message",
      "payload": [
        "conversationId": conversationId,
        "kind": "text",
        "body": body,
        "replyToId": replyToId ?? ""
      ]
    ]) { result in
      if case .success = result { completion(true) } else { completion(false) }
    }
  }

  func searchPeople(query: String, completion: @escaping ([ChatProfile]) -> Void) {
    rpc("search_chat_profiles", body: ["query": query]) { result in
      switch result {
      case .success(let json):
        completion(Self.parseProfiles(json))
      case .failure:
        completion([])
      }
    }
  }

  func openDirect(userId: String, completion: @escaping (String?) -> Void) {
    rpc("open_direct_conversation", body: ["other_user": userId]) { result in
      completion(Self.parseId(result))
    }
  }

  func createGroup(title: String, memberIds: [String] = [], completion: @escaping (String?) -> Void) {
    rpc("create_group_conversation", body: ["p_title": title, "member_ids": memberIds]) { result in
      completion(Self.parseId(result))
    }
  }

  func publishStory(caption: String, audience: String, completion: @escaping (String?) -> Void) {
    let allowed = ChatLogic.normalizeStoryAudience(audience)
    rpc("publish_story", body: ["p_caption": caption, "p_kind": "text", "p_audience": allowed]) { result in
      completion(Self.parseId(result))
    }
  }

  func markStoryViewed(id: String) {
    rpc("mark_story_viewed", body: ["target": id]) { _ in }
  }

  private func rpc(_ name: String, body: [String: Any], completion: @escaping (Result<Any, Error>) -> Void) {
    guard let token = accessToken, let url = URL(string: supabaseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + "/rest/v1/rpc/" + name) else {
      completion(.failure(URLError(.badURL)))
      return
    }
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue(anonKey, forHTTPHeaderField: "apikey")
    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    request.httpBody = try? JSONSerialization.data(withJSONObject: body)
    session.dataTask(with: request) { data, response, error in
      if let error {
        DispatchQueue.main.async { completion(.failure(error)) }
        return
      }
      let http = response as? HTTPURLResponse
      guard let data, http?.statusCode ?? 500 < 300 else {
        DispatchQueue.main.async { completion(.failure(URLError(.badServerResponse))) }
        return
      }
      let json = (try? JSONSerialization.jsonObject(with: data)) ?? []
      DispatchQueue.main.async { completion(.success(json)) }
    }.resume()
  }

  private static func parseInbox(_ json: Any) -> [ChatConversation] {
    guard let rows = json as? [[String: Any]] else { return [] }
    return rows.compactMap { row in
      guard let id = (row["conversation_id"] as? String) ?? (row["id"] as? String) else { return nil }
      let kind = row["kind"] as? String ?? "direct"
      let body = row["last_message_body"] as? String
      return ChatConversation(
        id: id,
        kind: kind,
        title: (row["title"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "Chat",
        peerId: row["peer_id"] as? String,
        peerUsername: row["peer_username"] as? String,
        preview: ChatLogic.inboxPreview(kind: row["last_message_kind"] as? String ?? "text", body: body),
        lastMessageKind: row["last_message_kind"] as? String ?? "text",
        lastMessageAt: parseTime(row["last_message_at"]),
        unread: row["unread_count"] as? Int ?? 0,
        pinned: row["pinned"] as? Bool ?? false,
        verified: row["peer_verified"] as? Bool ?? false,
        muted: row["muted"] as? Bool ?? false,
        peerOnline: row["peer_online"] as? Bool ?? false
      )
    }
  }

  private static func parseStories(_ json: Any) -> [ChatStory] {
    guard let rows = json as? [[String: Any]] else { return [] }
    return rows.compactMap { row in
      guard let id = row["id"] as? String else { return nil }
      return ChatStory(
        id: id,
        authorName: row["author_name"] as? String ?? "",
        authorUsername: row["author_username"] as? String ?? "",
        viewed: row["viewed"] as? Bool ?? false,
        authorId: row["author_id"] as? String ?? "",
        caption: row["caption"] as? String ?? "",
        audience: ChatLogic.normalizeStoryAudience(row["audience"] as? String),
        mine: row["mine"] as? Bool ?? false
      )
    }
  }

  private static func parseProfiles(_ json: Any) -> [ChatProfile] {
    guard let rows = json as? [[String: Any]] else { return [] }
    return rows.compactMap { row in
      guard let id = row["id"] as? String else { return nil }
      return ChatProfile(
        id: id,
        username: row["username"] as? String ?? "",
        displayName: row["display_name"] as? String ?? "",
        verified: row["is_verified"] as? Bool ?? false
      )
    }
  }

  private static func parseMessages(_ json: Any, selfId: String?) -> [ChatMessage] {
    guard let rows = json as? [[String: Any]] else { return [] }
    return rows.reversed().compactMap { row in
      guard let id = row["id"] as? String else { return nil }
      let sender = row["sender_id"] as? String ?? ""
      return ChatMessage(
        id: id,
        conversationId: row["conversation_id"] as? String ?? "",
        senderId: sender,
        senderName: row["sender_name"] as? String ?? "",
        kind: row["kind"] as? String ?? "text",
        body: row["body"] as? String,
        sentAt: parseTime(row["sent_at"]),
        mine: sender == selfId,
        deleted: row["deleted_at"] != nil && !(row["deleted_at"] is NSNull),
        state: row["state"] as? String ?? "sent"
      )
    }
  }

  private static func parseId(_ result: Result<Any, Error>) -> String? {
    guard case .success(let json) = result else { return nil }
    if let id = json as? String { return id }
    if let id = json as? [String] { return id.first }
    return nil
  }

  private static func parseTime(_ value: Any?) -> Int64 {
    if let number = value as? Int64 { return number }
    if let number = value as? Int { return Int64(number) }
    if let string = value as? String, let number = Int64(string) { return number }
    return 0
  }
}

private func nowMs() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
