package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.suggestion.SuggestionCardRemote
import ai.askdiverge.domain.model.message.incoming.part.suggestion.SuggestionCard

internal fun SuggestionCardRemote.mapToDomain(): SuggestionCard = SuggestionCard(
    id = id,
    title = title,
    description = description,
    imageUrl = image_url,
    promptText = prompt_text
)
