import UIKit

/// Chats inbox, groups, and conversation host. Uses ChatClient + ChatLogic.
final class ChatsInboxViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
  private let table = UITableView(frame: .zero, style: .plain)
  private var conversations: [ChatConversation] = []
  private var stories: [ChatStory] = []
  private var filter = ChatLogic.filterAll
  private let client = ChatClient.shared

  override func viewDidLoad() {
    super.viewDidLoad()
    title = "Chats"
    view.backgroundColor = .systemBackground
    table.translatesAutoresizingMaskIntoConstraints = false
    table.dataSource = self
    table.delegate = self
    table.register(UITableViewCell.self, forCellReuseIdentifier: "chat")
    view.addSubview(table)
    NSLayoutConstraint.activate([
      table.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
      table.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      table.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      table.bottomAnchor.constraint(equalTo: view.bottomAnchor)
    ])
    navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: .compose, target: self, action: #selector(newChat))
    reload()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    reload()
  }

  private func reload() {
    client.loadInbox { [weak self] rows, stories, _ in
      guard let self else { return }
      self.conversations = rows.filter { ChatLogic.matchesCategory($0, category: self.filter) }
      self.stories = stories
      self.renderStoriesHeader()
      self.table.reloadData()
    }
  }

  @objc private func newChat() {
    let alert = UIAlertController(title: "New chat", message: "Search people by name or username.", preferredStyle: .alert)
    alert.addTextField { $0.placeholder = "Search people" }
    alert.addAction(UIAlertAction(title: "Search", style: .default) { [weak self] _ in
      let query = alert.textFields?.first?.text ?? ""
      self?.client.searchPeople(query: query) { profiles in
        guard let profile = profiles.first else {
          self?.openConversation("demo-alfred")
          return
        }
        self?.client.openDirect(userId: profile.id) { id in
          self?.openConversation(id ?? "demo-alfred")
        }
      }
    })
    alert.addAction(UIAlertAction(title: "Demo Alfred", style: .default) { [weak self] _ in
      self?.openConversation("demo-alfred")
    })
    alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
    present(alert, animated: true)
  }

  func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { conversations.count }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
    let cell = tableView.dequeueReusableCell(withIdentifier: "chat", for: indexPath)
    let item = conversations[indexPath.row]
    var config = cell.defaultContentConfiguration()
    config.text = (item.verified ? "\(item.title)  ✓" : item.title) + (item.peerOnline ? "  ●" : "")
    config.secondaryText = item.preview
    cell.contentConfiguration = config
    cell.accessoryType = item.unread > 0 ? .detailButton : .disclosureIndicator
    return cell
  }

  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    tableView.deselectRow(at: indexPath, animated: true)
    openConversation(conversations[indexPath.row].id)
  }

  private func openConversation(_ id: String) {
    let conversation = conversations.first { $0.id == id }
    navigationController?.pushViewController(ChatConversationViewController(conversationId: id, titleText: conversation?.title ?? "Chat"), animated: true)
  }
}

final class ChatConversationViewController: UIViewController, UITableViewDataSource {
  private let conversationId: String
  private let titleText: String
  private let table = UITableView(frame: .zero, style: .plain)
  private let input = UITextField()
  private var messages: [ChatMessage] = []
  private let client = ChatClient.shared

  init(conversationId: String, titleText: String) {
    self.conversationId = conversationId
    self.titleText = titleText
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) { nil }

  override func viewDidLoad() {
    super.viewDidLoad()
    title = titleText
    view.backgroundColor = .systemBackground
    table.translatesAutoresizingMaskIntoConstraints = false
    table.dataSource = self
    table.register(UITableViewCell.self, forCellReuseIdentifier: "bubble")
    input.translatesAutoresizingMaskIntoConstraints = false
    input.placeholder = "Message"
    input.borderStyle = .roundedRect
    input.returnKeyType = .send
    input.addTarget(self, action: #selector(send), for: .editingDidEndOnExit)
    view.addSubview(table)
    view.addSubview(input)
    NSLayoutConstraint.activate([
      table.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
      table.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      table.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      table.bottomAnchor.constraint(equalTo: input.topAnchor, constant: -8),
      input.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 12),
      input.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -12),
      input.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -8),
      input.heightAnchor.constraint(equalToConstant: 44)
    ])
    reload()
  }

  private func reload() {
    client.loadMessages(conversationId: conversationId, selfId: "me") { [weak self] rows in
      self?.messages = rows
      self?.table.reloadData()
      if !rows.isEmpty {
        self?.table.scrollToRow(at: IndexPath(row: rows.count - 1, section: 0), at: .bottom, animated: false)
      }
    }
  }

  @objc private func send() {
    let text = input.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    guard !text.isEmpty else { return }
    input.text = ""
    let operationId = UUID().uuidString
    let optimistic = ChatMessage(id: operationId, conversationId: conversationId, senderId: "me",
                                 senderName: "You", kind: "text", body: text,
                                 sentAt: Int64(Date().timeIntervalSince1970 * 1000), mine: true, state: "queued")
    messages.append(optimistic)
    table.reloadData()
    client.sendText(conversationId: conversationId, body: text, replyToId: nil, operationId: operationId) { [weak self] _ in
      self?.reload()
    }
  }

  func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { messages.count }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
    let cell = tableView.dequeueReusableCell(withIdentifier: "bubble", for: indexPath)
    let message = messages[indexPath.row]
    var config = cell.defaultContentConfiguration()
    config.text = message.deleted ? "This message was deleted" : (message.body ?? "")
    config.secondaryText = message.mine ? "You · \(message.state)" : message.senderName
    cell.contentConfiguration = config
    cell.textLabel?.textAlignment = message.mine ? .right : .left
    return cell
  }
}
