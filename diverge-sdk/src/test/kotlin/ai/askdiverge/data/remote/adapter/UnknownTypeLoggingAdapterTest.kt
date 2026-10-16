package ai.askdiverge.data.remote.adapter

import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.domain.logging.ChatbotLogger
import com.squareup.moshi.Moshi
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UnknownTypeLoggingAdapterTest {

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
    fun `message with an unknown part type keeps its known parts and logs the unknown type`() {
        val message = messageAdapter.fromJson(
            """
            {
              "message_id": "m1",
              "role": "assistant",
              "parts": [
                {"type": "carousel", "part_id": "p1", "items": [{"id": 1}]},
                {"type": "rich_text", "part_id": "p2", "blocks": [{"type": "paragraph", "spans": [{"text": "Hi", "type": "text"}]}]}
              ]
            }
            """.trimIndent()
        )

        assertEquals(expected = 1, actual = message?.parts?.size)
        assertIs<RichTextPartRemote>(message?.parts?.first())
        verify(exactly = 1) { ChatbotLogger.logError("Unknown message part type: carousel") }
    }

    @Test
    fun `rich text with an unknown block type keeps its known blocks and logs the unknown type`() {
        val message = messageAdapter.fromJson(
            """
            {
              "message_id": "m1",
              "role": "assistant",
              "parts": [
                {"type": "rich_text", "part_id": "p1", "blocks": [
                  {"type": "quote", "spans": [{"text": "Hi", "type": "text"}]},
                  {"type": "paragraph", "spans": [{"text": "Hi", "type": "text"}]}
                ]}
              ]
            }
            """.trimIndent()
        )

        assertEquals(expected = 1, actual = (message?.parts?.first() as RichTextPartRemote).blocks.size)
        verify(exactly = 1) { ChatbotLogger.logError("Unknown rich text block type: quote") }
    }
}
