package ai.askdiverge.ui.compose.theme

import androidx.compose.ui.graphics.Color
import ai.askdiverge.domain.model.config.theme.Theme

/** Up to three colours from a group that must parse together - see [colorGroupOrNull]. */
private typealias ColorGroup = Triple<Color?, Color?, Color?>

internal fun Theme?.toChatbotColors(): ChatbotColors {
    val (userBubbleBackground, userMessageText) = colorGroupOrNull(
        listOf(
            this?.messages?.user?.backgroundColor?.toColorOrNull(),
            this?.messages?.user?.textColor?.toColorOrNull()
        )
    )
    val (assistantBubbleBackground, assistantMessageText) = colorGroupOrNull(
        listOf(
            this?.messages?.assistant?.backgroundColor?.toColorOrNull(),
            this?.messages?.assistant?.textColor?.toColorOrNull()
        )
    )
    val (headerButtonBackground, headerButtonIcon) = colorGroupOrNull(
        listOf(
            this?.header?.button?.backgroundColor?.toColorOrNull(),
            this?.header?.button?.iconColor?.toColorOrNull()
        )
    )
    val (inputBackground, inputText, inputPlaceholder) = colorGroupOrNull(
        listOf(
            this?.input?.backgroundColor?.toColorOrNull(),
            this?.input?.textColor?.toColorOrNull(),
            this?.input?.placeholderColor?.toColorOrNull()
        )
    )

    return ChatbotColors(
        primary = this?.brand?.primaryColor?.toColorOrNull(),
        screenBackground = this?.surface?.backgroundColor?.toColorOrNull(),
        mutedText = this?.surface?.mutedTextColor?.toColorOrNull(),
        headerButtonBackground = headerButtonBackground,
        headerButtonIcon = headerButtonIcon,
        userBubbleBackground = userBubbleBackground,
        userMessageText = userMessageText,
        userBubbleBorder = this?.messages?.user?.borderColor?.toColorOrNull(),
        assistantBubbleBackground = assistantBubbleBackground,
        assistantMessageText = assistantMessageText,
        assistantBubbleBorder = this?.messages?.assistant?.borderColor?.toColorOrNull(),
        thinkingBorderGradientColors = this?.messages?.assistant?.thinkingBorderGradient?.mapNotNull { it.toColorOrNull() }
            ?: emptyList(),
        inputText = inputText,
        inputPlaceholder = inputPlaceholder,
        inputBackground = inputBackground,
        inputBorder = this?.input?.borderColor?.toColorOrNull(),
        sendButtonIcon = this?.input?.sendButtonIconColor?.toColorOrNull(),
        productDiscountPrice = this?.productCard?.discountPriceColor?.toColorOrNull()
    )
}

// A themed background/foreground group must parse together - if any member fails, every member
// falls back to its own Material default rather than risk a mismatched (e.g. unreadable) combo.
// Unused trailing slots (for two-colour groups) stay null and are simply not destructured by callers.
private fun colorGroupOrNull(colors: List<Color?>): ColorGroup {
    val resolved = if (colors.any { it == null }) colors.map { null } else colors
    return Triple(resolved.getOrNull(0), resolved.getOrNull(1), resolved.getOrNull(2))
}
