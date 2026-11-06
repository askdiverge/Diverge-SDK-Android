package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/** Wire `support_ticket` action. */
@Keep
@JsonClass(generateAdapter = true)
internal data class SupportTicketActionRemote(
    val name: String,
    val email: String,
    val message: String,
    val attachments: List<OutgoingAttachmentRemote>? = null
) : SubmitActionRemote
