package ai.askdiverge.ui.compose.components.banner

import androidx.compose.ui.graphics.Color
import ai.askdiverge.domain.trimToNull
import ai.askdiverge.ui.compose.theme.toColorOrNull

/** Fill and text colours for an in-chat banner. */
internal object ChatBannerChrome {

    /**
     * The banner's fill and text colours, each `#RRGGBB` or `#RRGGBBAA`. A missing colour takes its
     * own fallback. When either one is set but cannot be parsed, both take their fallbacks, so the
     * text is never drawn on a fill it wasn't chosen for.
     */
    fun colors(
        backgroundHex: String?,
        textHex: String?,
        fallbackFill: Color,
        fallbackText: Color
    ): Pair<Color, Color> {
        val background = backgroundHex.trimToNull()
        val text = textHex.trimToNull()
        val fill = background?.toColorOrNull()
        val foreground = text?.toColorOrNull()
        if ((background != null && fill == null) || (text != null && foreground == null)) {
            return fallbackFill to fallbackText
        }
        return (fill ?: fallbackFill) to (foreground ?: fallbackText)
    }
}
