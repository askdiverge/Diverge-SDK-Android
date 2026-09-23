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
                    price = "1700.0 DKK",
                    description = null,
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "2",
                    title = "WAVE MUJIN 11(M)",
                    imageUrl = "",
                    price = "675.0 DKK",
                    description = "Herre / Løbesko",
                    originalPrice = "1350.0 DKK",
                    url = ""
                ),
                ProductCardUiModel(
                    id = "3",
                    title = "PATRIOT 14 - Løbesko",
                    imageUrl = "",
                    description = null,
                    price = "600.0 DKK",
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "4",
                    title = "ULTRABOOST LIGHT",
                    imageUrl = "",
                    description = "Dame / Løbesko",
                    price = "1400.0 DKK",
                    originalPrice = null,
                    url = ""
                )
            )
        )
}
