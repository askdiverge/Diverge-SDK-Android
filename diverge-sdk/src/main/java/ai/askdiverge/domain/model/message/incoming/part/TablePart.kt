package ai.askdiverge.domain.model.message.incoming.part

import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.domain.model.message.incoming.part.table.TableColumnAlignment

/**
 * A table returned as one part of a bot message.
 *
 * @property id the identifier of the part within the message.
 * @property caption the text describing the table, when the backend provides one.
 * @property headers the cells of the header row.
 * @property alignments the alignment per column, empty when the backend leaves it unspecified.
 * @property rows the body rows, each a list of cells.
 */
internal data class TablePart(
    val id: String,
    val caption: String?,
    val headers: List<TableCell>,
    val alignments: List<TableColumnAlignment>,
    val rows: List<List<TableCell>>
) : MessagePart
