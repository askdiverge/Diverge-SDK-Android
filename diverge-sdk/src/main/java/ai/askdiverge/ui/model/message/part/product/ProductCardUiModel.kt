package ai.askdiverge.ui.model.message.part.product

import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice

/**
 * @property price the amount and currency as sent; the card writes it in the locale it is drawn in.
 * @property originalPrice the price struck through beside [price], present only when it is higher.
 */
internal data class ProductCardUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val description: String?,
    val price: ProductPrice,
    val originalPrice: ProductPrice?,
    val url: String
)
