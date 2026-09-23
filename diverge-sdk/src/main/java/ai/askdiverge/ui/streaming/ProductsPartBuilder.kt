package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.message.incoming.part.product.ProductCard
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal class ProductsPartBuilder : PartBuilder {

    private val products = mutableListOf<ProductCard>()

    override fun handle(delta: StreamDeltaEvent) {
        when (delta) {
            is StreamDeltaEvent.ProductAppended -> products.add(delta.product)
            else -> Unit
        }
    }

    override fun build(): MessagePartUiModel? {
        if (products.isEmpty()) return null
        return MessagePartUiModel.Products(products = products.map { it.mapToUi() })
    }
}
