package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.RequestImageUpload
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class RequestImageUploadUiMapperTest {

    @Test
    fun `a marker maps when the host offers attachments`() {
        val part = RequestImageUpload(
            partId = "part_1",
            acceptedTypes = listOf("image/jpeg"),
            maxSizeBytes = 1_000
        )

        val ui = assertIs<MessagePartUiModel.RequestImageUpload>(
            part.mapToUi(offersAttachments = true)
        )
        assertEquals("part_1", ui.partId)
        assertEquals(listOf("image/jpeg"), ui.acceptedTypes)
        assertEquals(1_000, ui.maxSizeBytes)
    }

    @Test
    fun `a marker is dropped when the host disables attachments`() {
        val part = RequestImageUpload(
            partId = "part_1",
            acceptedTypes = listOf("image/jpeg"),
            maxSizeBytes = 1_000
        )

        assertNull(part.mapToUi(offersAttachments = false))
    }
}
