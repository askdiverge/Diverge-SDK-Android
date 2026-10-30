package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire shape of a `show_support_ticket` message part. Every key is required.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showsupportticketmarker)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ShowSupportTicketRemote(
    val part_id: String,
    val fields: List<FormFieldRemote>,
    val attachments_accepted: Boolean,
    val max_attachment_size_bytes: Int
)
