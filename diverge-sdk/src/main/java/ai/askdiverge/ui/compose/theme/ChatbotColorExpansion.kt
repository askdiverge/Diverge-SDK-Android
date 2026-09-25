package ai.askdiverge.ui.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalChatbotPrimaryColor = compositionLocalOf<Color?> { null }
internal val LocalScreenBackgroundColor = compositionLocalOf<Color?> { null }
internal val LocalMutedTextColor = compositionLocalOf<Color?> { null }
internal val LocalHeaderButtonBackgroundColor = compositionLocalOf<Color?> { null }
internal val LocalHeaderButtonIconColor = compositionLocalOf<Color?> { null }
internal val LocalUserBubbleBackgroundColor = compositionLocalOf<Color?> { null }
internal val LocalUserMessageTextColor = compositionLocalOf<Color?> { null }
internal val LocalUserBubbleBorderColor = compositionLocalOf<Color?> { null }
internal val LocalAssistantBubbleBackgroundColor = compositionLocalOf<Color?> { null }
internal val LocalAssistantMessageTextColor = compositionLocalOf<Color?> { null }
internal val LocalAssistantBubbleBorderColor = compositionLocalOf<Color?> { null }
internal val LocalThinkingBorderGradientColors = compositionLocalOf<List<Color>> { emptyList() }
internal val LocalInputTextColor = compositionLocalOf<Color?> { null }
internal val LocalInputPlaceholderColor = compositionLocalOf<Color?> { null }
internal val LocalInputBackgroundColor = compositionLocalOf<Color?> { null }
internal val LocalInputBorderColor = compositionLocalOf<Color?> { null }
internal val LocalSendButtonIconColor = compositionLocalOf<Color?> { null }
internal val LocalProductDiscountPriceColor = compositionLocalOf<Color?> { null }

internal val ColorScheme.chatbotPrimary: Color
    @Composable
    get() = LocalChatbotPrimaryColor.current ?: primary

internal val ColorScheme.screenBackground: Color
    @Composable
    get() = LocalScreenBackgroundColor.current ?: background

internal val ColorScheme.mutedText: Color
    @Composable
    get() = LocalMutedTextColor.current ?: onSurfaceVariant

internal val ColorScheme.headerButtonBackground: Color
    @Composable
    get() = LocalHeaderButtonBackgroundColor.current ?: Color.Transparent

internal val ColorScheme.headerButtonIcon: Color
    @Composable
    get() = LocalHeaderButtonIconColor.current ?: onSurface

internal val ColorScheme.userBubbleBackground: Color
    @Composable
    get() = LocalUserBubbleBackgroundColor.current ?: primary

internal val ColorScheme.userMessageText: Color
    @Composable
    get() = LocalUserMessageTextColor.current ?: onPrimary

internal val ColorScheme.userBubbleBorder: Color
    @Composable
    get() = LocalUserBubbleBorderColor.current ?: Color.Transparent

internal val ColorScheme.assistantBubbleBackground: Color
    @Composable
    get() = LocalAssistantBubbleBackgroundColor.current ?: surfaceVariant

internal val ColorScheme.assistantMessageText: Color
    @Composable
    get() = LocalAssistantMessageTextColor.current ?: primary

internal val ColorScheme.assistantBubbleBorder: Color
    @Composable
    get() = LocalAssistantBubbleBorderColor.current ?: outline

internal val ColorScheme.inputText: Color
    @Composable
    get() = LocalInputTextColor.current ?: onSurface

internal val ColorScheme.inputPlaceholder: Color
    @Composable
    get() = LocalInputPlaceholderColor.current ?: onSurfaceVariant

internal val ColorScheme.inputBackground: Color
    @Composable
    get() = LocalInputBackgroundColor.current ?: Color.Transparent

internal val ColorScheme.inputBorder: Color
    @Composable
    get() = LocalInputBorderColor.current ?: outline

internal val ColorScheme.sendButtonIcon: Color
    @Composable
    get() = LocalSendButtonIconColor.current ?: onPrimary

internal val ColorScheme.productDiscountPrice: Color
    @Composable
    get() = LocalProductDiscountPriceColor.current ?: onSurface
