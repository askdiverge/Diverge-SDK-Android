package ai.askdiverge.domain.model.form

/**
 * Instructs the client to display a support-ticket form. Field definitions and attachment
 * constraints travel inline.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showsupportticketmarker)
 */
internal data class ShowSupportTicket(
    val partId: String,
    val fields: List<FormField>,
    val attachmentsAccepted: Boolean,
    val maxAttachmentSizeBytes: Int
)
