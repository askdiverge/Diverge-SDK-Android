package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.sampleMessage
import ai.askdiverge.ui.sampleParagraph
import ai.askdiverge.ui.sampleProductPart
import ai.askdiverge.ui.sampleRichTextPart
import ai.askdiverge.ui.sampleRichTextUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MessageUiMapperTest {

    @Test
    fun `when the message is from the user expect it shown as the user's own`() {
        val bubbles = sampleMessage(role = MessageRole.USER).mapToUi()

        assertEquals(
            expected = MessageRoleUiModel.USER,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the message is from the assistant expect it shown as the bot`() {
        val bubbles = sampleMessage(role = MessageRole.ASSISTANT).mapToUi()

        assertEquals(
            expected = MessageRoleUiModel.BOT,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the message is from a human agent expect it shown as the bot`() {
        val bubbles = sampleMessage(role = MessageRole.AGENT).mapToUi()

        assertEquals(
            expected = MessageRoleUiModel.BOT,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the sender is unknown expect the message not shown at all`() {
        val bubbles = sampleMessage(role = MessageRole.UNKNOWN).mapToUi()

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when a message has several parts expect a bubble each, newest first`() {
        val bubbles = sampleMessage(
            parts = listOf(
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go."))),
                sampleProductPart()
            )
        ).mapToUi()

        // The conversation is rendered bottom-up, so the last part of the message comes first.
        assertEquals(
            expected = 2,
            actual = bubbles.size
        )
        assertEquals(
            expected = sampleRichTextUiModel("Here you go."),
            actual = bubbles.last().part
        )
    }

    @Test
    fun `when a part cannot be rendered expect it left out`() {
        val bubbles = sampleMessage(
            parts = listOf(
                ImagePart(url = "https://example.com/image.png"),
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go.")))
            )
        ).mapToUi()

        assertEquals(
            expected = listOf(sampleRichTextUiModel("Here you go.")),
            actual = bubbles.map { it.part }
        )
    }

    @Test
    fun `when a message has nothing to render expect no bubbles`() {
        val bubbles = sampleMessage(parts = emptyList()).mapToUi()

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when the user types a message expect it shown as a single paragraph`() {
        val bubble = "hello".toUserMessageItem()

        assertEquals(
            expected = MessageRoleUiModel.USER,
            actual = bubble.role
        )
        assertEquals(
            expected = sampleRichTextUiModel("hello"),
            actual = bubble.part
        )
    }
}
