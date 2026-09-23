package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.event.StreamDoneEventRemote
import ai.askdiverge.data.model.event.StreamErrorEventRemote
import ai.askdiverge.data.model.event.StreamPartEventRemote
import ai.askdiverge.data.model.event.StreamStatusEventRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.model.event.StreamErrorEvent
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.event.StreamStatusEvent
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class StreamEventMapperTest {

    @Nested
    @DisplayName("When event is STATUS")
    inner class StatusTest {

        @Test
        fun `maps to StreamStatusEvent`() {
            val response = StreamStatusEventRemote(status = "thinking")

            val result = response.mapToDomain()

            assertIs<StreamStatusEvent>(result)
            assertEquals(expected = "thinking", actual = result.status)
        }
    }

    @Nested
    @DisplayName("When event is PART")
    inner class PartTest {

        @Test
        fun `maps to StreamPartEvent`() {
            val response = StreamPartEventRemote(part = ImagePartRemote(url = "https://example.com/img.png"))

            val result = response.mapToDomain()

            assertIs<StreamPartEvent>(result)
        }

        @Test
        fun `returns null when part content is null`() {
            val response = StreamPartEventRemote(part = null)

            assertNull(response.mapToDomain())
        }
    }

    @Nested
    @DisplayName("When event is DONE")
    inner class DoneTest {

        @Test
        fun `maps to StreamDoneEvent with message`() {
            val response = StreamDoneEventRemote(
                message = MessageRemote(
                    message_id = "msg-1",
                    role = MessageRoleRemote.ASSISTANT,
                    parts = listOf(
                        RichTextPartRemote(
                            part_id = "p-1",
                            blocks = listOf(
                                RichTextParagraphBlockRemote(
                                    spans = listOf(
                                        RichTextSpanRemote(text = "hi", type = RichTextSpanTypeRemote.TEXT)
                                    )
                                )
                            )
                        )
                    )
                )
            )

            val result = response.mapToDomain()

            assertIs<StreamDoneEvent>(result)
            assertEquals(expected = "msg-1", actual = result.message.id)
            assertEquals(expected = 1, actual = result.message.parts.size)
        }
    }

    @Nested
    @DisplayName("When event is ERROR")
    inner class ErrorTest {

        @Test
        fun `maps to StreamErrorEvent`() {
            val response = StreamErrorEventRemote(
                code = "generation_failed",
                message = "Please try again.",
                retryable = true
            )

            val result = response.mapToDomain()

            assertIs<StreamErrorEvent>(result)
            assertEquals(expected = "Please try again.", actual = result.message)
            assertEquals(expected = true, actual = result.retryable)
        }
    }
}
