package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagePageMapperTest {

    @Test
    fun `maps hasMore`() {
        val remote = MessagePageRemote(
            has_more = true,
            messages = emptyList(),
            next_cursor = ""
        )

        assertTrue(remote.mapToDomain().hasMore)
    }

    @Test
    fun `maps nextCursor`() {
        val remote = MessagePageRemote(
            has_more = false,
            messages = emptyList(),
            next_cursor = "cursor-abc"
        )

        assertEquals("cursor-abc", remote.mapToDomain().nextCursor)
    }

    @Test
    fun `maps messages`() {
        val remote = MessagePageRemote(
            has_more = false,
            messages = listOf(
                MessageRemote(
                    message_id = "msg-1",
                    role = MessageRoleRemote.USER,
                    parts = listOf(ImagePartRemote(url = "https://example.com/img.png"))
                ),
                MessageRemote(
                    message_id = "msg-2",
                    role = MessageRoleRemote.ASSISTANT,
                    parts = emptyList()
                )
            ),
            next_cursor = ""
        )

        val result = remote.mapToDomain()

        assertEquals(2, result.messages.size)
        assertEquals("msg-1", result.messages[0].id)
        assertEquals("msg-2", result.messages[1].id)
    }
}
