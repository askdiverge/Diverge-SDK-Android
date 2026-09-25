package ai.askdiverge.ui.compose.theme

import androidx.compose.ui.graphics.Color
import ai.askdiverge.domain.model.config.theme.Brand
import ai.askdiverge.domain.model.config.theme.ThemeProductCard
import ai.askdiverge.domain.model.config.theme.ThemeSurface
import ai.askdiverge.domain.model.config.theme.header.ThemeHeaderButton
import ai.askdiverge.domain.model.config.theme.input.ThemeInput
import ai.askdiverge.domain.model.config.theme.messages.AssistantMessageStyle
import ai.askdiverge.domain.model.config.theme.messages.ThemeMessages
import ai.askdiverge.domain.model.config.theme.messages.UserMessageStyle
import ai.askdiverge.ui.sampleTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatbotColorsMapperTest {

    @Test
    fun `when the config provides colours expect them parsed`() {
        val colors = sampleTheme().copy(
            brand = Brand(primaryColor = "#FF0000"),
            surface = ThemeSurface(
                backgroundGradientColor = null,
                backgroundColor = "#00FF00",
                mutedTextColor = "#0000FF"
            ),
            productCard = ThemeProductCard(discountPriceColor = "#FFFFFF")
        ).toChatbotColors()

        assertEquals(
            expected = Color(
                red = 255,
                green = 0,
                blue = 0,
                alpha = 255
            ),
            actual = colors.primary
        )
        assertEquals(
            expected = Color(
                red = 0,
                green = 255,
                blue = 0,
                alpha = 255
            ),
            actual = colors.screenBackground
        )
        assertEquals(
            expected = Color(
                red = 0,
                green = 0,
                blue = 255,
                alpha = 255
            ),
            actual = colors.mutedText
        )
        assertEquals(
            expected = Color(
                red = 255,
                green = 255,
                blue = 255,
                alpha = 255
            ),
            actual = colors.productDiscountPrice
        )
    }

    @Test
    fun `when a bubble text colour is unusable expect its background to fall back too`() {
        val colors = sampleTheme().copy(
            messages = ThemeMessages(
                assistant = AssistantMessageStyle(
                    backgroundColor = "#FFFFFF",
                    textColor = "not a colour",
                    borderColor = "#123456",
                    thinkingBorderGradient = emptyList()
                ),
                user = UserMessageStyle(
                    backgroundColor = "#FFFFFF",
                    textColor = "#000000",
                    borderColor = null
                )
            )
        ).toChatbotColors()

        // Keeping a themed background with a default foreground risks an unreadable bubble, so the
        // pair falls back together.
        assertNull(colors.assistantBubbleBackground)
        assertNull(colors.assistantMessageText)
        // The border is independent, and the user bubble is unaffected.
        assertEquals(
            expected = Color(
                red = 18,
                green = 52,
                blue = 86,
                alpha = 255
            ),
            actual = colors.assistantBubbleBorder
        )
        assertEquals(
            expected = Color(
                red = 255,
                green = 255,
                blue = 255,
                alpha = 255
            ),
            actual = colors.userBubbleBackground
        )
    }

    @Test
    fun `when the input is only half described expect it to fall back as a whole`() {
        val colors = sampleTheme().copy(
            input = ThemeInput(
                textColor = "#000000",
                placeholderColor = null,
                backgroundColor = "#FFFFFF",
                borderColor = "#123456",
                sendButtonIconColor = "#654321"
            )
        ).toChatbotColors()

        assertNull(colors.inputText)
        assertNull(colors.inputPlaceholder)
        assertNull(colors.inputBackground)
        assertEquals(
            expected = Color(
                red = 18,
                green = 52,
                blue = 86,
                alpha = 255
            ),
            actual = colors.inputBorder
        )
        assertEquals(
            expected = Color(
                red = 101,
                green = 67,
                blue = 33,
                alpha = 255
            ),
            actual = colors.sendButtonIcon
        )
    }

    @Test
    fun `when the header button is only half described expect it to fall back as a whole`() {
        val colors = sampleTheme().copy(
            header = sampleTheme().header.copy(
                button = ThemeHeaderButton(
                    backgroundColor = "#FFFFFF",
                    iconColor = null
                )
            )
        ).toChatbotColors()

        assertNull(colors.headerButtonBackground)
        assertNull(colors.headerButtonIcon)
    }

    @Test
    fun `when the thinking gradient has an unusable colour expect the ones that parse to be kept`() {
        val colors = sampleTheme().copy(
            messages = sampleTheme().messages.copy(
                assistant = sampleTheme().messages.assistant.copy(
                    thinkingBorderGradient = listOf("#FF0000", "nope", "#0000FF")
                )
            )
        ).toChatbotColors()

        assertEquals(
            expected = listOf(
                Color(
                    red = 255,
                    green = 0,
                    blue = 0,
                    alpha = 255
                ),
                Color(
                    red = 0,
                    green = 0,
                    blue = 255,
                    alpha = 255
                )
            ),
            actual = colors.thinkingBorderGradientColors
        )
    }
}
