import UIKit

/// Outgoing, incoming, active voice/video, and group call UI.
final class CallScreenViewController: UIViewController {
  private let mode: String
  private let peerName: String
  private let status = UILabel()
  private let answer = UIButton(type: .system)
  private var elapsed = 0
  private var timer: Timer?
  private var video: Bool { mode.contains("video") }
  private var incoming: Bool { mode.hasPrefix("incoming") }
  private var group: Bool { mode.contains("group") }

  init(mode: String, peerName: String) {
    self.mode = mode
    self.peerName = peerName
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) { nil }

  override func viewDidLoad() {
    super.viewDidLoad()
    view.backgroundColor = UIColor(red: 0.12, green: 0.09, blue: 0.16, alpha: 1)
    let name = UILabel()
    name.text = peerName
    name.textColor = .white
    name.font = .boldSystemFont(ofSize: 28)
    name.textAlignment = .center
    status.textColor = UIColor.white.withAlphaComponent(0.8)
    status.textAlignment = .center
    status.text = incoming ? (video ? "Incoming video call" : "Incoming voice call") : "Calling…"
    let avatar = UILabel()
    avatar.text = String(peerName.prefix(1)).uppercased()
    avatar.textAlignment = .center
    avatar.font = .boldSystemFont(ofSize: 44)
    avatar.textColor = .white
    avatar.backgroundColor = UIColor.white.withAlphaComponent(0.15)
    avatar.layer.cornerRadius = 78
    avatar.clipsToBounds = true

    let mute = control("Mute")
    let speaker = control("Speaker")
    let camera = control("Camera")
    camera.isHidden = !video
    let people = control("People")
    people.isHidden = !group
    people.addTarget(self, action: #selector(showParticipants), for: .touchUpInside)

    let hangup = UIButton(type: .system)
    hangup.setTitle("End", for: .normal)
    hangup.setTitleColor(.white, for: .normal)
    hangup.backgroundColor = .systemRed
    hangup.layer.cornerRadius = 28
    hangup.addTarget(self, action: #selector(endCall), for: .touchUpInside)

    answer.setTitle("Answer", for: .normal)
    answer.setTitleColor(.white, for: .normal)
    answer.backgroundColor = .systemGreen
    answer.layer.cornerRadius = 28
    answer.isHidden = !incoming
    answer.addTarget(self, action: #selector(answerCall), for: .touchUpInside)

    let stack = UIStackView(arrangedSubviews: [status, avatar, name, mute, speaker, camera, people, hangup, answer])
    stack.axis = .vertical
    stack.alignment = .center
    stack.spacing = 16
    stack.translatesAutoresizingMaskIntoConstraints = false
    avatar.widthAnchor.constraint(equalToConstant: 156).isActive = true
    avatar.heightAnchor.constraint(equalToConstant: 156).isActive = true
    hangup.widthAnchor.constraint(equalToConstant: 120).isActive = true
    hangup.heightAnchor.constraint(equalToConstant: 56).isActive = true
    answer.widthAnchor.constraint(equalToConstant: 120).isActive = true
    answer.heightAnchor.constraint(equalToConstant: 56).isActive = true
    view.addSubview(stack)
    NSLayoutConstraint.activate([
      stack.centerXAnchor.constraint(equalTo: view.centerXAnchor),
      stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
      stack.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 24)
    ])
  }

  @objc private func answerCall() {
    answer.isHidden = true
    status.text = "00:00"
    startTimer()
  }

  @objc private func endCall() {
    timer?.invalidate()
    navigationController?.popViewController(animated: true)
    dismiss(animated: true)
  }

  @objc private func showParticipants() {
    let alert = UIAlertController(title: "Participants", message: "\(peerName)\nYou", preferredStyle: .actionSheet)
    alert.addAction(UIAlertAction(title: "Close", style: .cancel))
    present(alert, animated: true)
  }

  private func startTimer() {
    timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
      guard let self else { return }
      self.elapsed += 1
      self.status.text = ChatLogic.liveTimer(elapsedSec: self.elapsed)
    }
  }

  private func control(_ title: String) -> UIButton {
    let button = UIButton(type: .system)
    button.setTitle(title, for: .normal)
    button.setTitleColor(.white, for: .normal)
    return button
  }
}

extension ChatLogic {
  static func liveTimer(elapsedSec: Int) -> String {
    let minutes = elapsedSec / 60
    let seconds = elapsedSec % 60
    return String(format: "%02d:%02d", minutes, seconds)
  }
}
