package ai.askdiverge.domain.model.message.incoming.part.text

internal data class RichTextBulletListBlock(val items: List<RichTextBulletListItem>) :
    RichTextBlock,
    TableCellBlock
