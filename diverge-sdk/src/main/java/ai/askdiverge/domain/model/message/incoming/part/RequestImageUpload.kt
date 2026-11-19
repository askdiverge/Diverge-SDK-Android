package ai.askdiverge.domain.model.message.incoming.part

/**
 * Instructs the client to prompt the visitor to upload an image. Carries accepted MIME types and a
 * size limit; the SDK owns all copy. Arrives whole, as a `part` event, in history or in `done`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/requestimageuploadmarker)
 */
internal data class RequestImageUpload(
    /** Stable id of this marker part. */
    val partId: String,
    /** MIME types the backend accepts, for example `image/png`. */
    val acceptedTypes: List<String>,
    /** Largest image the backend accepts, in decoded bytes. */
    val maxSizeBytes: Int
) : MessagePart
