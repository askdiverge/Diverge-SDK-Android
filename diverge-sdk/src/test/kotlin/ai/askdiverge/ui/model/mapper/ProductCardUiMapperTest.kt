package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import ai.askdiverge.ui.sampleProductCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ProductCardUiMapperTest {

    @Test
    fun `when a product is mapped expect what the card shows to be kept`() {
        val card = sampleProductCard(
            id = "product-id",
            title = "Sneakers",
            description = "Comfortable",
            imageUrl = "https://example.com/product.png",
            url = "https://example.com/product"
        ).mapToUi()

        assertEquals(
            expected = "product-id",
            actual = card.id
        )
        assertEquals(
            expected = "Sneakers",
            actual = card.title
        )
        assertEquals(
            expected = "Comfortable",
            actual = card.description
        )
        assertEquals(
            expected = "https://example.com/product.png",
            actual = card.imageUrl
        )
        assertEquals(
            expected = "https://example.com/product",
            actual = card.url
        )
    }

    @Test
    fun `when a price is mapped expect it shown with its currency`() {
        val card = sampleProductCard(
            price = ProductPrice(
                amount = 249.5f,
                currency = "DKK"
            )
        ).mapToUi()

        assertEquals(
            expected = "249.5 DKK",
            actual = card.price
        )
    }

    @Test
    fun `when a product is discounted expect the price it was struck down from`() {
        val card = sampleProductCard(
            price = ProductPrice(
                amount = 249f,
                currency = "DKK"
            ),
            originalPrice = ProductPrice(
                amount = 499f,
                currency = "DKK"
            )
        ).mapToUi()

        assertEquals(
            expected = "249.0 DKK",
            actual = card.price
        )
        assertEquals(
            expected = "499.0 DKK",
            actual = card.originalPrice
        )
    }

    @Test
    fun `when a product is at full price expect nothing struck through`() {
        val card = sampleProductCard(
            price = ProductPrice(
                amount = 249f,
                currency = "DKK"
            ),
            originalPrice = ProductPrice(
                amount = 249f,
                currency = "DKK"
            )
        ).mapToUi()

        assertNull(card.originalPrice)
    }

    @Test
    fun `when the original price is below the current one expect no discount`() {
        val card = sampleProductCard(
            price = ProductPrice(
                amount = 499f,
                currency = "DKK"
            ),
            originalPrice = ProductPrice(
                amount = 249f,
                currency = "DKK"
            )
        ).mapToUi()

        assertNull(card.originalPrice)
    }

    @Test
    fun `when there is no original price expect nothing struck through`() {
        val card = sampleProductCard(originalPrice = null).mapToUi()

        assertNull(card.originalPrice)
    }
}
