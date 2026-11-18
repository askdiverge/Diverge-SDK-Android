package ai.askdiverge.ui.livechat

/**
 * Livechat state and actions the conversation list needs for the human-agent prompt card.
 */
internal data class LivechatUiCallbacks(
    /** See `ChatbotUiState.canRequestHumanAgent`. */
    val canRequestHumanAgent: Boolean = false,
    /** `part_id`s the visitor declined; those cards stay disabled. */
    val declinedHumanAgentPartIds: Set<String> = emptySet(),
    val onAcceptHumanAgent: (partId: String) -> Unit = {},
    val onDeclineHumanAgent: (partId: String) -> Unit = {}
) {
    /** Whether the card for [partId] can be accepted now. */
    fun canAcceptHumanAgent(partId: String): Boolean =
        canRequestHumanAgent && partId !in declinedHumanAgentPartIds

    companion object {
        val None = LivechatUiCallbacks()
    }
}
