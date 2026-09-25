package ai.askdiverge.ui.state

internal sealed class ChatConnectionState {
    data object Connecting : ChatConnectionState()
    data object Restarting : ChatConnectionState()
    data object Ready : ChatConnectionState()
    data object SessionExpired : ChatConnectionState()
    data class Error(val message: String) : ChatConnectionState()
}
