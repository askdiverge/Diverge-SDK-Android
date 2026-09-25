package ai.askdiverge.domain.model.config.display

import ai.askdiverge.domain.model.config.display.subtitle.Subtitle

internal data class Display(
    val name: String,
    val avatarUrl: String?,
    val welcomeMessage: String?,
    val subtitle: Subtitle?,
    val privacyPolicyUrl: String
)
