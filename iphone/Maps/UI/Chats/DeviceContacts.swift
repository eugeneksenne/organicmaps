import Contacts
import Foundation

/// Reads the on-device address book. The full list is never uploaded.
enum DeviceContacts {
  static func requestAccess(completion: @escaping (Bool) -> Void) {
    let store = CNContactStore()
    switch CNContactStore.authorizationStatus(for: .contacts) {
    case .authorized:
      completion(true)
    case .limited:
      completion(true)
    default:
      store.requestAccess(for: .contacts) { granted, _ in
        DispatchQueue.main.async { completion(granted) }
      }
    }
  }

  static func load(limit: Int = 250) -> [ChatContact] {
    let store = CNContactStore()
    let keys: [CNKeyDescriptor] = [
      CNContactGivenNameKey as CNKeyDescriptor,
      CNContactFamilyNameKey as CNKeyDescriptor,
      CNContactPhoneNumbersKey as CNKeyDescriptor,
      CNContactEmailAddressesKey as CNKeyDescriptor
    ]
    let request = CNContactFetchRequest(keysToFetch: keys)
    request.sortOrder = .givenName
    var result: [ChatContact] = []
    try? store.enumerateContacts(with: request) { contact, stop in
      let name = [contact.givenName, contact.familyName].filter { !$0.isEmpty }.joined(separator: " ")
      let phone = contact.phoneNumbers.first?.value.stringValue
      let email = contact.emailAddresses.first?.value as String?
      if name.isEmpty && phone == nil && email == nil { return }
      result.append(ChatContact(id: contact.identifier, name: name.isEmpty ? (phone ?? email ?? "Contact") : name,
                                phone: phone, email: email))
      if result.count >= limit { stop.pointee = true }
    }
    return result
  }
}

struct ChatContact {
  var id: String
  var name: String
  var phone: String?
  var email: String?
}

extension ChatLogic {
  static func contactCard(_ contact: ChatContact) -> String {
    var lines = ["👤 \(contact.name)"]
    if let phone = contact.phone, !phone.isEmpty { lines.append("📞 \(phone)") }
    if let email = contact.email, !email.isEmpty { lines.append("✉️ \(email)") }
    return lines.joined(separator: "\n")
  }
}
