package ai.askdiverge.ui.model.message.part.table

import androidx.compose.runtime.Immutable
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

@Immutable
internal sealed class TableCellBlockUiModel {
    data class Text(val content: MessagePartUiModel.RichText) : TableCellBlockUiModel()

    data class Image(
        val url: String,
        val alt: String?
    ) : TableCellBlockUiModel()
}
