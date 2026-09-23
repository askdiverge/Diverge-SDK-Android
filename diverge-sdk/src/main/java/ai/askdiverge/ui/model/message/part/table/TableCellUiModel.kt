package ai.askdiverge.ui.model.message.part.table

import androidx.compose.runtime.Immutable

@Immutable
internal data class TableCellUiModel(
    val blocks: List<TableCellBlockUiModel>,
    val alignment: TableColumnAlignmentUiModel
)
