package ai.askdiverge.domain.model.message.incoming.part

/**
 * A file attachment part on a persisted message, in history or in `done`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/filecontent)
 */
internal data class MessageFile(
    val filename: String,
    /**
     * Download URL: absolute, or a `data:` URL. A bare path from the API is resolved against the
     * API root in `data`; one that cannot be resolved is kept as sent and cannot be opened.
     */
    val url: String,
    val attachmentId: String?,
    val mimeType: String?,
    val sizeBytes: Int?,
    /** ISO 8601 date-time — optional expiry for signed download URLs. */
    val urlExpiresAt: String?
) : MessagePart
