package ai.askdiverge.domain.model.form

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class OutgoingAttachmentTest {

    @Nested
    @DisplayName("OutgoingAttachment.create")
    inner class Create {

        @Test
        fun `an attachment without a filename is named after its MIME type`() {
            val pdf = assertNotNull(makeSUT(mime = "application/pdf", filename = null))
            val unknown = assertNotNull(makeSUT(mime = "application/x-unknown", filename = null))

            assertEquals("attachment.pdf", pdf.filename)
            assertEquals("attachment", unknown.filename)
        }

        @Test
        fun `an attachment the Attachment contract rejects is never built, and a blank filename counts as none`() {
            assertNull(makeSUT(data = "", mime = "image/jpeg", filename = "a.jpg"))
            assertNull(makeSUT(data = "  ", mime = "image/jpeg", filename = "a.jpg"))
            assertNull(makeSUT(mime = "", filename = "a.jpg"))
            assertNull(makeSUT(mime = " ", filename = "a.jpg"))
            val unnamed = assertNotNull(makeSUT(mime = "application/pdf", filename = ""))
            val blankNamed = assertNotNull(makeSUT(mime = "application/pdf", filename = "   "))

            assertEquals("attachment.pdf", unnamed.filename)
            assertEquals("attachment.pdf", blankNamed.filename)
        }

        @Test
        fun `an attachment over the actions encoded cap is rejected whatever the caller's decoded cap`() {
            val atCap = "A".repeat(OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH)

            assertNotNull(makeSUT(data = atCap, mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = Int.MAX_VALUE))
            assertNull(makeSUT(data = atCap + "A", mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = Int.MAX_VALUE))
        }

        @Test
        fun `a custom-form file at the decoded cap is accepted once padded, and one byte more is rejected`() {
            assertNotNull(makeSUT(data = paddedBase64(bytes = 2_097_150), mime = "image/jpeg", filename = "a.jpg"))
            listOf(2_097_151, 2_097_152, 2_097_153).forEach { bytes ->
                assertNull(makeSUT(data = paddedBase64(bytes), mime = "image/jpeg", filename = "a.jpg"), "$bytes bytes")
            }
        }

        @Test
        fun `the decoded cap is the largest file whose padded base64 fits the encoded cap`() {
            val atCap = paddedBase64(OutgoingAttachment.MAX_ACTION_DECODED_BYTES)
            val overCap = paddedBase64(OutgoingAttachment.MAX_ACTION_DECODED_BYTES + 1)

            assertTrue(atCap.length <= OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH)
            assertTrue(overCap.length > OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH)
            assertNull(makeSUT(data = overCap, mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = Int.MAX_VALUE))
        }

        @Test
        fun `an attachment over the caller's decoded cap is rejected`() {
            // "QUJD" decodes to 3 bytes, "QUI=" to 2.
            assertNull(makeSUT(data = "QUJD", mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = 2))
            assertNotNull(makeSUT(data = "QUI=", mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = 2))
        }

        @Test
        fun `line breaks in the base64 are removed before the size checks`() {
            val wrapped = assertNotNull(
                makeSUT(data = "QU\nI=\n", mime = "image/jpeg", filename = "a.jpg", maxDecodedBytes = 2)
            )

            assertEquals("QUI=", wrapped.dataBase64)
        }
    }

    @Test
    fun `an attachment's text names neither its content nor its filename`() {
        val attachment = assertNotNull(makeSUT(data = "QUJD", mime = "image/jpeg", filename = "passport.jpg"))

        assertEquals("OutgoingAttachment(mimeType=image/jpeg, dataBase64=4 chars)", attachment.toString())
    }

    private fun makeSUT(
        data: String = "QQ==",
        mime: String,
        filename: String?,
        maxDecodedBytes: Int = OutgoingAttachment.MAX_ACTION_DECODED_BYTES
    ): OutgoingAttachment? = OutgoingAttachment.create(
        data = data,
        mime = mime,
        filename = filename,
        maxDecodedBytes = maxDecodedBytes
    )

    private fun paddedBase64(bytes: Int): String = Base64.getEncoder().encodeToString(ByteArray(bytes))
}
