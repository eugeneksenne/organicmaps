package app.organicmaps.chat.data

import android.app.AlertDialog
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chat.realtime.ChatConnectionState
import app.organicmaps.chats.ChatLogic
import app.organicmaps.chats.ChatModels
import app.organicmaps.chats.ChatModels.Conversation
import app.organicmaps.chats.ChatModels.Story

/** Binds the chats inbox to the repository, cache, and optional realtime engine. */
class ChatInboxBinder(
  private val fragment: Fragment,
  private val root: View,
  private val host: Host
) : ChatServices.Listener {
  interface Host {
    fun openConversation(conversationId: String)
    fun openStories()
    fun openStory(name: String)
    fun composeStory()
    fun openCalls()
    fun openGroups()
  }

  private val services = ChatServices.get(fragment.requireContext())
  private val repository = services.repository
  private val refresh = Runnable { loop() }
  private var attached = false
  private var filter = ChatModels.FILTER_ALL
  private var query = ""
  private var conversations: List<Conversation> = emptyList()
  private var stories: List<Story> = emptyList()

  fun start() {
    attached = true
    services.addListener(this)
    services.onForeground()
    bindChrome()
    loop()
    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(view: View) {}
      override fun onViewDetachedFromWindow(view: View) {
        attached = false
        services.removeListener(this@ChatInboxBinder)
        root.removeCallbacks(refresh)
      }
    })
  }

  private fun bindChrome() {
    root.findViewById<View>(R.id.chats_search).setOnClickListener { toggleSearch() }
    root.findViewById<View>(R.id.chats_new).setOnClickListener { showComposerMenu(it) }
    root.findViewById<View>(R.id.chats_account).setOnClickListener { showAccountMenu(it) }
    bindFilter(R.id.chats_filter_all, ChatModels.FILTER_ALL)
    bindFilter(R.id.chats_filter_unread, ChatModels.FILTER_UNREAD)
    bindFilter(R.id.chats_filter_personal, ChatModels.FILTER_PERSONAL)
    bindFilter(R.id.chats_filter_venues, ChatModels.FILTER_VENUE)
    root.findViewById<View>(R.id.chats_filter_groups)?.setOnClickListener { host.openGroups() }
    root.findViewById<View>(R.id.chats_filter_calls)?.setOnClickListener { host.openCalls() }
    root.findViewById<View>(R.id.chats_auth_signin).setOnClickListener { submitAuth(false) }
    root.findViewById<View>(R.id.chats_auth_signup).setOnClickListener { submitAuth(true) }
    root.findViewById<EditText>(R.id.chats_search_input).addTextChangedListener(SimpleWatcher { text ->
      query = text.trim()
      renderInbox(conversations, stories)
    })
    renderAuth()
  }

  private fun bindFilter(id: Int, value: String) {
    root.findViewById<TextView>(id)?.setOnClickListener {
      filter = value
      highlightFilters()
      renderInbox(conversations, stories)
    }
  }

  private fun highlightFilters() {
    highlight(R.id.chats_filter_all, filter == ChatModels.FILTER_ALL)
    highlight(R.id.chats_filter_unread, filter == ChatModels.FILTER_UNREAD)
    highlight(R.id.chats_filter_personal, filter == ChatModels.FILTER_PERSONAL)
    highlight(R.id.chats_filter_venues, filter == ChatModels.FILTER_VENUE)
  }

  private fun highlight(id: Int, active: Boolean) {
    val chip = root.findViewById<TextView>(id) ?: return
    chip.setBackgroundResource(if (active) R.drawable.chat_category_active else R.drawable.camera_look)
    chip.setTextColor(if (active) 0xFFFFFFFF.toInt() else 0xDE000000.toInt())
  }

  private fun toggleSearch() {
    val search = root.findViewById<EditText>(R.id.chats_search_input)
    val show = search.visibility != View.VISIBLE
    search.visibility = if (show) View.VISIBLE else View.GONE
    if (!show) {
      search.setText("")
      query = ""
      renderInbox(conversations, stories)
    }
  }

  private fun loop() {
    refreshInbox()
    if (attached) {
      val delay = if (services.connectionState == ChatConnectionState.connected) 30_000L else 8_000L
      root.postDelayed(refresh, delay)
    }
  }

  private fun refreshInbox() {
    if (!attached || !fragment.isAdded) return
    repository.loadInbox(object : ChatRepository.InboxCallback {
      override fun onLoaded(conversations: List<Conversation>, stories: List<Story>, live: Boolean) {
        runOnUi {
          this@ChatInboxBinder.conversations = conversations
          this@ChatInboxBinder.stories = stories
          renderInbox(conversations, stories)
        }
      }

      override fun onStatus(status: String) {
        runOnUi { setStatus(status) }
      }
    })
  }

  private fun renderInbox(conversations: List<Conversation>, stories: List<Story>) {
    renderStories(stories)
    val pinned = root.findViewById<LinearLayout>(R.id.chats_pinned)
    val recent = root.findViewById<LinearLayout>(R.id.chats_recent)
    pinned.removeAllViews()
    recent.removeAllViews()
    var pinnedCount = 0
    var visible = 0
    for (conversation in conversations) {
      if (!ChatLogic.matchesCategory(conversation, filter)) continue
      if (!ChatLogic.matchesQuery(conversation, query) && !messageMatches(conversation.id, query)) continue
      visible++
      if (conversation.pinned) {
        pinned.addView(row(conversation))
        pinnedCount++
      } else recent.addView(row(conversation))
    }
    root.findViewById<View>(R.id.chats_pinned_label).visibility = if (pinnedCount == 0) View.GONE else View.VISIBLE
    root.findViewById<View>(R.id.chats_recent_label).visibility = if (recent.childCount == 0) View.GONE else View.VISIBLE
    val empty = root.findViewById<TextView>(R.id.chats_empty)
    empty.visibility = if (visible == 0) View.VISIBLE else View.GONE
    empty.setText(if (conversations.isEmpty()) R.string.chats_empty else R.string.chats_empty_filter)
    renderAuth()
  }

  private fun messageMatches(conversationId: String, query: String): Boolean {
    if (query.length < 2) return false
    return repository.store.searchMessages(conversationId, query, repository.session().userId).isNotEmpty()
  }

  private fun renderStories(stories: List<Story>) {
    val row = root.findViewById<LinearLayout>(R.id.chats_stories)
    row.removeAllViews()
    val inflater = LayoutInflater.from(fragment.requireContext())
    val selfId = repository.session().userId
    val mine = ChatLogic.ownStory(stories, selfId, repository.session().displayName)
    val add = inflater.inflate(R.layout.chat_story_item, row, false)
    val myAvatar = add.findViewById<TextView>(R.id.chat_story_avatar)
    myAvatar.text = (repository.session().displayName ?: "F").take(1).uppercase()
    add.findViewById<TextView>(R.id.chat_story_name).setText(R.string.chats_my_story)
    if (mine != null) myAvatar.setBackgroundResource(R.drawable.story_ring)
    add.setOnClickListener {
      if (mine != null) host.openStory(mine.id) else host.composeStory()
    }
    add.setOnLongClickListener {
      host.openStories()
      true
    }
    row.addView(add)
    for (story in ChatLogic.orderedStories(stories, selfId, repository.session().displayName)) {
      val item = inflater.inflate(R.layout.chat_story_item, row, false)
      val name = story.authorName.ifEmpty { story.authorUsername }
      val avatar = item.findViewById<TextView>(R.id.chat_story_avatar)
      avatar.text = name.take(1).uppercase()
      avatar.setBackgroundResource(if (story.viewed) R.drawable.story_ring_viewed else R.drawable.story_ring)
      item.findViewById<TextView>(R.id.chat_story_name).text = name
      item.setOnClickListener { host.openStory(story.id) }
      row.addView(item)
    }
  }

  private fun row(conversation: Conversation): View {
    val view = LayoutInflater.from(fragment.requireContext())
      .inflate(R.layout.chat_row, root.findViewById<ViewGroup>(R.id.chats_recent), false)
    view.findViewById<TextView>(R.id.chat_row_avatar).text = conversation.initials()
    val title = if (conversation.verified) conversation.title + "  ✓" else conversation.title
    val online = if (conversation.peerOnline) "  ●" else ""
    view.findViewById<TextView>(R.id.chat_row_name).text =
      (if (conversation.muted) "$title  🔇" else title) + online
    val preview = if (conversation.preview.isNullOrEmpty()) " "
    else ChatLogic.inboxPreview(conversation.lastMessageKind, conversation.preview)
    view.findViewById<TextView>(R.id.chat_row_preview).text = preview
    view.findViewById<TextView>(R.id.chat_row_time).text =
      ChatLogic.relativeTime(conversation.lastMessageAt, System.currentTimeMillis())
    view.findViewById<View>(R.id.chat_row_unread).visibility = if (conversation.unread > 0) View.VISIBLE else View.GONE
    view.contentDescription = "${conversation.title}. $preview"
    view.setOnClickListener { host.openConversation(conversation.id) }
    view.setOnLongClickListener {
      showConversationActions(view, conversation)
      true
    }
    return view
  }

  private fun renderAuth() {
    val show = repository.isConfigured() && !repository.isSignedIn()
    root.findViewById<View>(R.id.chats_auth).visibility = if (show) View.VISIBLE else View.GONE
    val name = services.session.displayName
    root.findViewById<TextView>(R.id.chats_account).text =
      if (name.isNullOrEmpty()) "☺" else name.take(1).uppercase()
  }

  private fun submitAuth(signUp: Boolean) {
    val email = root.findViewById<EditText>(R.id.chats_auth_email).text.toString().trim()
    val password = root.findViewById<EditText>(R.id.chats_auth_password).text.toString()
    if (email.isEmpty() || password.length < 6) {
      Toast.makeText(fragment.requireContext(), "Enter email and a password of at least 6 characters", Toast.LENGTH_SHORT).show()
      return
    }
    val callback = object : FomoAuthRepository.Callback {
      override fun onSuccess() {
        runOnUi {
          services.onSignedIn()
          renderAuth()
          refreshInbox()
        }
      }
      override fun onError(message: String) {
        runOnUi { Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_LONG).show() }
      }
    }
    if (signUp) services.auth.signUp(email, password, callback) else services.auth.signIn(email, password, callback)
  }

  private fun showComposerMenu(anchor: View) {
    if (repository.isConfigured() && !repository.isSignedIn()) {
      root.findViewById<View>(R.id.chats_auth).visibility = View.VISIBLE
      return
    }
    val menu = PopupMenu(fragment.requireContext(), anchor)
    menu.menu.add(fragment.getString(R.string.chats_search_people))
    menu.menu.add(fragment.getString(R.string.contacts_from_phone))
    menu.menu.add(fragment.getString(R.string.chats_new_group))
    menu.setOnMenuItemClickListener { item ->
      when (item.title) {
        fragment.getString(R.string.chats_new_group) -> showCreateGroup()
        fragment.getString(R.string.contacts_from_phone) -> pickDeviceContact()
        else -> showPeoplePicker()
      }
      true
    }
    menu.show()
  }

  private fun showAccountMenu(anchor: View) {
    val menu = PopupMenu(fragment.requireContext(), anchor)
    if (repository.isSignedIn()) {
      menu.menu.add(services.session.email ?: fragment.getString(R.string.chats_sign_out))
      menu.menu.add(fragment.getString(R.string.chats_sign_out))
    } else menu.menu.add(fragment.getString(R.string.login))
    menu.setOnMenuItemClickListener { item ->
      if (fragment.getString(R.string.chats_sign_out) == item.title) {
        services.auth.signOut()
        services.onSignedOut()
        renderAuth()
        refreshInbox()
      } else if (!repository.isSignedIn()) {
        root.findViewById<View>(R.id.chats_auth).visibility = View.VISIBLE
      }
      true
    }
    menu.show()
  }

  private fun showPeoplePicker() {
    val context = fragment.requireContext()
    val input = EditText(context)
    input.setHint(R.string.chats_search_people)
    input.inputType = InputType.TYPE_CLASS_TEXT
    val results = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val container = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(input)
      addView(results)
    }
    val dialog = AlertDialog.Builder(context)
      .setTitle(R.string.chats_search_people)
      .setView(container)
      .setNegativeButton(R.string.cancel, null)
      .show()
    fun renderContacts(query: String) {
      if (!DeviceContacts.hasPermission(context)) return
      DeviceContacts.load(context)
        .filter { ChatLogic.matchesContactQuery(it, query) }
        .take(20)
        .forEach { contact ->
          val row = TextView(context)
          row.setPadding(24, 20, 24, 20)
          row.text = contact.name + "\n" + listOfNotNull(contact.phone, contact.email).joinToString(" · ")
          row.setTextColor(0xDE000000.toInt())
          row.setOnClickListener {
            dialog.dismiss()
            onDeviceContactChosen(contact)
          }
          results.addView(row)
        }
    }
    input.addTextChangedListener(SimpleWatcher { text ->
      results.removeAllViews()
      renderContacts(text)
      if (text.length < 2) return@SimpleWatcher
      repository.searchPeople(text, object : ChatRepository.ProfilesCallback {
        override fun onLoaded(profiles: List<ChatModels.Profile>) {
          runOnUi {
            results.removeAllViews()
            profiles.forEach { profile ->
              val row = TextView(context)
              row.setPadding(24, 24, 24, 24)
              row.text = "${profile.displayName}  @${profile.username}"
              row.setTextColor(0xDE000000.toInt())
              row.setOnClickListener {
                dialog.dismiss()
                openDirect(profile.id)
              }
              results.addView(row)
            }
            renderContacts(text)
          }
        }
        override fun onError(message: String) {
          runOnUi { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        }
      })
    })
    DeviceContacts.request(fragment) { renderContacts("") }
  }

  private fun pickDeviceContact() {
    DeviceContacts.request(fragment) {
      val context = fragment.requireContext()
      val contacts = DeviceContacts.load(context)
      if (contacts.isEmpty()) {
        Toast.makeText(context, R.string.contacts_empty, Toast.LENGTH_SHORT).show()
        return@request
      }
      val labels = contacts.take(40).map { it.name + "  " + (it.phone ?: it.email ?: "") }.toTypedArray()
      AlertDialog.Builder(context)
        .setTitle(R.string.contacts_from_phone)
        .setItems(labels) { _, which -> onDeviceContactChosen(contacts[which]) }
        .setNegativeButton(R.string.cancel, null)
        .show()
    }
  }

  private fun onDeviceContactChosen(contact: ChatModels.Contact) {
    val query = contact.name.substringBefore(" ")
    if (query.length >= 2 && repository.isSignedIn()) {
      repository.searchPeople(query, object : ChatRepository.ProfilesCallback {
        override fun onLoaded(profiles: List<ChatModels.Profile>) {
          runOnUi {
            val match = profiles.firstOrNull { it.displayName.equals(contact.name, ignoreCase = true) }
              ?: profiles.firstOrNull()
            if (match != null) openDirect(match.id)
            else Toast.makeText(
              fragment.requireContext(),
              fragment.getString(R.string.contacts_not_on_fomo, contact.name),
              Toast.LENGTH_LONG
            ).show()
          }
        }
        override fun onError(message: String) {
          runOnUi {
            Toast.makeText(
              fragment.requireContext(),
              fragment.getString(R.string.contacts_not_on_fomo, contact.name),
              Toast.LENGTH_LONG
            ).show()
          }
        }
      })
    } else {
      Toast.makeText(
        fragment.requireContext(),
        fragment.getString(R.string.contacts_not_on_fomo, contact.name),
        Toast.LENGTH_LONG
      ).show()
    }
  }

  private fun showCreateGroup() {
    val input = EditText(fragment.requireContext())
    input.setHint(R.string.chats_group_name)
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.chats_new_group)
      .setView(input)
      .setNegativeButton(R.string.cancel, null)
      .setPositiveButton(R.string.create) { _, _ ->
        val title = input.text.toString().trim()
        if (title.isEmpty()) return@setPositiveButton
        repository.createGroup(title, object : ChatRepository.IdCallback {
          override fun onReady(id: String) { runOnUi { host.openConversation(id) } }
          override fun onError(message: String) {
            runOnUi { Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show() }
          }
        })
      }
      .show()
  }

  private fun showConversationActions(anchor: View, conversation: Conversation) {
    val menu = PopupMenu(fragment.requireContext(), anchor)
    menu.menu.add(fragment.getString(R.string.chats_pin).takeIf { !conversation.pinned }
      ?: fragment.getString(R.string.chats_unpin))
    menu.menu.add(
      if (conversation.muted) fragment.getString(R.string.chats_unmute)
      else fragment.getString(R.string.chats_mute)
    )
    menu.menu.add(fragment.getString(R.string.chats_clear))
    if (conversation.kind == "group") menu.menu.add(fragment.getString(R.string.chats_leave))
    menu.setOnMenuItemClickListener { item ->
      when (item.title) {
        fragment.getString(R.string.chats_pin), fragment.getString(R.string.chats_unpin) ->
          repository.setPinned(conversation.id, !conversation.pinned)
        fragment.getString(R.string.chats_mute), fragment.getString(R.string.chats_unmute) ->
          repository.setMuted(conversation.id, !conversation.muted)
        fragment.getString(R.string.chats_clear) -> repository.clearHistory(conversation.id)
        fragment.getString(R.string.chats_leave) -> repository.leaveConversation(conversation.id)
      }
      refreshInbox()
      true
    }
    menu.show()
  }

  private fun openDirect(userId: String) {
    repository.openDirect(userId, object : ChatRepository.IdCallback {
      override fun onReady(id: String) { runOnUi { host.openConversation(id) } }
      override fun onError(message: String) {
        runOnUi { Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show() }
      }
    })
  }

  private fun setStatus(status: String) {
    var text = status
    val view = root.findViewById<TextView>(R.id.chats_status)
    if (text.isEmpty()) {
      text = when {
        services.connectionState == ChatConnectionState.offline && repository.isSignedIn() ->
          fragment.getString(R.string.chats_offline)
        services.connectionState == ChatConnectionState.connecting ||
          services.connectionState == ChatConnectionState.reconnecting ->
          fragment.getString(R.string.chats_connecting)
        else -> ""
      }
    }
    view.text = text
    view.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
  }

  private fun runOnUi(action: () -> Unit) {
    if (fragment.isAdded) fragment.requireActivity().runOnUiThread(action)
  }

  override fun onInboxHint() { refreshInbox() }
  override fun onConversationHint(conversationId: String) { refreshInbox() }
  override fun onTyping(conversationId: String, userId: String, isTyping: Boolean) {}
  override fun onConnectionState(state: ChatConnectionState) { runOnUi { setStatus("") } }

  private class SimpleWatcher(private val listener: (String) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { listener(s.toString()) }
    override fun afterTextChanged(s: Editable) {}
  }
}
