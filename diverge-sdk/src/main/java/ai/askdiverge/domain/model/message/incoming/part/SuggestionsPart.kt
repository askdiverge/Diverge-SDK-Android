package ai.askdiverge.domain.model.message.incoming.part

import ai.askdiverge.domain.model.message.incoming.part.suggestion.SuggestionCard

internal data class SuggestionsPart(
    val id: String,
    val suggestions: List<SuggestionCard>
) : MessagePart
