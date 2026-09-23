package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.MessagePart
import ai.askdiverge.domain.model.message.incoming.part.ProductPart
import ai.askdiverge.domain.model.message.incoming.part.RichTextPart
import ai.askdiverge.domain.model.message.incoming.part.TablePart
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListItem
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellImageBlock
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpan
import ai.askdiverge.domain.model.message.incoming.part.text.span.RichTextSpanType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellBlockUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.table.TableColumnAlignmentUiModel
import ai.askdiverge.ui.model.message.part.text.BulletListItemUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel

internal fun MessagePart.mapToUi(): MessagePartUiModel? = when (this) {
    is RichTextPart -> MessagePartUiModel.RichText(
        blocks = blocks.mapNotNull { it.mapToUi() }
    )

    is ProductPart -> MessagePartUiModel.Products(
        products = this.products.map { it.mapToUi() }
    )

    is TablePart -> MessagePartUiModel.Table(
        caption = caption,
        headers = headers.mapIndexed { column, cell ->
            cell.mapToUi(alignment = alignments.alignmentAt(column = column))
        },
        rows = rows.map { cells ->
            cells.mapIndexed { column, cell ->
                cell.mapToUi(alignment = alignments.alignmentAt(column = column))
            }
        }
    )

    else -> null
}

private fun RichTextBlock.mapToUi(): RichTextBlockUiModel? = when (this) {
    is RichTextParagraphBlock -> RichTextBlockUiModel.Paragraph(spans = spans.map { it.mapToUi() })
    is RichTextBulletListBlock -> RichTextBlockUiModel.BulletList(items = items.map { it.mapToUi() })
    else -> null
}

private fun RichTextSpan.mapToUi() = RichTextSpanUiModel(
    text = text,
    type = type.mapToUi(),
    url = url
)

private fun RichTextSpanType.mapToUi() = when (this) {
    RichTextSpanType.TEXT -> RichTextSpanTypeUiModel.TEXT
    RichTextSpanType.BOLD -> RichTextSpanTypeUiModel.BOLD
    RichTextSpanType.STRIKE -> RichTextSpanTypeUiModel.STRIKE
    RichTextSpanType.LINK -> RichTextSpanTypeUiModel.LINK
}

private fun RichTextBulletListItem.mapToUi() = BulletListItemUiModel(spans = spans.map { it.mapToUi() })

private fun TableCell.mapToUi(alignment: TableColumnAlignmentUiModel) = TableCellUiModel(
    blocks = blocks.mapNotNull { it.mapToCellUi() },
    alignment = alignment
)

/**
 * The backend sends one alignment per column, and may send fewer than there are columns — or none at
 * all — so a column without an explicit alignment reads as [TableColumnAlignmentUiModel.LEFT].
 */
private fun List<TableColumnAlignment>.alignmentAt(column: Int): TableColumnAlignmentUiModel =
    getOrNull(column)?.mapToUi() ?: TableColumnAlignmentUiModel.LEFT

/**
 * Named apart from [RichTextBlock.mapToUi] on purpose: the paragraph and bullet list classes
 * implement both block interfaces, so two same-named extensions would make every call on those
 * concrete types ambiguous.
 */
private fun TableCellBlock.mapToCellUi(): TableCellBlockUiModel? = when (this) {
    is TableCellImageBlock -> TableCellBlockUiModel.Image(
        url = thumbnailUrl ?: url,
        alt = alt
    )

    is RichTextBlock -> mapToUi()?.let { block ->
        TableCellBlockUiModel.Text(
            content = MessagePartUiModel.RichText(blocks = listOf(block))
        )
    }
}

private fun TableColumnAlignment.mapToUi() = when (this) {
    TableColumnAlignment.LEFT -> TableColumnAlignmentUiModel.LEFT
    TableColumnAlignment.CENTER -> TableColumnAlignmentUiModel.CENTER
    TableColumnAlignment.RIGHT -> TableColumnAlignmentUiModel.RIGHT
}
