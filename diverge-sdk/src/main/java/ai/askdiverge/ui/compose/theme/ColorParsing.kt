package ai.askdiverge.ui.compose.theme

import androidx.compose.ui.graphics.Color

private const val HEX_PREFIX_LENGTH = 1
private const val HEX_RGB_LENGTH = 6
private const val HEX_RGBA_LENGTH = 8
private const val HEX_RADIX = 16
private const val FULLY_OPAQUE_ALPHA_HEX = "FF"

/**
 * Parses a `#RRGGBB` or `#RRGGBBAA` hex string into a [Color], or `null` for a null or malformed
 * input.
 */
internal fun String?.toColorOrNull(): Color? {
    if (this == null || !startsWith("#")) return null

    val hex = substring(HEX_PREFIX_LENGTH)
    val (rgbHex, alphaHex) = when (hex.length) {
        HEX_RGB_LENGTH -> hex to FULLY_OPAQUE_ALPHA_HEX
        HEX_RGBA_LENGTH -> hex.substring(0, HEX_RGB_LENGTH) to hex.substring(HEX_RGB_LENGTH)
        else -> return null
    }
    if (!rgbHex.isHexDigitsOnly() || !alphaHex.isHexDigitsOnly()) return null

    val rgb = rgbHex.toLongOrNull(radix = HEX_RADIX) ?: return null
    val alpha = alphaHex.toLongOrNull(radix = HEX_RADIX) ?: return null

    return Color(
        red = ((rgb shr 16) and 0xFF).toInt(),
        green = ((rgb shr 8) and 0xFF).toInt(),
        blue = (rgb and 0xFF).toInt(),
        alpha = alpha.toInt()
    )
}

// toLongOrNull(radix = 16) alone would accept a leading '+'/'-', silently turning malformed input
// such as "#-12345" into a valid (garbage) colour instead of null.
private fun String.isHexDigitsOnly(): Boolean = all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
