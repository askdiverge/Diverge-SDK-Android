package ai.askdiverge.domain.model.form

/**
 * A visitor-picked file ready to POST as an `Attachment` on `/actions` (ticket attachments and
 * form file fields). Encodes as `filename` / `mime_type` / `data_base64`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/attachment)
 */
internal data class OutgoingAttachment(
    /** Bare base64 payload — no `data:` prefix. */
    val dataBase64: String,
    /** MIME type of the file, for example `image/jpeg` or `application/pdf`. */
    val mimeType: String,
    /** The wire filename: the picked file's name, or `attachment` with the extension [mimeType] implies. */
    val filename: String
) {

    /** Names only the type and size, so logging an attachment never prints the visitor's file or its name. */
    override fun toString(): String = "OutgoingAttachment(mimeType=$mimeType, dataBase64=${dataBase64.length} chars)"

    companion object {

        /** Decoded-size cap on an `/actions` `Attachment` (2 MiB) — the cap on custom-form files. */
        const val MAX_ACTION_DECODED_BYTES = 2_097_152

        /**
         * Base64 character cap matching the TypeSpec `@maxLength(2796203)` on
         * `Attachment.data_base64`.
         */
        const val MAX_ACTION_ENCODED_LENGTH = 2_796_203

        /**
         * Builds an attachment, or `null` when [data] or [mime] is blank, [data] is longer than
         * [MAX_ACTION_ENCODED_LENGTH], or it decodes to more than [maxDecodedBytes], so a file the
         * contract rejects is never uploaded. Whitespace in [data], such as the line breaks
         * `Base64.DEFAULT` inserts, is removed first. [maxDecodedBytes] is the ticket's
         * `max_attachment_size_bytes` for a ticket attachment and [MAX_ACTION_DECODED_BYTES] for a
         * custom-form file. A blank [filename] counts as none and is derived from [mime].
         */
        fun create(
            data: String,
            mime: String,
            filename: String?,
            maxDecodedBytes: Int
        ): OutgoingAttachment? {
            val bare = data.filterNot(Char::isWhitespace)
            if (bare.isEmpty() || mime.isBlank() || bare.length > MAX_ACTION_ENCODED_LENGTH) {
                return null
            }
            if (decodedSize(bare) > maxDecodedBytes) return null
            return OutgoingAttachment(
                dataBase64 = bare,
                mimeType = mime,
                filename = filename?.takeIf { it.isNotBlank() } ?: filenameForMime(mime)
            )
        }

        private fun decodedSize(base64: String): Long =
            base64.length * 3L / 4 - base64.takeLastWhile { it == '=' }.length

        private fun filenameForMime(mime: String): String {
            val extension = when (mime) {
                "application/pdf" -> "pdf"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                else -> null
            }
            return if (extension != null) "attachment.$extension" else "attachment"
        }
    }
}
