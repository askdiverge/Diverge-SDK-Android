package ai.askdiverge.ui.compose.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ColorParsingTest {

    @Test
    fun `toColorOrNull parses a 6-digit hex string as a fully opaque color`() {
        val result = "#1962CF".toColorOrNull()

        assertEquals(
            expected = Color(red = 0x19, green = 0x62, blue = 0xCF, alpha = 0xFF),
            actual = result
        )
    }

    @Test
    fun `toColorOrNull parses an 8-digit hex string including its alpha channel`() {
        val result = "#1962CF80".toColorOrNull()

        assertEquals(
            expected = Color(red = 0x19, green = 0x62, blue = 0xCF, alpha = 0x80),
            actual = result
        )
    }

    @Test
    fun `toColorOrNull returns null for a malformed hex string`() {
        assertNull("#12345".toColorOrNull())
        assertNull("1962CF".toColorOrNull())
        assertNull("#GGGGGG".toColorOrNull())
    }

    @Test
    fun `toColorOrNull returns null for a signed hex string`() {
        assertNull("#-12345".toColorOrNull())
        assertNull("#+12345".toColorOrNull())
    }

    @Test
    fun `toColorOrNull returns null for a null input`() {
        val input: String? = null

        assertNull(input.toColorOrNull())
    }
}
