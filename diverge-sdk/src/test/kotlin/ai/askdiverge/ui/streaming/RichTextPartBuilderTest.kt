package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.sampleProductCard
import ai.askdiverge.ui.sampleSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class RichTextPartBuilderTest {

    private val builder = RichTextPartBuilder()

    @Test
    fun `when text deltas arrive expect them in the paragraph they belong to`() {
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            sampleTextAppended(
                blockIndex = 0,
                text = "Sure, "
            )
        )
        builder.handle(
            sampleTextAppended(
                blockIndex = 0,
                text = "here you go."
            )
        )

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(buildParagraphs().single())
        assertEquals(
            expected = listOf("Sure, ", "here you go."),
            actual = paragraph.spans.map { it.text }
        )
        assertEquals(
            expected = listOf(RichTextSpanTypeUiModel.TEXT, RichTextSpanTypeUiModel.TEXT),
            actual = paragraph.spans.map { it.type }
        )
    }

    @Test
    fun `when a styled span arrives expect its style and link kept`() {
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            sampleTextAppended(
                blockIndex = 0,
                text = "See "
            )
        )
        builder.handle(
            StreamDeltaEvent.SpanAppended(
                partId = "part-1",
                blockIndex = 0,
                span = RichTextSpan(
                    text = "our guide",
                    type = RichTextSpanType.LINK,
                    url = "https://example.com/guide"
                )
            )
        )

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(buildParagraphs().single())
        assertEquals(
            expected = 2,
            actual = paragraph.spans.size
        )
        assertEquals(
            expected = RichTextSpanTypeUiModel.LINK,
            actual = paragraph.spans.last().type
        )
        assertEquals(
            expected = "https://example.com/guide",
            actual = paragraph.spans.last().url
        )
    }

    @Test
    fun `when bullet list items arrive expect them in the order they arrived`() {
        builder.handle(
            sampleBlockStarted(
                blockIndex = 0,
                blockType = DeltaBlockType.BULLET_LIST
            )
        )
        builder.handle(
            StreamDeltaEvent.ItemAppended(
                partId = "part-1",
                blockIndex = 0,
                item = RichTextBulletListItem(spans = listOf(sampleSpan(text = "first")))
            )
        )
        builder.handle(
            StreamDeltaEvent.ItemAppended(
                partId = "part-1",
                blockIndex = 0,
                item = RichTextBulletListItem(spans = listOf(sampleSpan(text = "second")))
            )
        )

        val bulletList = assertIs<RichTextBlockUiModel.BulletList>(buildParagraphs().single())
        assertEquals(
            expected = listOf("first", "second"),
            actual = bulletList.items.map { item -> item.spans.single().text }
        )
    }

    @Test
    fun `when blocks arrive out of order expect them ordered by their index`() {
        builder.handle(sampleBlockStarted(blockIndex = 1))
        builder.handle(
            sampleTextAppended(
                blockIndex = 1,
                text = "second"
            )
        )
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            sampleTextAppended(
                blockIndex = 0,
                text = "first"
            )
        )

        val blocks = buildParagraphs()
        assertEquals(
            expected = 2,
            actual = blocks.size
        )
        assertEquals(
            expected = listOf("first", "second"),
            actual = blocks.map { block ->
                assertIs<RichTextBlockUiModel.Paragraph>(block).spans.single().text
            }
        )
    }

    @Test
    fun `when text is addressed to a bullet list expect it dropped`() {
        builder.handle(
            sampleBlockStarted(
                blockIndex = 0,
                blockType = DeltaBlockType.BULLET_LIST
            )
        )
        builder.handle(
            sampleTextAppended(
                blockIndex = 0,
                text = "not an item"
            )
        )

        val bulletList = assertIs<RichTextBlockUiModel.BulletList>(buildParagraphs().single())
        assertEquals(
            expected = 0,
            actual = bulletList.items.size
        )
    }

    @Test
    fun `when an item is addressed to a paragraph expect it dropped`() {
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            StreamDeltaEvent.ItemAppended(
                partId = "part-1",
                blockIndex = 0,
                item = RichTextBulletListItem(spans = listOf(sampleSpan(text = "not a paragraph")))
            )
        )

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(buildParagraphs().single())
        assertEquals(
            expected = 0,
            actual = paragraph.spans.size
        )
    }

    @Test
    fun `when a delta addresses a block that never started expect it dropped`() {
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            sampleTextAppended(
                blockIndex = 7,
                text = "orphan"
            )
        )

        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(buildParagraphs().single())
        assertEquals(
            expected = 0,
            actual = paragraph.spans.size
        )
    }

    @Test
    fun `when a delta is meant for another kind of delta part expect it ignored`() {
        builder.handle(sampleBlockStarted(blockIndex = 0))
        builder.handle(
            StreamDeltaEvent.ProductAppended(
                partId = "part-1",
                product = sampleProductCard()
            )
        )

        assertIs<RichTextBlockUiModel.Paragraph>(buildParagraphs().single())
    }

    private fun sampleBlockStarted(
        blockIndex: Int,
        blockType: DeltaBlockType = DeltaBlockType.PARAGRAPH
    ) = StreamDeltaEvent.BlockStarted(
        partId = "part-1",
        blockIndex = blockIndex,
        blockType = blockType
    )

    private fun sampleTextAppended(
        blockIndex: Int,
        text: String
    ) = StreamDeltaEvent.TextAppended(
        partId = "part-1",
        blockIndex = blockIndex,
        text = text
    )

    private fun buildParagraphs(): List<RichTextBlockUiModel> {
        val part = assertIs<MessagePartUiModel.RichText>(builder.build())
        return part.blocks
    }
}
