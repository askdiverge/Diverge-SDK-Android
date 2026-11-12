package ai.askdiverge.data.model.message.outgoing

/**
 * Body literals for outgoing message parts (`ImageInput` / `FileInput`). Tests pin these strings.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/imageinput)
 */
internal object OutgoingMessageWire {

    /** Base64 payload on an image or file part. */
    const val DATA = "data"

    /** MIME type on an image or file part. */
    const val MIME = "mime"

    /** Optional filename on an image or file part. */
    const val FILENAME = "filename"
}
