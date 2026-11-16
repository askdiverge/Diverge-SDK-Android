package ai.askdiverge.ui.suggestion

/**
 * Suggestion-card sends the conversation list needs.
 */
internal data class SuggestionUiCallbacks(
    /** Whether a tap can send a card's prompt now; the cards show disabled otherwise. */
    val canSend: Boolean = false,
    /** Sends the tapped card's `prompt_text` as the next user message. */
    val onSend: (promptText: String) -> Unit = {}
) {
    companion object {
        val None = SuggestionUiCallbacks()
    }
}
