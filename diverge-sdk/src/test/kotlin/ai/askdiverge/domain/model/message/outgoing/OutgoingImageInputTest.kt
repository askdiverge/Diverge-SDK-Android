package ai.askdiverge.domain.model.message.outgoing

import ai.askdiverge.ui.form.ImageAttachmentEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class OutgoingImageInputTest {

    @Test
    fun `create rejects blank data or mime and oversize payloads`() {
        assertNull(OutgoingImageInput.create(data = "", mime = "image/jpeg", filename = null))
        assertNull(OutgoingImageInput.create(data = "QQ==", mime = "", filename = null))
        val oversize = "A".repeat(OutgoingImageInput.MAX_ENCODED_LENGTH + 1)
        assertNull(OutgoingImageInput.create(data = oversize, mime = "image/jpeg", filename = null))
    }

    @Test
    fun `create trims whitespace and blank filename`() {
        val image = assertNotNull(
            OutgoingImageInput.create(data = " QQ== ", mime = "image/jpeg", filename = "  ")
        )
        assertEquals("QQ==", image.dataBase64)
        assertNull(image.filename)
    }

    @Test
    fun `message encoded cap keeps the ImageInput max for the livechat default`() {
        // `/config` sends `max_attachment_size_bytes` as 5 MiB.
        val livechatDefaultBytes = 5_242_880
        assertEquals(
            OutgoingImageInput.MAX_ENCODED_LENGTH,
            ImageAttachmentEncoder.messageEncodedCap(livechatDefaultBytes)
        )
        assertEquals(
            OutgoingImageInput.MAX_ENCODED_LENGTH,
            ImageAttachmentEncoder.messageEncodedCap(null)
        )
        assertEquals(4_000, ImageAttachmentEncoder.messageEncodedCap(3_000))
    }
}
