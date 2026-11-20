package ai.askdiverge.ui.compose.components.message.product.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel

internal class  ProductCardListPreviewProvider : PreviewParameterProvider<List<ProductCardUiModel>> {

    override val values: Sequence<List<ProductCardUiModel>>
        get() = sequenceOf(
            listOf(
                ProductCardUiModel(
                    id = "1",
                    title = "GEL-NIMBUS 28 - Løbesko",
                    imageUrl = "",
                    price = ProductPrice(amount = 1700f, currency = "DKK"),
                    description = null,
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "2",
                    title = "WAVE MUJIN 11(M)",
                    imageUrl = "",
                    price = ProductPrice(amount = 675f, currency = "DKK"),
                    description = "Herre / Løbesko",
                    originalPrice = ProductPrice(amount = 1350f, currency = "DKK"),
                    url = ""
                ),
                ProductCardUiModel(
                    id = "3",
                    title = "PATRIOT 14 - Løbesko",
                    imageUrl = "",
                    description = null,
                    price = ProductPrice(amount = 600f, currency = "DKK"),
                    originalPrice = null,
                    url = ""
                ),
                ProductCardUiModel(
                    id = "4",
                    title = "ULTRABOOST LIGHT",
                    imageUrl = "",
                    description = "Dame / Løbesko",
                    price = ProductPrice(amount = 1400f, currency = "DKK"),
                    originalPrice = null,
                    url = ""
                )
            )
        )
}
