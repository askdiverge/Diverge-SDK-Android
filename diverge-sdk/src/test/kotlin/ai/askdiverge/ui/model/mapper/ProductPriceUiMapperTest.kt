package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ProductPriceUiMapperTest {

    @Test
    fun `when the locale is Danish expect Danish separators and the krone sign`() {
        assertEquals(
            expected = "249,95 kr.",
            actual = makeSUT(amount = 249.95f, currency = "DKK", locale = DANISH)
        )
    }

    @Test
    fun `when an amount is whole expect the currency's decimals`() {
        assertEquals(
            expected = "720,00 kr.",
            actual = makeSUT(amount = 720f, currency = "DKK", locale = DANISH)
        )
    }

    @Test
    fun `when an amount runs into thousands expect the locale's grouping`() {
        assertEquals(
            expected = "1.234,50 kr.",
            actual = makeSUT(amount = 1234.5f, currency = "DKK", locale = DANISH)
        )
    }

    @Test
    fun `when the locale writes the currency code expect it spaced from the amount`() {
        assertEquals(
            expected = "DKK 249.50",
            actual = makeSUT(amount = 249.5f, currency = "DKK", locale = Locale.US)
        )
    }

    @Test
    fun `when the locale writes a currency symbol expect it against the amount`() {
        assertEquals(
            expected = "€249.50",
            actual = makeSUT(amount = 249.5f, currency = "EUR", locale = Locale.US)
        )
    }

    @Test
    fun `when the currency has no minor unit expect no decimals`() {
        val price = makeSUT(amount = 1500f, currency = "JPY", locale = DANISH)

        assertTrue(price.contains("1.500"), price)
        assertFalse(price.contains(","), price)
    }

    @Test
    fun `when the locale's own currency has no minor unit expect the price's currency decimals`() {
        assertEquals(
            expected = "DKK 249.50",
            actual = makeSUT(amount = 249.5f, currency = "DKK", locale = Locale.JAPAN)
        )
    }

    @Test
    fun `when the currency code is unknown expect the amount followed by the code`() {
        assertEquals(
            expected = "249,5 XYZ",
            actual = makeSUT(amount = 249.5f, currency = "XYZ", locale = DANISH)
        )
    }

    // The JVM and Android's ICU separate amount and symbol with different no-break spaces.
    private fun makeSUT(amount: Float, currency: String, locale: Locale): String =
        ProductPrice(amount = amount, currency = currency)
            .mapToUi(locale)
            .replace(' ', ' ')
            .replace(' ', ' ')

    private companion object {
        val DANISH: Locale = Locale.forLanguageTag("da-DK")
    }
}
