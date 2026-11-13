package ai.askdiverge.domain.model.message.incoming.part

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class RequestImageUploadTest {

    @Test
    fun `create rejects blank part id or non-positive size`() {
        assertNull(
            RequestImageUpload.create(
                partId = "  ",
                acceptedTypes = listOf("image/jpeg"),
                maxSizeBytes = 1
            )
        )
        assertNull(
            RequestImageUpload.create(
                partId = "part_1",
                acceptedTypes = listOf("image/jpeg"),
                maxSizeBytes = 0
            )
        )
    }

    @Test
    fun `create trims the part id`() {
        val marker = assertNotNull(
            RequestImageUpload.create(
                partId = " part_1 ",
                acceptedTypes = listOf("image/jpeg"),
                maxSizeBytes = 10
            )
        )
        assertEquals("part_1", marker.partId)
        assertEquals(10, marker.maxSizeBytes)
    }
}
