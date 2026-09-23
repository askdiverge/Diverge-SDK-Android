package ai.askdiverge.ui.state

import ai.askdiverge.ui.model.SubtitleUiModel

internal data class SessionState(
    val connectionState: ChatConnectionState = ChatConnectionState.Connecting,
    val assistantName: String = "",
    val avatarUrl: String = "",
    val welcomeMessage: String = "",
    val subtitle: SubtitleUiModel? = null,
    val privacyPolicyUrl: String = ""
)
