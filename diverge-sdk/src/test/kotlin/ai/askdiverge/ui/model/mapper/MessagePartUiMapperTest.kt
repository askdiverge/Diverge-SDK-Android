package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellBlockUiModel
import ai.askdiverge.ui.model.message.part.table.TableColumnAlignmentUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.sampleBulletList
import ai.askdiverge.ui.sampleImageCell
import ai.askdiverge.ui.sampleParagraph
import ai.askdiverge.ui.sampleProductPart
import ai.askdiverge.ui.sampleRichTextPart
import ai.askdiverge.ui.sampleSpan
import ai.askdiverge.ui.sampleTablePart
import ai.askdiverge.ui.sampleTextCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class MessagePartUiMapperTest {

    @Test
    fun `when a paragraph is mapped expect its spans in order`() {
        val part = sampleRichTextPart(
            blocks = listOf(sampleParagraph("Sure, ", "here you go."))
        ).mapToUi()

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(
            assertIs<MessagePartUiModel.RichText>(part).blocks.single()
        )
        assertEquals(
            expected = listOf("Sure, ", "here you go."),
            actual = paragraph.spans.map { it.text }
        )
    }

    @Test
    fun `when a span is styled expect the matching UI style`() {
        val part = sampleRichTextPart(
            blocks = listOf(
                RichTextParagraphBlock(
                    spans = listOf(
                        sampleSpan(
                            text = "plain",
                            type = RichTextSpanType.TEXT
                        ),
                        sampleSpan(
                            text = "bold",
                            type = RichTextSpanType.BOLD
                        ),
                        sampleSpan(
                            text = "struck",
                            type = RichTextSpanType.STRIKE
                        ),
                        sampleSpan(
                            text = "link",
                            type = RichTextSpanType.LINK,
                            url = "https://example.com"
                        )
                    )
                )
            )
        ).mapToUi()

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(
            assertIs<MessagePartUiModel.RichText>(part).blocks.single()
        )
        assertEquals(
            expected = listOf(
                RichTextSpanTypeUiModel.TEXT,
                RichTextSpanTypeUiModel.BOLD,
                RichTextSpanTypeUiModel.STRIKE,
                RichTextSpanTypeUiModel.LINK
            ),
            actual = paragraph.spans.map { it.type }
        )
        assertEquals(
            expected = "https://example.com",
            actual = paragraph.spans.last().url
        )
    }

    @Test
    fun `when a bullet list is mapped expect its items kept`() {
        val part = sampleRichTextPart(blocks = listOf(sampleBulletList("first", "second"))).mapToUi()

        val bulletList = assertIs<RichTextBlockUiModel.BulletList>(
            assertIs<MessagePartUiModel.RichText>(part).blocks.single()
        )
        assertEquals(
            expected = listOf("first", "second"),
            actual = bulletList.items.map { item -> item.spans.single().text }
        )
    }

    @Test
    fun `when a product part is mapped expect product cards`() {
        val part = sampleProductPart().mapToUi()

        assertEquals(
            expected = 1,
            actual = assertIs<MessagePartUiModel.Products>(part).products.size
        )
    }

    @Test
    fun `when a table is mapped expect its caption, headers and rows kept`() {
        val part = sampleTablePart(
            caption = "Sizes",
            headers = listOf(sampleTextCell("Size"), sampleTextCell("Stock")),
            rows = listOf(
                listOf(sampleTextCell("38"), sampleTextCell("In stock")),
                listOf(sampleTextCell("39"), sampleTextCell("Sold out"))
            )
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
        assertEquals(
            expected = "Sizes",
            actual = table.caption
        )
        assertEquals(
            expected = 2,
            actual = table.headers.size
        )
        assertEquals(
            expected = 2,
            actual = table.rows.size
        )
    }

    @Test
    fun `when the backend sends alignments expect each column aligned that way`() {
        val part = sampleTablePart(
            headers = listOf(sampleTextCell("Size"), sampleTextCell("Stock")),
            alignments = listOf(TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT),
            rows = listOf(listOf(sampleTextCell("38"), sampleTextCell("In stock")))
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
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
    fun `when a column has no alignment expect it to read from the left`() {
        val part = sampleTablePart(
            headers = listOf(sampleTextCell("Size"), sampleTextCell("Stock")),
            alignments = listOf(TableColumnAlignment.RIGHT),
            rows = listOf(listOf(sampleTextCell("38"), sampleTextCell("In stock")))
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
        assertEquals(
            expected = listOf(TableColumnAlignmentUiModel.RIGHT, TableColumnAlignmentUiModel.LEFT),
            actual = table.headers.map { it.alignment }
        )
    }

    @Test
    fun `when an image cell has a thumbnail expect the thumbnail`() {
        val part = sampleTablePart(
            headers = emptyList(),
            rows = listOf(
                listOf(
                    sampleImageCell(
                        url = "https://example.com/full.png",
                        thumbnailUrl = "https://example.com/thumb.png",
                        alt = "A shoe"
                    )
                )
            )
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
        val image = assertIs<TableCellBlockUiModel.Image>(table.rows.single().single().blocks.single())
        assertEquals(
            expected = "https://example.com/thumb.png",
            actual = image.url
        )
        assertEquals(
            expected = "A shoe",
            actual = image.alt
        )
    }

    @Test
    fun `when an image cell has no thumbnail expect the full image`() {
        val part = sampleTablePart(
            headers = emptyList(),
            rows = listOf(listOf(sampleImageCell(url = "https://example.com/full.png")))
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
        val image = assertIs<TableCellBlockUiModel.Image>(table.rows.single().single().blocks.single())
        assertEquals(
            expected = "https://example.com/full.png",
            actual = image.url
        )
        assertNull(image.alt)
    }

    @Test
    fun `when a cell holds text expect it rendered as rich text`() {
        val part = sampleTablePart(
            headers = emptyList(),
            rows = listOf(listOf(sampleTextCell("38")))
        ).mapToUi()

        val table = assertIs<MessagePartUiModel.Table>(part)
        val text = assertIs<TableCellBlockUiModel.Text>(table.rows.single().single().blocks.single())
        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(text.content.blocks.single())
        assertEquals(
            expected = "38",
            actual = paragraph.spans.single().text
        )
    }
}
