package ai.askdiverge.ui.model.mapper

import ai.askdiverge.LivechatSessionInfo
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState

/**
 * The visitor's livechat session as the host sees it, or null while it is unknown: from the start
 * of a connect, a reset or a delete until livechat has been read again or the outcome is known. An
 * expired visitor session has no livechat session.
 */
internal fun ChatbotUiState.toLivechatSessionInfo(): LivechatSessionInfo? = when {
    session.connectionState is ChatConnectionState.SessionExpired ->
        LivechatSessionInfo(status = LivechatSessionInfo.Status.INACTIVE)
    !livechat.isStateKnown -> null
    else -> LivechatSessionInfo(
        status = livechat.status.toHostStatus(),
        agentDisplayName = livechat.headerTitleOverride
    )
}

// A status this SDK does not know is not in session, as the chat itself treats it.
private fun LivechatState.Status.toHostStatus(): LivechatSessionInfo.Status = when (this) {
    LivechatState.Status.WAITING -> LivechatSessionInfo.Status.WAITING
    LivechatState.Status.ACTIVE -> LivechatSessionInfo.Status.ACTIVE
    LivechatState.Status.CLOSED -> LivechatSessionInfo.Status.CLOSED
    LivechatState.Status.INACTIVE,
    LivechatState.Status.UNKNOWN -> LivechatSessionInfo.Status.INACTIVE
}
