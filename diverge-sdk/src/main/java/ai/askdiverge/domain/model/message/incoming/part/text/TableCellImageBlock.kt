package ai.askdiverge.domain.model.message.incoming.part.text

/**
 * An image inside a table cell.
 *
 * @property url the full size image.
 * @property thumbnailUrl a smaller variant of [url], when the backend provides one.
 * @property alt the alternative text describing the image.
 */
internal data class TableCellImageBlock(
    val url: String,
    val thumbnailUrl: String?,
    val alt: String?
) : TableCellBlock
