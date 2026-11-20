package ai.askdiverge.ui.compose.components.message.product.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel

internal class  ProductCardListPreviewProvider : PreviewParameterProvider<List<ProductCardUiModel>> {

    override val values: Sequence<List<ProductCardUiModel>>
        get() = sequenceOf(
            listOf(
                ProductCardUiModel(
                    id = "1",
                    title = "GEL-NIMBUS 28 - Løbesko",
                    imageUrl = "",
                    price = "DKK 1,700.00",
                    description = null,
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "2",
                    title = "WAVE MUJIN 11(M)",
                    imageUrl = "",
                    price = "DKK 675.00",
                    description = "Herre / Løbesko",
                    originalPrice = "DKK 1,350.00",
                    url = ""
                ),
                ProductCardUiModel(
                    id = "3",
                    title = "PATRIOT 14 - Løbesko",
                    imageUrl = "",
                    description = null,
                    price = "DKK 600.00",
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "4",
                    title = "ULTRABOOST LIGHT",
                    imageUrl = "",
                    description = "Dame / Løbesko",
                    price = "DKK 1,400.00",
                    originalPrice = null,
                    url = ""
                )
            )
        )
}
