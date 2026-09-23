package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.ProductPartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.TablePartRemote
import ai.askdiverge.data.model.message.incoming.part.product.ProductCardRemote
import ai.askdiverge.data.model.message.incoming.part.product.ProductPriceRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListItemRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellImageBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.domain.model.message.incoming.part.ProductPart
import ai.askdiverge.domain.model.message.incoming.part.RichTextPart
import ai.askdiverge.domain.model.message.incoming.part.TablePart
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellImageBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.collections.get

class MessagePartMapperTest {

    @Nested
    @DisplayName("When mapping RichTextPartRemote")
    inner class RichTextTest {

        @Test
        fun `maps to RichTextPart with correct id and blocks`() {
            val remote = RichTextPartRemote(
                part_id = "rt-1",
                blocks = listOf(
                    RichTextParagraphBlockRemote(
                        spans = listOf(RichTextSpanRemote(text = "hello", type = RichTextSpanTypeRemote.TEXT))
                    )
                )
            )

            val result = remote.mapToDomain()

            assertIs<RichTextPart>(result)
            assertEquals("rt-1", result.id)
            assertEquals(1, result.blocks.size)
        }
    }

    @Nested
    @DisplayName("When mapping ImagePartRemote")
    inner class ImageTest {

        @Test
        fun `maps to ImagePart with correct url`() {
            val remote = ImagePartRemote(url = "https://example.com/image.png")

            val result = remote.mapToDomain()

            assertIs<ImagePart>(result)
            assertEquals("https://example.com/image.png", result.url)
        }
    }

    @Nested
    @DisplayName("When mapping ProductPartRemote")
    inner class ProductTest {

        @Test
        fun `maps to ProductPart with correct id and products`() {
            val remote = ProductPartRemote(
                part_id = "prod-1",
                products = listOf(
                    ProductCardRemote(
                        id = "card-1",
                        title = "Shirt",
                        description = "A nice shirt",
                        image_url = "https://example.com/shirt.jpg",
                        original_price = ProductPriceRemote(amount = 100f, currency = "SEK"),
                        price = ProductPriceRemote(amount = 80f, currency = "SEK"),
                        url = "https://example.com/shirt"
                    )
                )
            )

            val result = remote.mapToDomain()

            assertIs<ProductPart>(result)
            assertEquals("prod-1", result.id)
            assertEquals(1, result.products.size)
        }
    }

    @Nested
    @DisplayName("When mapping TablePartRemote")
    inner class TableTest {

        @Test
        fun `maps a cell holding each of the three block types the contract allows`() {
            val remote = TablePartRemote(
                part_id = "table-1",
                caption = "Delivery options",
                headers = listOf(
                    TableCellRemote(
                        blocks = listOf(
                            TableCellImageBlockRemote(
                                url = "https://example.com/shoe.jpg",
                                thumbnail_url = "https://example.com/shoe-small.jpg",
                                alt = "Dunk Low Metro"
                            )
                        )
                    )
                ),
                alignments = listOf(TableColumnAlignmentRemote.CENTER),
                rows = listOf(
                    listOf(
                        TableCellRemote(
                            blocks = listOf(
                                RichTextParagraphBlockRemote(
                                    spans = listOf(RichTextSpanRemote(text = "Fast", type = RichTextSpanTypeRemote.BOLD))
                                ),
                                RichTextBulletListBlockRemote(
                                    items = listOf(
                                        RichTextBulletListItemRemote(
                                            spans = listOf(
                                                RichTextSpanRemote(text = "2-4 days", type = RichTextSpanTypeRemote.TEXT)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            val result = remote.mapToDomain()

            assertIs<TablePart>(result)
            assertEquals(expected = "table-1", actual = result.id)
            assertEquals(expected = "Delivery options", actual = result.caption)
            assertEquals(expected = listOf(TableColumnAlignment.CENTER), actual = result.alignments)

            val image = result.headers.single().blocks.single()
            assertIs<TableCellImageBlock>(image)
            assertEquals(expected = "https://example.com/shoe-small.jpg", actual = image.thumbnailUrl)
            assertEquals(expected = "Dunk Low Metro", actual = image.alt)

            val cellBlocks = result.rows.single().single().blocks
            val paragraph = cellBlocks[0]
            assertIs<RichTextParagraphBlock>(paragraph)
            assertEquals(expected = "Fast", actual = paragraph.spans.single().text)
            assertIs<RichTextBulletListBlock>(cellBlocks[1])
        }

        @Test
        fun `leaves alignments empty when the backend omits them`() {
            val remote = TablePartRemote(
                part_id = "table-2",
                caption = null,
                headers = emptyList(),
                alignments = null,
                rows = emptyList()
            )

            val result = remote.mapToDomain()

            assertIs<TablePart>(result)
            assertTrue(result.alignments.isEmpty())
        }

        @Test
        fun `falls back to left for an alignment the app does not support`() {
            val remote = TablePartRemote(
                part_id = "table-3",
                caption = null,
                headers = emptyList(),
                alignments = listOf(TableColumnAlignmentRemote.UNKNOWN),
                rows = emptyList()
            )

            val result = remote.mapToDomain()

            assertIs<TablePart>(result)
            assertEquals(expected = listOf(TableColumnAlignment.LEFT), actual = result.alignments)
        }
    }
}
