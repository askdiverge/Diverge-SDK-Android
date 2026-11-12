package ai.askdiverge.ui.state

import androidx.compose.runtime.Immutable
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.form.FormSubmissionModel
import ai.askdiverge.ui.model.ChatBubbleUiModel

@Immutable
internal data class ChatbotUiState(
    val session: SessionState = SessionState(),
    val pendingBotMessage: PendingBotMessageState = PendingBotMessageState.None,
    val conversation: List<ChatBubbleUiModel> = emptyList(),
    /** Drafts keyed by form `partId`, so recycling a list item keeps the visitor's answers. */
    val formModels: Map<String, FormSubmissionModel> = emptyMap(),
    /** True while a form or composer photo is JPEG-encoding off the main thread. */
    val isEncodingAttachment: Boolean = false,
    /** Composer photo chips waiting to send with the next message. */
    val pendingAttachments: List<PendingComposerAttachment> = emptyList(),
    val lastMessageId: String? = null,
    val messageToScrollTo: String? = null,
    val inputMessage: String = "",
    val activeSheet: SheetType? = null,
    val isDeletingChatData: Boolean = false,
    val isResettingConversation: Boolean = false,
    val snackbarState: SnackbarState? = null,
    val theme: Theme? = null,
    val livechat: LivechatUiState = LivechatUiState()
) {
    /**
     * Whether a livechat handover or close can start: the session is ready, not being reset or
     * deleted, and no other handover or close is running.
     */
    val canRunLivechatAction: Boolean
        get() = session.connectionState is ChatConnectionState.Ready &&
            !isResettingConversation &&
            !isDeletingChatData &&
            !livechat.isBusy

    /**
     * Whether an assistant's `request_human_agent` card can be accepted, unless the visitor declined
     * that card: no livechat session is open and no reply is in flight.
     */
    val canRequestHumanAgent: Boolean
        get() = canRunLivechatAction &&
            !livechat.status.isInSession &&
            pendingBotMessage == PendingBotMessageState.None
}
