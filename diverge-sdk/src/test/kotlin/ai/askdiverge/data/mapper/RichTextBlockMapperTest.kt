package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListItemRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanTypeRemote
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.collections.get

class RichTextBlockMapperTest {

    @Nested
    @DisplayName("When mapping RichTextParagraphBlockRemote")
    inner class ParagraphTest {

        @Test
        fun `maps to RichTextParagraphBlock with spans`() {
            val remote = RichTextParagraphBlockRemote(
                spans = listOf(
                    RichTextSpanRemote(text = "hello", type = RichTextSpanTypeRemote.TEXT),
                    RichTextSpanRemote(text = "bold", type = RichTextSpanTypeRemote.BOLD)
                )
            )

            val result = remote.mapToDomain()

            assertIs<RichTextParagraphBlock>(result)
            assertEquals(2, result.spans.size)
            assertEquals("hello", result.spans[0].text)
            assertEquals(RichTextSpanType.TEXT, result.spans[0].type)
            assertEquals("bold", result.spans[1].text)
            assertEquals(RichTextSpanType.BOLD, result.spans[1].type)
        }
    }

    @Nested
    @DisplayName("When mapping RichTextBulletListBlockRemote")
    inner class BulletListTest {

        @Test
        fun `maps to RichTextBulletListBlock with items`() {
            val remote = RichTextBulletListBlockRemote(
                items = listOf(
                    RichTextBulletListItemRemote(
                        spans = listOf(RichTextSpanRemote(text = "item 1", type = RichTextSpanTypeRemote.TEXT))
                    ),
                    RichTextBulletListItemRemote(
                        spans = listOf(RichTextSpanRemote(text = "item 2", type = RichTextSpanTypeRemote.STRIKE))
                    )
                )
            )

            val result = remote.mapToDomain()

            assertIs<RichTextBulletListBlock>(result)
            assertEquals(2, result.items.size)
            assertEquals("item 1", result.items[0].spans[0].text)
            assertEquals(RichTextSpanType.TEXT, result.items[0].spans[0].type)
            assertEquals("item 2", result.items[1].spans[0].text)
            assertEquals(RichTextSpanType.STRIKE, result.items[1].spans[0].type)
        }
    }
}
