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
        val part = makeSUT(acceptedTypes = listOf("image/png", "image/jpeg"))

        val ui = assertIs<MessagePartUiModel.RequestImageUpload>(
            part.mapToUi(offersAttachments = true)
        )
        assertEquals("part_1", ui.partId)
        assertEquals(1_000, ui.maxSizeBytes)
    }

    @Test
    fun `a marker is dropped when the host disables attachments`() {
        val part = makeSUT(acceptedTypes = listOf("image/jpeg"))

        assertNull(part.mapToUi(offersAttachments = false))
    }

    @Test
    fun `a marker that does not accept jpeg is dropped`() {
        val part = makeSUT(acceptedTypes = listOf("image/png"))

        assertNull(part.mapToUi(offersAttachments = true))
    }

    private fun makeSUT(acceptedTypes: List<String>) = RequestImageUpload(
        partId = "part_1",
        acceptedTypes = acceptedTypes,
        maxSizeBytes = 1_000
    )
}
