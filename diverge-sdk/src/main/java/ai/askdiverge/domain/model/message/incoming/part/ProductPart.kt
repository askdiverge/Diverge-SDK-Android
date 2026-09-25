package ai.askdiverge.domain.model.message.incoming.part

import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard

internal data class ProductPart(
    val id: String,
    val products: List<ProductCard>
) : MessagePart
