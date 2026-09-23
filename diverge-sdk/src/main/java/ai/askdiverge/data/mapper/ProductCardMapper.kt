package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.product.ProductCardRemote
import ai.askdiverge.data.model.message.incoming.part.product.ProductPriceRemote
import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice

internal fun ProductCardRemote.mapToDomain(): ProductCard = ProductCard(
    id = id,
    title = title,
    description = description,
    imageUrl = image_url,
    originalPrice = original_price?.mapToDomain(),
    price = price.mapToDomain(),
    url = url
)

private fun ProductPriceRemote.mapToDomain(): ProductPrice = ProductPrice(
    amount = amount,
    currency = currency
)
