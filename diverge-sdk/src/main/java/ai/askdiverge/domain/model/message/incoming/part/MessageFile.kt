package ai.askdiverge.domain.model.message.incoming.part

/**
 * A file attachment part on a persisted message, in history or in `done`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/filecontent)
 */
internal data class MessageFile(
    val filename: String,
    /**
     * Download URL as the API sent it. May be absolute, a bare path, or a `data:` URL; resolve
     * with [ai.askdiverge.domain.model.AttachmentUrl] before opening.
     */
    val url: String,
    val attachmentId: String?,
    val mimeType: String?,
    val sizeBytes: Int?,
    /** ISO 8601 date-time — optional expiry for signed download URLs. */
    val urlExpiresAt: String?
) : MessagePart
