package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel

internal fun ProductCard.mapToUi(): ProductCardUiModel = ProductCardUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    price = price,
    description = description,
    originalPrice = originalPrice?.takeIf { it.amount > price.amount },
    url = url
)
