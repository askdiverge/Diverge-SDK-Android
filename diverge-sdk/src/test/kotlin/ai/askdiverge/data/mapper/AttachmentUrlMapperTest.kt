package ai.askdiverge.data.mapper

import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.junit.jupiter.api.DisplayName

@DisplayName("Attachment URL mapping")
internal class AttachmentUrlMapperTest {

    @Test
    fun `a bare file path resolves against the API root`() {
        val file = makeFile(url = "/api/v1/files/a").withAbsoluteAttachmentUrls(BASE_URL)

        assertEquals("https://api.example/api/v1/files/a", assertIs<MessageFile>(file).url)
    }

    @Test
    fun `an image and its thumbnail resolve against the API root`() {
        val image = ImagePart(
            url = "v1/images/full.jpg",
            attachmentId = null,
            mimeType = null,
            sizeBytes = null,
            urlExpiresAt = null,
            thumbnailUrl = "/api/v1/images/thumb.jpg",
            caption = null
        ).withAbsoluteAttachmentUrls(BASE_URL)

        val resolved = assertIs<ImagePart>(image)
        assertEquals("https://api.example/api/v1/images/full.jpg", resolved.url)
        assertEquals("https://api.example/api/v1/images/thumb.jpg", resolved.thumbnailUrl)
    }

    @Test
    fun `absolute and data links are kept`() {
        val absolute = makeFile(url = "https://cdn.example/a.pdf").withAbsoluteAttachmentUrls(BASE_URL)
        val data = makeFile(url = "data:application/pdf;base64,abc").withAbsoluteAttachmentUrls(BASE_URL)

        assertEquals("https://cdn.example/a.pdf", assertIs<MessageFile>(absolute).url)
        assertEquals("data:application/pdf;base64,abc", assertIs<MessageFile>(data).url)
    }

    @Test
    fun `a blank thumbnail is dropped and a blank link kept as sent`() {
        val image = ImagePart(
            url = " ",
            attachmentId = null,
            mimeType = null,
            sizeBytes = null,
            urlExpiresAt = null,
            thumbnailUrl = " ",
            caption = null
        ).withAbsoluteAttachmentUrls(BASE_URL)

        val resolved = assertIs<ImagePart>(image)
        assertEquals(" ", resolved.url)
        assertNull(resolved.thumbnailUrl)
    }

    @Test
    fun `every livechat message in a sync is resolved`() {
        val sync = LivechatSync(
            state = LivechatState.of(LivechatState.Status.ACTIVE),
            messages = listOf(
                LivechatMessage(
                    messageId = "m1",
                    role = MessageRole.AGENT,
                    agent = null,
                    parts = listOf(makeFile(url = "/api/v1/files/a")),
                    createdAt = "2026-01-01T00:00:00Z",
                    sequenceNumber = 1
                )
            ),
            lastSequenceNumber = 1,
            hasMore = false,
            syncCursor = null
        ).withAbsoluteAttachmentUrls(BASE_URL)

        val file = assertIs<MessageFile>(sync.messages.single().parts.single())
        assertEquals("https://api.example/api/v1/files/a", file.url)
    }

    @Test
    fun `a relative agent photo resolves against the API root`() {
        val message = Message(
            id = "m1",
            role = MessageRole.AGENT,
            agent = LivechatAgent(agentId = "a1", displayName = "Alex", avatarUrl = "/avatars/alex.png"),
            parts = emptyList()
        ).withAbsoluteAttachmentUrls(BASE_URL)

        assertEquals("https://api.example/avatars/alex.png", message.agent?.avatarUrl)
    }

    @Test
    fun `an agent photo that cannot be resolved is dropped`() {
        val message = LivechatMessage(
            messageId = "m1",
            role = MessageRole.AGENT,
            agent = LivechatAgent(agentId = "a1", displayName = "Alex", avatarUrl = " "),
            parts = emptyList(),
            createdAt = "2026-01-01T00:00:00Z",
            sequenceNumber = 1
        ).withAbsoluteAttachmentUrls(BASE_URL)

        assertNull(message.agent?.avatarUrl)
    }

    private fun makeFile(url: String) = MessageFile(
        filename = "a.pdf",
        url = url,
        attachmentId = null,
        mimeType = null,
        sizeBytes = null,
        urlExpiresAt = null
    )

    private companion object {
        const val BASE_URL = "https://api.example/api/"
    }
}
