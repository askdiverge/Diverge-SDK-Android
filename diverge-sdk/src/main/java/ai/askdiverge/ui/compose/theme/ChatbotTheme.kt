package ai.askdiverge.ui.compose.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import ai.askdiverge.domain.model.config.theme.Theme

/**
 * @param theme the colors the config supplies, or null to fall back to the host's own scheme.
 * @param fontFamily drawn in place of the host's, or null to keep the host's typography as it is.
 * @param content the chat UI the theme applies to.
 */
@Composable
internal fun ChatbotTheme(
    theme: Theme?,
    fontFamily: FontFamily? = null,
    content: @Composable () -> Unit
) {
    val colors = remember(theme) { theme.toChatbotColors() }
    // Only the family is replaced, so the chat keeps whatever sizes and spacing the host set.
    val hostTypography = MaterialTheme.typography
    val typography = remember(hostTypography, fontFamily) {
        fontFamily?.let(hostTypography::withFamily) ?: hostTypography
    }

    CompositionLocalProvider(
        LocalChatbotPrimaryColor provides colors.primary,
        LocalScreenBackgroundColor provides colors.screenBackground,
        LocalMutedTextColor provides colors.mutedText,
        LocalHeaderButtonBackgroundColor provides colors.headerButtonBackground,
        LocalHeaderButtonIconColor provides colors.headerButtonIcon,
        LocalUserBubbleBackgroundColor provides colors.userBubbleBackground,
        LocalUserMessageTextColor provides colors.userMessageText,
        LocalUserBubbleBorderColor provides colors.userBubbleBorder,
        LocalAssistantBubbleBackgroundColor provides colors.assistantBubbleBackground,
        LocalAssistantMessageTextColor provides colors.assistantMessageText,
        LocalAssistantBubbleBorderColor provides colors.assistantBubbleBorder,
        LocalThinkingBorderGradientColors provides colors.thinkingBorderGradientColors,
        LocalInputTextColor provides colors.inputText,
        LocalInputPlaceholderColor provides colors.inputPlaceholder,
        LocalInputBackgroundColor provides colors.inputBackground,
        LocalInputBorderColor provides colors.inputBorder,
        LocalSendButtonIconColor provides colors.sendButtonIcon,
        LocalProductDiscountPriceColor provides colors.productDiscountPrice
    ) {
        MaterialTheme(typography = typography) {
            content()
        }
    }
}
