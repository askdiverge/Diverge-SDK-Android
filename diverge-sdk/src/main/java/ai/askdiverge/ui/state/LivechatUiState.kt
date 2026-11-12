package ai.askdiverge.ui.state

import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.ui.form.WaitingFormModel
import ai.askdiverge.ui.livechat.LivechatControl
import ai.askdiverge.ui.livechat.LivechatRatingModel
import androidx.compose.runtime.Immutable

@Immutable
internal data class LivechatUiState(
    val config: LivechatConfig = LivechatConfig.serverDefaults,
    val status: LivechatState.Status = LivechatState.Status.INACTIVE,
    val agentDisplayName: String? = null,
    /** Whether the connected agent is typing (only meaningful while [status] is ACTIVE). */
    val isAgentTyping: Boolean = false,
    /** Waiting-room form draft while status is WAITING; null when no card. */
    val waitingForm: WaitingFormModel? = null,
    /** CSAT draft while a closed session with pending feedback is being rated; null when no prompt. */
    val rating: LivechatRatingModel? = null,
    /** `part_id`s the visitor declined; those cards stay disabled. */
    val declinedHumanAgentPartIds: Set<String> = emptySet(),
    /** A handover or close is in flight. */
    val isBusy: Boolean = false,
    /**
     * Status the leave/end dialog asked about. Non-null while the dialog is open; a confirm that
     * finds a different status is ignored so a late answer cannot leave a session that changed.
     */
    val leaveAskedStatus: LivechatState.Status? = null
) {
    val control: LivechatControl
        get() = when {
            status.isInSession -> LivechatControl.END
            !config.enabled || !config.showLivechatLogo -> LivechatControl.HIDDEN
            config.isAvailable -> LivechatControl.START
            else -> LivechatControl.OFFLINE
        }

    val headerTitleOverride: String?
        get() = agentDisplayName?.takeIf {
            status == LivechatState.Status.ACTIVE && it.isNotBlank()
        }
}
