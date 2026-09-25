package ai.askdiverge.domain.model.config.theme.header

internal data class ThemeHeader(
    val alignment: HeaderAlignment,
    val logoUrl: String?,
    val button: ThemeHeaderButton
)
