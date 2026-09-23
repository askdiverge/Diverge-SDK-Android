package ai.askdiverge.domain.model.message.incoming.part.table

import ai.askdiverge.domain.model.message.incoming.part.text.TableCellBlock

/**
 * A single cell of a table, holding its content as a vertical stack of [blocks].
 */
internal data class TableCell(val blocks: List<TableCellBlock>)
