package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.message.incoming.part.suggestion.SuggestionCard
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal class SuggestionsPartBuilder : PartBuilder {

    private val suggestions = mutableListOf<SuggestionCard>()

    override fun handle(delta: StreamDeltaEvent) {
        when (delta) {
            is StreamDeltaEvent.SuggestionAppended -> suggestions.add(delta.suggestion)
            else -> Unit
        }
    }

    override fun build(): MessagePartUiModel? {
        if (suggestions.isEmpty()) return null
        return MessagePartUiModel.Suggestions(suggestions = suggestions.map { it.mapToUi() })
    }
}
