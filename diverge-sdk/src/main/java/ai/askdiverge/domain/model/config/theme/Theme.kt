package ai.askdiverge.domain.model.config.theme

import ai.askdiverge.domain.model.config.theme.font.ThemeFont
import ai.askdiverge.domain.model.config.theme.header.ThemeHeader
import ai.askdiverge.domain.model.config.theme.input.ThemeInput
import ai.askdiverge.domain.model.config.theme.messages.ThemeMessages

internal data class Theme(
    val brand: Brand,
    val surface: ThemeSurface,
    val header: ThemeHeader,
    val messages: ThemeMessages,
    val input: ThemeInput,
    val productCard: ThemeProductCard,
    val font: ThemeFont
)
