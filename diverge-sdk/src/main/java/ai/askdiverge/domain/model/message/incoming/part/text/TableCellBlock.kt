package ai.askdiverge.domain.model.message.incoming.part.text

/**
 * A block of content inside a table cell.
 *
 * A superset of [RichTextBlock]: a cell holds paragraphs and bullet lists like ordinary prose does,
 * and additionally images, which are only legal inside a table.
 */
internal sealed interface TableCellBlock
