package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.sampleProductCard
import ai.askdiverge.ui.sampleRichTextUiModel
import ai.askdiverge.ui.sampleTextCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class StreamingMessageBuilderTest {

    private val builder = StreamingMessageBuilder(DefaultPartBuilderFactory())

    @Test
    fun `when the delta part type is unknown expect it skipped instead of crashing the reply`() {
        val parts = builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.UNKNOWN
            )
        )

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `when a delta arrives before any delta part started expect it ignored`() {
        val parts = builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "Hello"
            )
        )

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `when text arrives delta by delta expect the delta part built up`() {
        builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )
        builder.process(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )

        val firstDelta = builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "Sure"
            )
        )
        val secondDelta = builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = ", here you go."
            )
        )

        assertEquals(
            expected = listOf(sampleRichTextUiModel("Sure")),
            actual = firstDelta
        )
        val part = assertIs<MessagePartUiModel.RichText>(secondDelta.single())
        assertEquals(
            expected = 1,
            actual = part.blocks.size
        )
    }

    @Test
    fun `when a delta part has no delta yet expect it to contribute nothing`() {
        val parts = builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )

        assertTrue(parts.isEmpty())
    }

    @Test
    fun `when a delta part ends expect it kept until the next one starts`() {
        builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )
        builder.process(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )
        builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "Here you go."
            )
        )

        val afterEnd = builder.process(StreamDeltaEvent.PartEnded(partId = "part-1"))

        assertEquals(
            expected = listOf(sampleRichTextUiModel("Here you go.")),
            actual = afterEnd
        )
    }

    @Test
    fun `when the next delta part starts expect the previous one dropped`() {
        builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )
        builder.process(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )
        builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "Here you go."
            )
        )
        builder.process(StreamDeltaEvent.PartEnded(partId = "part-1"))

        // The real part reaches the conversation through its own event, so the pending bubble
        // starts over with the delta part that is still arriving.
        builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-2",
                partType = DeltaPartType.PRODUCTS
            )
        )
        val parts = builder.process(
            StreamDeltaEvent.ProductAppended(
                partId = "part-2",
                product = sampleProductCard()
            )
        )

        assertEquals(
            expected = 1,
            actual = parts.size
        )
        assertIs<MessagePartUiModel.Products>(parts.single())
    }

    @Test
    fun `when a table delta part starts expect it renderable from its headers`() {
        val parts = builder.process(
            StreamDeltaEvent.TablePartStarted(
                partId = "part-1",
                partType = DeltaPartType.TABLE,
                caption = "Sizes",
                headers = listOf(sampleTextCell("Size")),
                alignments = emptyList()
            )
        )

        val table = assertIs<MessagePartUiModel.Table>(parts.single())
        assertEquals(
            expected = "Sizes",
            actual = table.caption
        )
        assertEquals(
            expected = 1,
            actual = table.headers.size
        )
    }

    @Test
    fun `when the builder is reset expect every delta part it had built to be dropped`() {
        builder.process(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )
        builder.process(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )
        builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "half a rep"
            )
        )
        builder.process(StreamDeltaEvent.PartEnded(partId = "part-1"))

        builder.reset()

        val parts = builder.process(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "orphan delta"
            )
        )
        assertTrue(parts.isEmpty())
    }
}
