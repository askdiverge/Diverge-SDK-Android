package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.message.incoming.part.RichTextPart
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

private const val STREAMING_PART_ID = "streaming"

internal class RichTextPartBuilder : PartBuilder {

    private val inProgressBlocks = mutableMapOf<Int, BlockBuilder>()

    override fun handle(delta: StreamDeltaEvent) {
        when (delta) {
            is StreamDeltaEvent.BlockStarted -> startBlock(delta)
            is StreamDeltaEvent.TextAppended -> appendText(delta)
            is StreamDeltaEvent.SpanAppended -> appendSpan(delta)
            is StreamDeltaEvent.ItemAppended -> appendItem(delta)
            else -> Unit
        }
    }

    override fun build(): MessagePartUiModel? {
        if (inProgressBlocks.isEmpty()) return null
        // Map insertion order is not guaranteed, so sort by index to preserve blocks order.
        val blocks = inProgressBlocks.entries
            .sortedBy { it.key }
            .map { (_, builder) ->
                when (builder) {
                    is BlockBuilder.Paragraph -> RichTextParagraphBlock(spans = builder.spans.toList())
                    is BlockBuilder.BulletList -> RichTextBulletListBlock(items = builder.items.toList())
                }
            }
        return RichTextPart(id = STREAMING_PART_ID, blocks = blocks).mapToUi()
    }

    private fun startBlock(delta: StreamDeltaEvent.BlockStarted) {
        inProgressBlocks[delta.blockIndex] = when (delta.blockType) {
            DeltaBlockType.PARAGRAPH, DeltaBlockType.UNKNOWN -> BlockBuilder.Paragraph()
            DeltaBlockType.BULLET_LIST -> BlockBuilder.BulletList()
        }
    }

    private fun appendText(delta: StreamDeltaEvent.TextAppended) {
        val builder = inProgressBlocks[delta.blockIndex] as? BlockBuilder.Paragraph ?: return
        builder.spans.add(RichTextSpan(text = delta.text, type = RichTextSpanType.TEXT, url = null))
    }

    private fun appendSpan(delta: StreamDeltaEvent.SpanAppended) {
        val builder = inProgressBlocks[delta.blockIndex] as? BlockBuilder.Paragraph ?: return
        builder.spans.add(delta.span)
    }

    private fun appendItem(delta: StreamDeltaEvent.ItemAppended) {
        val builder = inProgressBlocks[delta.blockIndex] as? BlockBuilder.BulletList ?: return
        builder.items.add(delta.item)
    }
}

private sealed class BlockBuilder {
    class Paragraph(val spans: MutableList<RichTextSpan> = mutableListOf()) : BlockBuilder()
    class BulletList(val items: MutableList<RichTextBulletListItem> = mutableListOf()) : BlockBuilder()
}
