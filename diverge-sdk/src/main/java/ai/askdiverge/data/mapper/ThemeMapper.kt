package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.config.theme.BrandRemote
import ai.askdiverge.data.model.config.theme.ThemeProductCardRemote
import ai.askdiverge.data.model.config.theme.ThemeRemote
import ai.askdiverge.data.model.config.theme.ThemeSurfaceRemote
import ai.askdiverge.data.model.config.theme.font.ThemeAndroidFontRemote
import ai.askdiverge.data.model.config.theme.font.ThemeFontRemote
import ai.askdiverge.data.model.config.theme.header.HeaderAlignmentRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderButtonRemote
import ai.askdiverge.data.model.config.theme.header.ThemeHeaderRemote
import ai.askdiverge.data.model.config.theme.input.ThemeInputRemote
import ai.askdiverge.data.model.config.theme.messages.AssistantMessageStyleRemote
import ai.askdiverge.data.model.config.theme.messages.ThemeMessagesRemote
import ai.askdiverge.data.model.config.theme.messages.UserMessageStyleRemote
import ai.askdiverge.domain.model.config.theme.Brand
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.domain.model.config.theme.ThemeProductCard
import ai.askdiverge.domain.model.config.theme.ThemeSurface
import ai.askdiverge.domain.model.config.theme.font.ThemeAndroidFont
import ai.askdiverge.domain.model.config.theme.font.ThemeFont
import ai.askdiverge.domain.model.config.theme.header.HeaderAlignment
import ai.askdiverge.domain.model.config.theme.header.ThemeHeader
import ai.askdiverge.domain.model.config.theme.header.ThemeHeaderButton
import ai.askdiverge.domain.model.config.theme.input.ThemeInput
import ai.askdiverge.domain.model.config.theme.messages.AssistantMessageStyle
import ai.askdiverge.domain.model.config.theme.messages.ThemeMessages
import ai.askdiverge.domain.model.config.theme.messages.UserMessageStyle

internal fun ThemeRemote.mapToDomain() = Theme(
    brand = brand.mapToDomain(),
    surface = surface.mapToDomain(),
    header = header.mapToDomain(),
    messages = messages.mapToDomain(),
    input = input.mapToDomain(),
    productCard = product_card.mapToDomain(),
    font = font.mapToDomain()
)

private fun BrandRemote.mapToDomain() = Brand(primaryColor = primary_color)

private fun ThemeSurfaceRemote.mapToDomain() = ThemeSurface(
    backgroundGradientColor = background_gradient_color,
    backgroundColor = background_color,
    mutedTextColor = muted_text_color
)

private fun ThemeHeaderRemote.mapToDomain() = ThemeHeader(
    alignment = alignment.mapToDomain(),
    logoUrl = logo.url,
    button = button.mapToDomain()
)

private fun HeaderAlignmentRemote.mapToDomain() = when (this) {
    HeaderAlignmentRemote.LEFT -> HeaderAlignment.LEFT
    HeaderAlignmentRemote.CENTER -> HeaderAlignment.CENTER
    HeaderAlignmentRemote.RIGHT -> HeaderAlignment.RIGHT
    HeaderAlignmentRemote.UNKNOWN -> HeaderAlignment.UNKNOWN
}

private fun ThemeHeaderButtonRemote.mapToDomain() = ThemeHeaderButton(
    backgroundColor = background_color,
    iconColor = icon_color
)

private fun ThemeMessagesRemote.mapToDomain() = ThemeMessages(
    assistant = assistant.mapToDomain(),
    user = user.mapToDomain()
)

private fun AssistantMessageStyleRemote.mapToDomain() = AssistantMessageStyle(
    backgroundColor = background_color,
    textColor = text_color,
    borderColor = border_color,
    thinkingBorderGradient = thinking_border_gradient
)

private fun UserMessageStyleRemote.mapToDomain() = UserMessageStyle(
    backgroundColor = background_color,
    textColor = text_color,
    borderColor = border_color
)

private fun ThemeInputRemote.mapToDomain() = ThemeInput(
    textColor = text_color,
    placeholderColor = placeholder_color,
    backgroundColor = background_color,
    borderColor = border_color,
    sendButtonIconColor = send_button.icon_color
)

private fun ThemeProductCardRemote.mapToDomain() = ThemeProductCard(discountPriceColor = discount_price_color)

private fun ThemeFontRemote.mapToDomain() = ThemeFont(android = android.mapToDomain())

private fun ThemeAndroidFontRemote.mapToDomain() = ThemeAndroidFont(assetUrl = asset_url)
