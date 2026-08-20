package app.organicmaps.chat.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import app.organicmaps.chats.ChatModels.Contact

/** Reads the on-device address book. Never uploads the full list — only a user-chosen contact is sent. */
object DeviceContacts {
  const val REQUEST = 215

  @JvmField
  var pendingGrant: Runnable? = null

  fun hasPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

  fun request(fragment: Fragment, onGranted: Runnable) {
    if (hasPermission(fragment.requireContext())) {
      onGranted.run()
      return
    }
    pendingGrant = onGranted
    fragment.requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST)
  }

  fun onPermissionResult(grantResults: IntArray): Boolean {
    val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    val pending = pendingGrant
    pendingGrant = null
    if (granted) pending?.run()
    return granted
  }

  fun load(context: Context, limit: Int = 250): List<Contact> {
    if (!hasPermission(context)) return emptyList()
    val byId = LinkedHashMap<String, MutableContact>()
    context.contentResolver.query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
      ),
      null,
      null,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC"
    )?.use { cursor ->
      val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
      val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
      val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
      while (cursor.moveToNext() && byId.size < limit) {
        val id = if (idIdx >= 0) cursor.getString(idIdx) ?: continue else continue
        val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
        if (name.isEmpty()) continue
        val phone = if (phoneIdx >= 0) cursor.getString(phoneIdx)?.trim() else null
        val existing = byId.getOrPut(id) { MutableContact(id, name, null, null) }
        if (existing.phone.isNullOrEmpty() && !phone.isNullOrEmpty()) existing.phone = phone
      }
    }
    context.contentResolver.query(
      ContactsContract.CommonDataKinds.Email.CONTENT_URI,
      arrayOf(
        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Email.ADDRESS
      ),
      null,
      null,
      null
    )?.use { cursor ->
      val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
      val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
      val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
      while (cursor.moveToNext()) {
        val id = if (idIdx >= 0) cursor.getString(idIdx) ?: continue else continue
        val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
        val email = if (emailIdx >= 0) cursor.getString(emailIdx)?.trim() else null
        val existing = byId[id]
        if (existing != null) {
          if (existing.email.isNullOrEmpty() && !email.isNullOrEmpty()) existing.email = email
        } else if (byId.size < limit && (name.isNotEmpty() || !email.isNullOrEmpty())) {
          byId[id] = MutableContact(id, name.ifEmpty { email ?: "Contact" }, null, email)
        }
      }
    }
    return byId.values
      .filter { !it.phone.isNullOrEmpty() || !it.email.isNullOrEmpty() }
      .map { Contact(it.id, it.name, it.phone, it.email) }
      .sortedBy { it.name.lowercase() }
  }

  private class MutableContact(
    val id: String,
    val name: String,
    var phone: String?,
    var email: String?
  )
}
