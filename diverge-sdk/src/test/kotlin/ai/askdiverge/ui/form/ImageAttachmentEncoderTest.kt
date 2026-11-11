package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.ui.form.ImageAttachmentEncoder.UprightTransform
import androidx.exifinterface.media.ExifInterface
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

    @Test
    fun `each EXIF orientation maps to the rotation and mirror that make it upright`() {
        val expected = mapOf(
            ExifInterface.ORIENTATION_NORMAL to UprightTransform(0f, flipHorizontal = false),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to UprightTransform(0f, flipHorizontal = true),
            ExifInterface.ORIENTATION_ROTATE_180 to UprightTransform(180f, flipHorizontal = false),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to UprightTransform(180f, flipHorizontal = true),
            ExifInterface.ORIENTATION_TRANSPOSE to UprightTransform(90f, flipHorizontal = true),
            ExifInterface.ORIENTATION_ROTATE_90 to UprightTransform(90f, flipHorizontal = false),
            ExifInterface.ORIENTATION_TRANSVERSE to UprightTransform(270f, flipHorizontal = true),
            ExifInterface.ORIENTATION_ROTATE_270 to UprightTransform(270f, flipHorizontal = false),
            ExifInterface.ORIENTATION_UNDEFINED to UprightTransform(0f, flipHorizontal = false)
        )

        expected.forEach { (orientation, transform) ->
            assertEquals(transform, ImageAttachmentEncoder.uprightTransform(orientation), "orientation $orientation")
        }
    }
}
