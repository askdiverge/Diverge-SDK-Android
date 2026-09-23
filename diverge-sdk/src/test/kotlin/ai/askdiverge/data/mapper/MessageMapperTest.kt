package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import org.junit.Test
import kotlin.test.assertEquals

class MessageMapperTest {

    @Test
    fun `maps message id`() {
        val remote = MessageRemote(
            message_id = "msg-1",
            role = MessageRoleRemote.USER,
            parts = emptyList()
        )

        val result = remote.mapToDomain()

        assertEquals("msg-1", result.id)
    }

    @Test
    fun `maps message parts`() {
        val remote = MessageRemote(
            message_id = "msg-1",
            role = MessageRoleRemote.ASSISTANT,
            parts = listOf(
                RichTextPartRemote(
                    part_id = "p-1",
                    blocks = listOf(
                        RichTextParagraphBlockRemote(
                            spans = listOf(RichTextSpanRemote(text = "hello", type = RichTextSpanTypeRemote.TEXT))
                        )
                    )
                ),
                ImagePartRemote(url = "https://example.com/img.png")
            )
        )

        val result = remote.mapToDomain()

        assertEquals(2, result.parts.size)
    }
}
