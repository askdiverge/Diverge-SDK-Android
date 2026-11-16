package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.part.suggestion.SuggestionCard
import ai.askdiverge.ui.model.message.part.suggestion.SuggestionCardUiModel

internal fun SuggestionCard.mapToUi(): SuggestionCardUiModel = SuggestionCardUiModel(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    promptText = promptText
)
