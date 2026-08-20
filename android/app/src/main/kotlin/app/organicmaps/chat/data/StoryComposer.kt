package app.organicmaps.chat.data

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import app.organicmaps.R
import app.organicmaps.chats.ChatLogic

/** Text story composer with audience selection. Camera remains the photo/video path. */
object StoryComposer {
  fun interface Published { fun onPublished(id: String) }
  fun interface CameraAction { fun openCamera() }

  @JvmStatic
  fun show(fragment: Fragment, onPublished: Published, onCamera: CameraAction) {
    val context = fragment.requireContext()
    val input = EditText(context)
    input.hint = fragment.getString(R.string.story_caption_hint)
    input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
      InputType.TYPE_TEXT_FLAG_MULTI_LINE
    AlertDialog.Builder(context)
      .setTitle(R.string.story_create)
      .setView(input)
      .setPositiveButton(R.string.story_post) { _, _ ->
        val caption = input.text.toString().trim()
        if (caption.isEmpty()) {
          Toast.makeText(context, R.string.story_caption_hint, Toast.LENGTH_SHORT).show()
          return@setPositiveButton
        }
        chooseAudience(fragment, caption, onPublished)
      }
      .setNeutralButton(R.string.chats_attach_camera) { _, _ -> onCamera.openCamera() }
      .setNegativeButton(R.string.cancel, null)
      .show()
  }

  private fun chooseAudience(fragment: Fragment, caption: String, onPublished: Published) {
    val audiences = ChatLogic.storyAudiences.toTypedArray()
    val labels = arrayOf(
      fragment.getString(R.string.story_audience_friends),
      fragment.getString(R.string.story_audience_only_me)
    )
    AlertDialog.Builder(fragment.requireContext())
      .setTitle(R.string.story_privacy)
      .setItems(labels) { _, which ->
        ChatServices.get(fragment.requireContext()).repository.publishStory(
          caption, audiences[which], object : ChatRepository.IdCallback {
            override fun onReady(id: String) {
              fragment.requireActivity().runOnUiThread { onPublished.onPublished(id) }
            }
            override fun onError(message: String) {
              fragment.requireActivity().runOnUiThread {
                Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show()
              }
            }
          }
        )
      }
      .show()
  }
}
