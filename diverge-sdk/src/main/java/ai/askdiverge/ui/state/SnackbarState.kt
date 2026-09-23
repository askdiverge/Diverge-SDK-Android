package ai.askdiverge.ui.state

internal data class SnackbarState(
    val type: SnackbarType,
    // Server-provided text; only set for types whose message is dynamic (StreamError).
    val text: String? = null
)

internal enum class SnackbarType(val isError: Boolean) {
    StreamError(isError = true),
    MessageSendFailed(isError = true),
    DeleteChatDataSucceeded(isError = false),
    DeleteChatDataFailed(isError = true),
    ConversationResetFailed(isError = true)
}
