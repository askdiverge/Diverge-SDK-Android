package ai.askdiverge.ui.compose.components.banner

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ChatBannerChromeTest {

    @Test
    fun `banner colours that parse are used, surrounding spaces ignored`() {
        val colors = makeSUT(backgroundHex = "#FFF4E5", textHex = " #000000")

        assertEquals(Color(0xFFFFF4E5) to Color.Black, colors)
    }

    @Test
    fun `a missing colour takes only its own fallback`() {
        val colors = makeSUT(backgroundHex = null, textHex = "#000000")

        assertEquals(FALLBACK_FILL to Color.Black, colors)
    }

    @Test
    fun `a colour that does not parse sends both colours to their fallbacks`() {
        assertEquals(
            FALLBACK_FILL to FALLBACK_TEXT,
            makeSUT(backgroundHex = "#FFF4E5", textHex = "#333")
        )
        assertEquals(
            FALLBACK_FILL to FALLBACK_TEXT,
            makeSUT(backgroundHex = "rgb(255, 244, 229)", textHex = "#000000")
        )
    }

    private fun makeSUT(backgroundHex: String?, textHex: String?): Pair<Color, Color> =
        ChatBannerChrome.colors(
            backgroundHex = backgroundHex,
            textHex = textHex,
            fallbackFill = FALLBACK_FILL,
            fallbackText = FALLBACK_TEXT
        )

    private companion object {
        val FALLBACK_FILL = Color(0xFF1962CF)
        val FALLBACK_TEXT = Color(0xFFFFFFFF)
    }
}
