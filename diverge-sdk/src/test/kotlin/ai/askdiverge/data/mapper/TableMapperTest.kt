package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListItemRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellImageBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellImageBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class TableMapperTest {

    @Test
    fun `when a cell holds text expect it mapped as a paragraph`() {
        val cell = TableCellRemote(
            blocks = listOf(
                RichTextParagraphBlockRemote(
                    spans = listOf(
                        RichTextSpanRemote(
                            text = "38",
                            type = RichTextSpanTypeRemote.TEXT
                        )
                    )
                )
            )
        ).mapToDomain()

        val paragraph = assertIs<RichTextParagraphBlock>(cell.blocks.single())
        assertEquals(
            expected = "38",
            actual = paragraph.spans.single().text
        )
    }

    @Test
    fun `when a cell holds a list expect it mapped as a bullet list`() {
        val cell = TableCellRemote(
            blocks = listOf(
                RichTextBulletListBlockRemote(
                    items = listOf(
                        RichTextBulletListItemRemote(
                            spans = listOf(
                                RichTextSpanRemote(
                                    text = "Cotton",
                                    type = RichTextSpanTypeRemote.TEXT
                                )
                            )
                        )
                    )
                )
            )
        ).mapToDomain()

        val bulletList = assertIs<RichTextBulletListBlock>(cell.blocks.single())
        assertEquals(
            expected = "Cotton",
            actual = bulletList.items.single().spans.single().text
        )
    }

    @Test
    fun `when a cell holds an image expect both sizes and the alternative text kept`() {
        val cell = TableCellRemote(
            blocks = listOf(
                TableCellImageBlockRemote(
                    url = "https://example.com/full.png",
                    thumbnail_url = "https://example.com/thumb.png",
                    alt = "A shoe"
                )
            )
        ).mapToDomain()

        val image = assertIs<TableCellImageBlock>(cell.blocks.single())
        assertEquals(
            expected = "https://example.com/full.png",
            actual = image.url
        )
        assertEquals(
            expected = "https://example.com/thumb.png",
            actual = image.thumbnailUrl
        )
        assertEquals(
            expected = "A shoe",
            actual = image.alt
        )
    }

    @Test
    fun `when a cell holds several blocks expect them stacked in order`() {
        val cell = TableCellRemote(
            blocks = listOf(
                RichTextParagraphBlockRemote(
                    spans = listOf(
                        RichTextSpanRemote(
                            text = "38",
                            type = RichTextSpanTypeRemote.TEXT
                        )
                    )
                ),
                TableCellImageBlockRemote(
                    url = "https://example.com/full.png",
                    thumbnail_url = null,
                    alt = null
                )
            )
        ).mapToDomain()

        assertEquals(
            expected = 2,
            actual = cell.blocks.size
        )
        assertIs<RichTextParagraphBlock>(cell.blocks.first())
        assertIs<TableCellImageBlock>(cell.blocks.last())
    }

    @Test
    fun `when an alignment is unknown expect it to read from the left`() {
        assertEquals(
            expected = TableColumnAlignment.LEFT,
            actual = TableColumnAlignmentRemote.UNKNOWN.mapToDomain()
        )
    }
}
