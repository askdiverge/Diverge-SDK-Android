package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.TablePartRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.domain.logging.ChatbotLogger
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SkipUnknownInListAdapterFactoryTest {

    private val messageAdapter = Moshi.Builder().buildConversationModels().build().adapter(MessageRemote::class.java)

    @BeforeTest
    fun setup() {
        mockkObject(ChatbotLogger)
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(ChatbotLogger)
    }

    @Test
    fun `a message part missing a required field is dropped and logged and its siblings decode`() {
        val message = decode(
            """
            {"type": "rich_text", "blocks": [{"type": "paragraph", "spans": [{"text": "Lost", "type": "text"}]}]},
            {"type": "rich_text", "part_id": "p2", "blocks": [{"type": "paragraph", "spans": [{"text": "Hi", "type": "text"}]}]}
            """
        )

        val part = assertIs<RichTextPartRemote>(message.parts.single())
        assertEquals("p2", part.part_id)
        verify(exactly = 1) { ChatbotLogger.logError(any<JsonDataException>()) }
    }

    @Test
    fun `a rich text paragraph with an unknown span type is dropped and logged and the other blocks decode`() {
        val message = decode(
            """
            {"type": "rich_text", "part_id": "p1", "blocks": [
              {"type": "paragraph", "spans": [{"text": "Hi", "type": "italic"}]},
              {"type": "paragraph", "spans": [{"text": "Kept", "type": "text"}]}
            ]}
            """
        )

        val part = assertIs<RichTextPartRemote>(message.parts.single())
        val block = assertIs<RichTextParagraphBlockRemote>(part.blocks.single())
        assertEquals("Kept", block.spans.single().text)
        verify(exactly = 1) { ChatbotLogger.logError(any<JsonDataException>()) }
    }

    @Test
    fun `a table cell block that fails to decode is dropped and logged and the other cell blocks decode`() {
        val message = decode(
            """
            {"type": "table", "part_id": "p1", "caption": null, "alignments": null,
              "headers": [{"blocks": [
                {"type": "image", "thumbnail_url": null, "alt": null},
                {"type": "paragraph", "spans": [{"text": "Name", "type": "text"}]}
              ]}],
              "rows": [[{"blocks": [
                {"type": "paragraph", "spans": [{"text": "x", "type": "italic"}]},
                {"type": "paragraph", "spans": [{"text": "Alice", "type": "text"}]}
              ]}]]
            }
            """
        )

        val table = assertIs<TablePartRemote>(message.parts.single())
        val header = assertIs<RichTextParagraphBlockRemote>(table.headers.single().blocks.single())
        val cell = assertIs<RichTextParagraphBlockRemote>(table.rows.single().single().blocks.single())
        assertEquals("Name", header.spans.single().text)
        assertEquals("Alice", cell.spans.single().text)
        verify(exactly = 2) { ChatbotLogger.logError(any<JsonDataException>()) }
    }

    private fun decode(parts: String): MessageRemote = requireNotNull(
        messageAdapter.fromJson(
            """{"message_id": "m1", "role": "assistant", "parts": [$parts]}"""
        )
    )
}
