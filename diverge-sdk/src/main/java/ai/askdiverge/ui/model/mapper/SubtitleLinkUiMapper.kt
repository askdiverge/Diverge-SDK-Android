package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.config.display.subtitle.SubtitleLink
import ai.askdiverge.ui.model.SubtitleLinkUiModel

internal fun SubtitleLink.mapToUi() = SubtitleLinkUiModel(
    text = text,
    url = url
)
