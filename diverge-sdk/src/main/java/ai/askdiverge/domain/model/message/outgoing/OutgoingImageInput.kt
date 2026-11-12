package ai.askdiverge.domain.model.message.outgoing

/**
 * A visitor-picked image ready to POST as an `ImageInput` part on `/messages` or
 * `/livechat/messages`. Encodes as `type` / `data` / `mime` / optional `filename`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/imageinput)
 */
internal data class OutgoingImageInput(
    /** Bare base64 payload — no `data:` prefix. */
    val dataBase64: String,
    /** MIME type of the image, for example `image/jpeg`. */
    val mimeType: String,
    /**
     * Original filename when the pick had one; `null` for a composer photo without a name, which
     * `ImageInput` allows.
     */
    val filename: String?
) {

    /** Names only the type and size, so logging never prints the visitor's image bytes. */
    override fun toString(): String =
        "OutgoingImageInput(mimeType=$mimeType, dataBase64=${dataBase64.length} chars)"

    companion object {

        /**
         * Base64 character cap matching the TypeSpec `@maxLength(6990508)` on `ImageInput.data`
         * (~5 MiB decoded).
         */
        const val MAX_ENCODED_LENGTH = 6_990_508

        /** Decoded-size default for a chat-message image when no tighter cap applies (5 MiB). */
        const val MAX_DECODED_BYTES = 5_242_880

        /**
         * Builds an image part, or `null` when [data] or [mime] is blank, or [data] is longer than
         * [MAX_ENCODED_LENGTH], so a part the contract rejects is never uploaded. Whitespace in
         * [data] is removed first. A blank [filename] counts as none.
         */
        fun create(
            data: String,
            mime: String,
            filename: String?
        ): OutgoingImageInput? {
            val bare = data.filterNot(Char::isWhitespace)
            if (bare.isEmpty() || mime.isBlank() || bare.length > MAX_ENCODED_LENGTH) {
                return null
            }
            return OutgoingImageInput(
                dataBase64 = bare,
                mimeType = mime,
                filename = filename?.takeIf { it.isNotBlank() }
            )
        }
    }
}
