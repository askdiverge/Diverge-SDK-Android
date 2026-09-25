package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.config.display.subtitle.Subtitle
import ai.askdiverge.ui.model.SubtitleUiModel

internal fun Subtitle.mapToUi() = SubtitleUiModel(
    text = text,
    link = link?.mapToUi()
)
