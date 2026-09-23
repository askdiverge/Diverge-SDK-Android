package ai.askdiverge.domain.model.message.incoming.part.product

internal data class ProductCard(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val originalPrice: ProductPrice?,
    val price: ProductPrice,
    val url: String
)
