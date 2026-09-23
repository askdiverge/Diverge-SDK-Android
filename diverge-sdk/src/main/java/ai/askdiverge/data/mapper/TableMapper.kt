package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.table.TableCellRemote
import ai.askdiverge.data.model.message.incoming.part.table.TableColumnAlignmentRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBulletListBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.RichTextParagraphBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellBlockRemote
import ai.askdiverge.data.model.message.incoming.part.text.TableCellImageBlockRemote
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextBulletListBlock
import ai.askdiverge.domain.model.message.incoming.part.text.RichTextParagraphBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellBlock
import ai.askdiverge.domain.model.message.incoming.part.text.TableCellImageBlock

internal fun TableCellRemote.mapToDomain(): TableCell = TableCell(
    blocks = blocks.map { it.mapToDomain() }
)

private fun TableCellBlockRemote.mapToDomain(): TableCellBlock = when (this) {
    is RichTextParagraphBlockRemote -> RichTextParagraphBlock(
        spans = spans.map { it.mapToDomain() }
    )

    is RichTextBulletListBlockRemote -> RichTextBulletListBlock(
        items = items.map { it.mapToDomain() }
    )

    is TableCellImageBlockRemote -> TableCellImageBlock(
        url = url,
        thumbnailUrl = thumbnail_url,
        alt = alt
    )
}

internal fun TableColumnAlignmentRemote.mapToDomain(): TableColumnAlignment = when (this) {
    TableColumnAlignmentRemote.CENTER -> TableColumnAlignment.CENTER
    TableColumnAlignmentRemote.RIGHT -> TableColumnAlignment.RIGHT
    TableColumnAlignmentRemote.LEFT, TableColumnAlignmentRemote.UNKNOWN -> TableColumnAlignment.LEFT
}
