package ai.askdiverge.ui.compose.theme

import androidx.compose.ui.graphics.Color

/**
 * The colours from a config [ai.askdiverge.domain.model.config.theme.Theme], parsed
 * from hex strings into [Color]s. A field is `null` when the config didn't supply that colour or
 * the value doesn't parse.
 */
internal data class ChatbotColors(
    val primary: Color?,
    val screenBackground: Color?,
    val mutedText: Color?,
    val headerButtonBackground: Color?,
    val headerButtonIcon: Color?,
    val userBubbleBackground: Color?,
    val userMessageText: Color?,
    val userBubbleBorder: Color?,
    val assistantBubbleBackground: Color?,
    val assistantMessageText: Color?,
    val assistantBubbleBorder: Color?,
    val thinkingBorderGradientColors: List<Color>,
    val inputText: Color?,
    val inputPlaceholder: Color?,
    val inputBackground: Color?,
    val inputBorder: Color?,
    val sendButtonIcon: Color?,
    val productDiscountPrice: Color?
)
