package ai.askdiverge.ui.compose.components.message.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.message.product.previewprovider.ProductCardListPreviewProvider
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel

private const val COLUMN_COUNT = 2
private val GRID_HORIZONTAL_GAP = 6.dp
private val GRID_VERTICAL_GAP = 16.dp

@Composable
internal fun ProductsContent(
    products: List<ProductCardUiModel>,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    onUrlClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = GRID_VERTICAL_GAP)
    ) {
        products.chunked(size = COLUMN_COUNT).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = GRID_HORIZONTAL_GAP)
            ) {
                row.forEach { product ->
                    ProductCard(
                        product = product,
                        modifier = Modifier.weight(weight = 1f),
                        onUrlClick = onUrlClick
                    )
                }
                if (row.size < COLUMN_COUNT) {
                    if (isStreaming) {
                        LoadingProductCard(modifier = Modifier.weight(weight = 1f))
                    } else {
                        Spacer(modifier = Modifier.weight(weight = 1f))
                    }
                }
            }
        }

        // Trailing placeholder row shown while more products are still streaming in.
        if (isStreaming && products.size % COLUMN_COUNT == 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = GRID_HORIZONTAL_GAP)
            ) {
                LoadingProductCard(modifier = Modifier.weight(weight = 1f))
                Spacer(modifier = Modifier.weight(weight = 1f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductsContentPreview(@PreviewParameter(ProductCardListPreviewProvider::class) products: List<ProductCardUiModel>) {
    ProductsContent(
        products = products
    )
}
