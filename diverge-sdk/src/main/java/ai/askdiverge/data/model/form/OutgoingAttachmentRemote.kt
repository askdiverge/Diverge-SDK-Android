package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire `Attachment` on `/actions` (ticket attachments and form file fields).
 *
 * [API ref](https://docs.askdiverge.ai/api#model/attachment)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class OutgoingAttachmentRemote(
    val filename: String,
    val mime_type: String,
    val data_base64: String
)
