# FOMO Chats (iOS)

UIKit adapters for the shared chat contract.

- `ChatLogic.swift` — keep in lockstep with `shared/chats/src/commonMain/kotlin/app/organicmaps/chats/ChatLogic.kt`
- `ChatClient.swift` — Supabase RPC client (anon key + user JWT only): inbox, stories, people search, open-direct, groups, publish-story
- `ChatsInboxViewController.swift` — inbox + conversation
- `CallScreenViewController.swift` — outgoing, incoming, active voice/video, group participants
- `DeviceContacts.swift` — CNContactStore access for sharing and messaging contacts
- `StoryViewerViewController.swift` — story playback and reply

Add these files to the Organic Maps Xcode target (Maps) if they are not already in `project.pbxproj`.
