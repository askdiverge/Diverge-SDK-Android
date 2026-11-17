package ai.askdiverge.ui.compose.components.banner

import androidx.compose.ui.graphics.Color
import ai.askdiverge.ui.compose.theme.toColorOrNull

/** Fill / text colours for an in-chat banner. Unparseable hex falls back like the web widget. */
internal object ChatBannerChrome {

    /** Card fill — valid `#RRGGBB` / `#RRGGBBAA`, otherwise the chatbot accent. */
    fun fill(
        hex: String?,
        accent: Color
    ): Color = hex.toColorOrNull() ?: accent

    /** Body / CTA / dismiss glyph — valid hex, otherwise white. */
    fun foreground(hex: String?): Color = hex.toColorOrNull() ?: Color.White
}
