package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.event.StreamPartDeltaEventRemote
import ai.askdiverge.data.model.event.delta.DeltaActionRemote
import ai.askdiverge.data.model.event.delta.DeltaPartTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class StreamDeltaEventMapperTest {

    @Nested
    @DisplayName("When a table part starts")
    inner class TablePartStartedTest {

        @Test
        fun `carries the header row so the grid can render before its first body row`() {
            val event = StreamPartDeltaEventRemote(
                part_id = "table-1",
                delta = DeltaRemote(
                    action = DeltaActionRemote.START_PART,
                    part_type = DeltaPartTypeRemote.TABLE,
                    caption = "Delivery options",
                    headers = listOf(cell(text = "Carrier"), cell(text = "Price")),
                    alignments = listOf(TableColumnAlignmentRemote.LEFT, TableColumnAlignmentRemote.RIGHT)
                )
            )

            val result = event.mapToDomain()

            assertIs<StreamDeltaEvent.TablePartStarted>(result)
            assertEquals(expected = "table-1", actual = result.partId)
            assertEquals(expected = "Delivery options", actual = result.caption)
            assertEquals(expected = 2, actual = result.headers.size)
            assertEquals(
                expected = listOf(TableColumnAlignment.LEFT, TableColumnAlignment.RIGHT),
                actual = result.alignments
            )
        }

        @Test
        fun `leaves alignments empty when the backend omits them`() {
            val event = StreamPartDeltaEventRemote(
                part_id = "table-1",
                delta = DeltaRemote(
                    action = DeltaActionRemote.START_PART,
                    part_type = DeltaPartTypeRemote.TABLE,
                    headers = listOf(cell(text = "Carrier"))
                )
            )

            val result = event.mapToDomain()

            assertIs<StreamDeltaEvent.TablePartStarted>(result)
            assertTrue(result.alignments.isEmpty())
        }
    }

    @Nested
    @DisplayName("When a rich text part starts")
    inner class PartStartedTest {

        @Test
        fun `stays a plain part start`() {
            val event = StreamPartDeltaEventRemote(
                part_id = "text-1",
                delta = DeltaRemote(
                    action = DeltaActionRemote.START_PART,
                    part_type = DeltaPartTypeRemote.RICH_TEXT
                )
            )

            val result = event.mapToDomain()

            assertIs<StreamDeltaEvent.PartStarted>(result)
            assertEquals(expected = DeltaPartType.RICH_TEXT, actual = result.partType)
        }
    }

    @Nested
    @DisplayName("When a row is appended")
    inner class RowAppendedTest {

        @Test
        fun `maps the complete row`() {
            val event = StreamPartDeltaEventRemote(
                part_id = "table-1",
                delta = DeltaRemote(
                    action = DeltaActionRemote.APPEND_ROW,
                    row = listOf(cell(text = "PostNord"), cell(text = "49.00 SEK"))
                )
            )

            val result = event.mapToDomain()

            assertIs<StreamDeltaEvent.RowAppended>(result)
            assertEquals(expected = "table-1", actual = result.partId)

            val firstBlock = result.row.first().blocks.single()
            assertIs<RichTextParagraphBlock>(firstBlock)
            assertEquals(expected = "PostNord", actual = firstBlock.spans.single().text)
        }

        @Test
        fun `is dropped when the row is missing`() {
            val event = StreamPartDeltaEventRemote(
                part_id = "table-1",
                delta = DeltaRemote(action = DeltaActionRemote.APPEND_ROW)
            )

            assertNull(event.mapToDomain())
        }
    }
}

private fun cell(text: String) = TableCellRemote(
    blocks = listOf(
        RichTextParagraphBlockRemote(
            spans = listOf(RichTextSpanRemote(text = text, type = RichTextSpanTypeRemote.TEXT))
        )
    )
)
