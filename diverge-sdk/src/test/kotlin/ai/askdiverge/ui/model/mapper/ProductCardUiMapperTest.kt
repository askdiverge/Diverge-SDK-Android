package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import ai.askdiverge.ui.sampleProductCard
import java.util.Locale
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
    fun `when a price is mapped expect its amount and currency kept for the card to write`() {
        val price = ProductPrice(
            amount = 249.5f,
            currency = "DKK"
        )
        val card = sampleProductCard(price = price).mapToUi()

        assertEquals(
            expected = price,
            actual = card.price
        )
    }

    @Test
    fun `when the device locale changes after mapping expect the same card`() {
        val product = sampleProductCard(
            price = ProductPrice(
                amount = 249f,
                currency = "DKK"
            ),
            originalPrice = ProductPrice(
                amount = 499f,
                currency = "DKK"
            )
        )
        val deviceLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val mappedInEnglish = product.mapToUi()
            Locale.setDefault(Locale.forLanguageTag("da-DK"))
            val mappedInDanish = product.mapToUi()

            assertEquals(
                expected = mappedInEnglish,
                actual = mappedInDanish
            )
        } finally {
            Locale.setDefault(deviceLocale)
        }
    }

    @Test
    fun `when a product is discounted expect the price it was struck down from`() {
        val price = ProductPrice(
            amount = 249f,
            currency = "DKK"
        )
        val originalPrice = ProductPrice(
            amount = 499f,
            currency = "DKK"
        )
        val card = sampleProductCard(
            price = price,
            originalPrice = originalPrice
        ).mapToUi()

        assertEquals(
            expected = price,
            actual = card.price
        )
        assertEquals(
            expected = originalPrice,
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
