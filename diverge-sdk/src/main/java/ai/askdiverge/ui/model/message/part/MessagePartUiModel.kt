package ai.askdiverge.ui.model.message.part

import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel
import ai.askdiverge.ui.model.message.part.table.TableCellUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import androidx.compose.runtime.Immutable

internal sealed class MessagePartUiModel {
    @Immutable
    data class RichText(val blocks: List<RichTextBlockUiModel>) : MessagePartUiModel()

    @Immutable
    data class Products(val products: List<ProductCardUiModel>) : MessagePartUiModel()

    @Immutable
    data class Table(
        val caption: String?,
        val headers: List<TableCellUiModel>,
        val rows: List<List<TableCellUiModel>>
    ) : MessagePartUiModel()
}
