package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.ui.model.message.part.product.ProductCardUiModel
import java.util.Locale

internal fun ProductCard.mapToUi(): ProductCardUiModel = ProductCardUiModel(
    id = id,
    title = title,
    imageUrl = imageUrl,
    price = price.mapToUi(Locale.getDefault()),
    description = description,
    originalPrice = originalPrice?.takeIf { it.amount > price.amount }?.mapToUi(Locale.getDefault()),
    url = url
)
