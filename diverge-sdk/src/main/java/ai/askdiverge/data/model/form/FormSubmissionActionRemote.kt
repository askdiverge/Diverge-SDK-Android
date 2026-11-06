package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/** Wire `form_submission` action for a `show_form` marker. */
@Keep
@JsonClass(generateAdapter = true)
internal data class FormSubmissionActionRemote(
    val form_id: String? = null,
    val values: Map<String, String>? = null,
    val files: Map<String, OutgoingAttachmentRemote>? = null
) : SubmitActionRemote
