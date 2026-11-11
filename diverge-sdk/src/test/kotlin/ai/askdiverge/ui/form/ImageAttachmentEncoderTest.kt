package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.OutgoingAttachment
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ImageAttachmentEncoderTest {

    @Test
    fun `encoded length budget floors four chars per three decoded bytes and caps at action max`() {
        assertEquals(0, ImageAttachmentEncoder.encodedLengthBudget(0))
        assertEquals(4, ImageAttachmentEncoder.encodedLengthBudget(3))
        assertEquals(
            OutgoingAttachment.MAX_ACTION_ENCODED_LENGTH,
            ImageAttachmentEncoder.encodedLengthBudget(Int.MAX_VALUE)
        )
        assertEquals(
            (OutgoingAttachment.MAX_ACTION_DECODED_BYTES / 3) * 4,
            ImageAttachmentEncoder.encodedLengthBudget(OutgoingAttachment.MAX_ACTION_DECODED_BYTES)
        )
    }
}
