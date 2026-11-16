package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.ui.model.SenderAvatar
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.sampleLivechatAgent
import ai.askdiverge.ui.sampleMessage
import ai.askdiverge.ui.sampleParagraph
import ai.askdiverge.ui.sampleProductPart
import ai.askdiverge.ui.sampleRichTextPart
import ai.askdiverge.ui.sampleRichTextUiModel
import ai.askdiverge.ui.sampleSuggestionsPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class MessageUiMapperTest {

    @Test
    fun `when the message is from the user expect it shown as the user's own`() {
        val bubbles = sampleMessage(role = MessageRole.USER).mapToUi(offersAttachments = false)

        assertEquals(
            expected = MessageRoleUiModel.USER,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the message is from the assistant expect it shown as the bot`() {
        val bubbles = sampleMessage(role = MessageRole.ASSISTANT).mapToUi(offersAttachments = false)

        assertEquals(
            expected = MessageRoleUiModel.BOT,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the message is from a human agent expect it shown as the bot`() {
        val bubbles = sampleMessage(role = MessageRole.AGENT).mapToUi(offersAttachments = false)

        assertEquals(
            expected = MessageRoleUiModel.BOT,
            actual = bubbles.single().role
        )
    }

    @Test
    fun `when the message is from the assistant expect the session avatar`() {
        val bubbles = sampleMessage(role = MessageRole.ASSISTANT).mapToUi(offersAttachments = false)

        assertEquals(SenderAvatar.Session, bubbles.single().senderAvatar)
    }

    @Test
    fun `when the message is from an agent expect their resolved avatar`() {
        val bubbles = sampleMessage(
            role = MessageRole.AGENT,
            agent = sampleLivechatAgent(
                displayName = "Alex",
                avatarUrl = "https://cdn.example.com/alex.png"
            )
        ).mapToUi(offersAttachments = false)

        val avatar = assertIs<SenderAvatar.Agent>(bubbles.single().senderAvatar)
        assertEquals("https://cdn.example.com/alex.png", avatar.imageUrl)
        assertEquals("Alex", avatar.displayName)
    }

    @Test
    fun `when an agent has no avatar URL expect Agent with a null image`() {
        val bubbles = sampleMessage(
            role = MessageRole.AGENT,
            agent = sampleLivechatAgent(avatarUrl = null)
        ).mapToUi(offersAttachments = false)

        val avatar = assertIs<SenderAvatar.Agent>(bubbles.single().senderAvatar)
        assertNull(avatar.imageUrl)
    }

    @Test
    fun `when the sender is unknown expect the message not shown at all`() {
        val bubbles = sampleMessage(role = MessageRole.UNKNOWN).mapToUi(offersAttachments = false)

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when a message has several parts expect a bubble each, newest first`() {
        val bubbles = sampleMessage(
            parts = listOf(
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go."))),
                sampleProductPart()
            )
        ).mapToUi(offersAttachments = false)

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
    fun `when a message has an image part expect it rendered`() {
        val bubbles = sampleMessage(
            parts = listOf(
                ImagePart(
                    url = "https://example.com/image.png",
                    attachmentId = null,
                    mimeType = null,
                    sizeBytes = null,
                    urlExpiresAt = null,
                    thumbnailUrl = null,
                    caption = null
                ),
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go.")))
            )
        ).mapToUi(offersAttachments = false)

        assertEquals(expected = 2, actual = bubbles.size)
        // Last part of the message is first in the bottom-up list.
        assertEquals(
            expected = sampleRichTextUiModel("Here you go."),
            actual = bubbles.first().part
        )
        assertIs<MessagePartUiModel.Image>(bubbles.last().part)
    }

    @Test
    fun `when a part cannot be resolved expect it left out`() {
        val bubbles = sampleMessage(
            parts = listOf(
                ImagePart(
                    url = " ",
                    attachmentId = null,
                    mimeType = null,
                    sizeBytes = null,
                    urlExpiresAt = null,
                    thumbnailUrl = null,
                    caption = null
                ),
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go.")))
            )
        ).mapToUi(offersAttachments = false)

        assertEquals(
            expected = listOf(sampleRichTextUiModel("Here you go.")),
            actual = bubbles.map { it.part }
        )
    }

    @Test
    fun `when a rich text part has no blocks expect no bubble for it`() {
        val bubbles = sampleMessage(
            parts = listOf(sampleRichTextPart(blocks = emptyList()))
        ).mapToUi(offersAttachments = false)

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when a rich text part without blocks is left out expect the other bubble to keep its id`() {
        val bubbles = sampleMessage(
            id = "message-id",
            parts = listOf(
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go."))),
                sampleRichTextPart(blocks = emptyList())
            )
        ).mapToUi(offersAttachments = false)

        assertEquals(
            expected = listOf(sampleRichTextUiModel("Here you go.")),
            actual = bubbles.map { it.part }
        )
        // Ids count every part from the end of the message, so the skipped last part still holds 0.
        assertEquals(
            expected = listOf("message-id-1"),
            actual = bubbles.map { it.id }
        )
    }

    @Test
    fun `when a message has nothing to render expect no bubbles`() {
        val bubbles = sampleMessage(parts = emptyList()).mapToUi(offersAttachments = false)

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when a message holds only a suggestions part with no cards expect no bubbles`() {
        val bubbles = sampleMessage(
            parts = listOf(sampleSuggestionsPart(suggestions = emptyList()))
        ).mapToUi(offersAttachments = false)

        assertTrue(bubbles.isEmpty())
    }

    @Test
    fun `when a suggestions part with no cards follows another part expect that part's bubble to keep its id`() {
        val bubbles = sampleMessage(
            id = "message-1",
            parts = listOf(
                sampleRichTextPart(blocks = listOf(sampleParagraph("Here you go."))),
                sampleSuggestionsPart(suggestions = emptyList())
            )
        ).mapToUi(offersAttachments = false)

        // The empty part is last in the message, so it holds position 0 of the bottom-up list.
        val bubble = bubbles.single()
        assertEquals(
            expected = sampleRichTextUiModel("Here you go."),
            actual = bubble.part
        )
        assertEquals(
            expected = "message-1-1",
            actual = bubble.id
        )
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

    @Test
    fun `when the user sends photos expect each shown from its own payload, newest first, then the text`() {
        val png = requireNotNull(OutgoingImageInput.create(data = "QQ==", mime = "image/png", filename = null))
        val jpeg = requireNotNull(OutgoingImageInput.create(data = "Qg==", mime = "image/jpeg", filename = null))

        val bubbles = outgoingUserBubbles(text = "look", images = listOf(png, jpeg))

        val images = bubbles.dropLast(1).map { assertIs<MessagePartUiModel.Image>(it.part) }
        assertEquals(
            expected = listOf("data:image/jpeg;base64,Qg==", "data:image/png;base64,QQ=="),
            actual = images.map { it.displayUrl }
        )
        assertEquals(expected = listOf("image/jpeg", "image/png"), actual = images.map { it.mimeType })
        assertEquals(expected = sampleRichTextUiModel("look"), actual = bubbles.last().part)
        assertTrue(bubbles.all { it.role == MessageRoleUiModel.USER })
    }
}
