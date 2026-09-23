package ai.askdiverge.domain.model

import ai.askdiverge.domain.model.config.display.Display
import ai.askdiverge.domain.model.config.theme.Theme

internal data class Config(
    val display: Display,
    val theme: Theme
)
