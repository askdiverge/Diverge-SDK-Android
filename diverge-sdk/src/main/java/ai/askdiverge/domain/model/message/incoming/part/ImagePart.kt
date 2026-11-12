package ai.askdiverge.domain.model.message.incoming.part

/**
 * An image part on a persisted message (history / `done` / livechat log). Never streams.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/imagecontent)
 */
internal data class ImagePart(
    /**
     * Full-resolution image URL as the API sent it. May be absolute, a bare path, or a `data:`
     * URL; resolve with [ai.askdiverge.domain.model.AttachmentUrl] before opening or loading.
     */
    val url: String,
    /** Diverge's id for the stored image; `null` when Diverge does not store it. */
    val attachmentId: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Int? = null,
    /** ISO 8601 date-time — optional expiry for signed download URLs. */
    val urlExpiresAt: String? = null,
    /**
     * Thumbnail URL string as the API sent it. `null` when absent or unusable; the client then
     * loads [url].
     */
    val thumbnailUrl: String? = null,
    /** Text shown with the image; `null` when the sender gave none. */
    val caption: String? = null
) : MessagePart
