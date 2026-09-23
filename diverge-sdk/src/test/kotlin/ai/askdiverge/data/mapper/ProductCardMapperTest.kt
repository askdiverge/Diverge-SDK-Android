package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.product.ProductCardRemote
import ai.askdiverge.data.model.message.incoming.part.product.ProductPriceRemote
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductCardMapperTest {

    @Test
    fun `maps all fields correctly`() {
        val remote = ProductCardRemote(
            id = "card-1",
            title = "Shirt",
            description = "A nice shirt",
            image_url = "https://example.com/shirt.jpg",
            original_price = ProductPriceRemote(amount = 100f, currency = "SEK"),
            price = ProductPriceRemote(amount = 80f, currency = "DKK"),
            url = "https://example.com/shirt"
        )

        val result = remote.mapToDomain()

        assertEquals("card-1", result.id)
        assertEquals("Shirt", result.title)
        assertEquals("A nice shirt", result.description)
        assertEquals("https://example.com/shirt.jpg", result.imageUrl)
        assertEquals(100f, result.originalPrice?.amount)
        assertEquals("SEK", result.originalPrice?.currency)
        assertEquals(80f, result.price.amount)
        assertEquals("DKK", result.price.currency)
        assertEquals("https://example.com/shirt", result.url)
    }
}
