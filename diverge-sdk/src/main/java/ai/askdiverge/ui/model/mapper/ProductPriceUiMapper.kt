package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.product.ProductPrice
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The price as [locale] writes an amount of its ISO 4217 currency, with that currency's decimals:
 * "DKK 249.50" in en-US, "249,50 kr." in da-DK. A sign made of letters is kept apart from the
 * digits by a no-break space, as CLDR's currency spacing does; a symbol such as € stays against
 * them. A code the platform does not know is written after the amount in [locale]'s number format.
 * The amount is read from its decimal form, so a float such as 249.95 keeps its value.
 */
internal fun ProductPrice.mapToUi(locale: Locale): String {
    val exactAmount = BigDecimal(amount.toString())
    val knownCurrency = currencyOrNull(currency)
        ?: return "${NumberFormat.getNumberInstance(locale).format(exactAmount)} $currency".trim()
    return NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = knownCurrency
        knownCurrency.defaultFractionDigits.takeIf { it >= 0 }?.let { digits ->
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
    }.format(exactAmount).replace(LETTER_SIGN_AGAINST_DIGITS, NO_BREAK_SPACE)
}

// java.text writes a letter sign against the amount ("DKK559.00").
private val LETTER_SIGN_AGAINST_DIGITS = Regex("""(?<=\p{L})(?=\p{Nd})|(?<=\p{Nd})(?=\p{L})""")
private const val NO_BREAK_SPACE = " "

private fun currencyOrNull(code: String): Currency? = try {
    Currency.getInstance(code)
} catch (e: IllegalArgumentException) {
    null
}
