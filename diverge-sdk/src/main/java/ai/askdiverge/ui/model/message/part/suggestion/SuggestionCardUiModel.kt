package ai.askdiverge.ui.model.message.part.suggestion

import androidx.compose.runtime.Immutable

@Immutable
internal data class SuggestionCardUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val promptText: String
)
