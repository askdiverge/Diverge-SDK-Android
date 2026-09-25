package ai.askdiverge.ui.model.message.part.product

internal data class ProductCardUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val description: String?,
    val price: String,
    val originalPrice: String?,
    val url: String
)
