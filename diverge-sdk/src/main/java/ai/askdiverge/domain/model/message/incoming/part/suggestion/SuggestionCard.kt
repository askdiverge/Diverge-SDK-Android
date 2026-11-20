package ai.askdiverge.domain.model.message.incoming.part.suggestion

internal data class SuggestionCard(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val promptText: String
)
