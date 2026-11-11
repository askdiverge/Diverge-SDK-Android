package ai.askdiverge.ui.livechat

/**
 * Livechat actions the conversation list needs for the human-agent prompt card.
 */
internal data class LivechatUiCallbacks(
    val canAcceptHumanAgent: (partId: String) -> Boolean = { false },
    val onAcceptHumanAgent: (partId: String) -> Unit = {},
    val onDeclineHumanAgent: (partId: String) -> Unit = {}
) {
    companion object {
        val None = LivechatUiCallbacks()
    }
}
