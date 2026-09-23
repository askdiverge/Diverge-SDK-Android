package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.sampleProductCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class ProductsPartBuilderTest {

    private val builder = ProductsPartBuilder()

    @Test
    fun `when several products arrive expect the order the backend sent them in`() {
        builder.handle(sampleProductAppended(id = "first"))
        builder.handle(sampleProductAppended(id = "second"))

        val products = assertIs<MessagePartUiModel.Products>(builder.build())
        assertEquals(
            expected = listOf("first", "second"),
            actual = products.products.map { it.id }
        )
    }

    @Test
    fun `when a product arrives expect its price formatted`() {
        builder.handle(sampleProductAppended(id = "product-id"))

        val products = assertIs<MessagePartUiModel.Products>(builder.build())
        assertEquals(
            expected = "249.0 DKK",
            actual = products.products.single().price
        )
    }

    @Test
    fun `when a delta is meant for another kind of delta part expect it ignored`() {
        builder.handle(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )

        assertNull(builder.build())
    }

    private fun sampleProductAppended(id: String) = StreamDeltaEvent.ProductAppended(
        partId = "part-1",
        product = sampleProductCard(
            id = id,
            price = ProductPrice(
                amount = 249f,
                currency = "DKK"
            )
        )
    )
}
