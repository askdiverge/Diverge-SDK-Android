package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellBlockUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.table.TableColumnAlignmentUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.sampleProductCard
import ai.askdiverge.ui.sampleTextCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class TablePartBuilderTest {

    private val builder = TablePartBuilder()

    @Test
    fun `when a table has no headers or rows expect nothing to be built`() {
        builder.handle(sampleTableStarted(headers = emptyList()))

        assertNull(builder.build())
    }

    @Test
    fun `when a table delta part starts expect its caption and headers`() {
        builder.handle(sampleTableStarted())

        val table = assertIs<MessagePartUiModel.Table>(builder.build())
        assertEquals(
            expected = "Sizes",
            actual = table.caption
        )
        assertEquals(
            expected = 2,
            actual = table.headers.size
        )
        assertEquals(
            expected = 0,
            actual = table.rows.size
        )
    }

    @Test
    fun `when rows arrive expect them in the order they arrived`() {
        builder.handle(sampleTableStarted())
        builder.handle(
            StreamDeltaEvent.RowAppended(
                partId = "part-1",
                row = listOf(sampleTextCell("38"), sampleTextCell("In stock"))
            )
        )
        builder.handle(
            StreamDeltaEvent.RowAppended(
                partId = "part-1",
                row = listOf(sampleTextCell("39"), sampleTextCell("Sold out"))
            )
        )

        val table = assertIs<MessagePartUiModel.Table>(builder.build())
        assertEquals(
            expected = listOf("38", "39"),
            actual = table.rows.map { row -> readText(row.first()) }
        )
    }

    @Test
    fun `when the backend sends alignments expect each column aligned that way`() {
        builder.handle(
            sampleTableStarted(
                alignments = listOf(TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT)
            )
        )
        builder.handle(
            StreamDeltaEvent.RowAppended(
                partId = "part-1",
                row = listOf(sampleTextCell("38"), sampleTextCell("In stock"))
            )
        )

        val table = assertIs<MessagePartUiModel.Table>(builder.build())
        assertEquals(
            expected = listOf(TableColumnAlignmentUiModel.CENTER, TableColumnAlignmentUiModel.RIGHT),
            actual = table.headers.map { it.alignment }
        )
        assertEquals(
            expected = listOf(TableColumnAlignmentUiModel.CENTER, TableColumnAlignmentUiModel.RIGHT),
            actual = table.rows.single().map { it.alignment }
        )
    }

    @Test
    fun `when a delta is meant for another kind of delta part expect it ignored`() {
        builder.handle(
            StreamDeltaEvent.ProductAppended(
                partId = "part-1",
                product = sampleProductCard()
            )
        )

        assertNull(builder.build())
    }

    private fun readText(cell: TableCellUiModel): String {
        val text = assertIs<TableCellBlockUiModel.Text>(cell.blocks.single())
        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(text.content.blocks.single())
        return paragraph.spans.single().text
    }

    private fun sampleTableStarted(
        caption: String? = "Sizes",
        headers: List<TableCell> = listOf(
            sampleTextCell("Size"),
            sampleTextCell("Stock")
        ),
        alignments: List<TableColumnAlignment> = emptyList()
    ) = StreamDeltaEvent.TablePartStarted(
        partId = "part-1",
        partType = DeltaPartType.TABLE,
        caption = caption,
        headers = headers,
        alignments = alignments
    )
}
