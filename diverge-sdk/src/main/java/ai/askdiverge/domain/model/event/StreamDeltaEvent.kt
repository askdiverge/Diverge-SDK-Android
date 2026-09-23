package ai.askdiverge.domain.model.event

import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan

internal sealed class StreamDeltaEvent : StreamEvent {

    abstract val partId: String

    data class PartStarted(
        override val partId: String,
        val partType: DeltaPartType
    ) : StreamDeltaEvent()

    data class TablePartStarted(
        override val partId: String,
        val partType: DeltaPartType,
        val caption: String?,
        val headers: List<TableCell>,
        val alignments: List<TableColumnAlignment>
    ) : StreamDeltaEvent()

    data class PartEnded(override val partId: String) : StreamDeltaEvent()

    data class BlockStarted(
        override val partId: String,
        val blockIndex: Int,
        val blockType: DeltaBlockType
    ) : StreamDeltaEvent()

    data class BlockEnded(override val partId: String) : StreamDeltaEvent()

    data class TextAppended(
        override val partId: String,
        val blockIndex: Int,
        val text: String
    ) : StreamDeltaEvent()

    data class SpanAppended(
        override val partId: String,
        val blockIndex: Int,
        val span: RichTextSpan
    ) : StreamDeltaEvent()

    data class ItemAppended(
        override val partId: String,
        val blockIndex: Int,
        val item: RichTextBulletListItem
    ) : StreamDeltaEvent()

    data class ProductAppended(
        override val partId: String,
        val product: ProductCard
    ) : StreamDeltaEvent()

    data class RowAppended(
        override val partId: String,
        val row: List<TableCell>
    ) : StreamDeltaEvent()
}
