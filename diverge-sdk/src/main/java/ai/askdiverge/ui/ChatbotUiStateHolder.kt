package ai.askdiverge.ui

import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.ui.form.FormSubmissionModel
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.LivechatUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarState
import ai.askdiverge.ui.state.SnackbarType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Sole owner and writer of [ChatbotUiState]. The ViewModel keeps the asynchronous orchestration
 * (coroutines, use cases, pagination) and translates outcomes into the semantic calls below; this
 * holder only reduces state.
 */
internal class ChatbotUiStateHolder {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    // Wipes the chat back to a clean "connecting" state. This is for first open only.
    // A restart goes through onRestartStarted() instead, which keeps
    // the config-derived session fields so /config does not
    // have to be re-fetched.
    fun onConnectionStarted() {
        _uiState.update { ChatbotUiState() }
    }

    fun onConfigLoaded(config: Config) {
        _uiState.update {
            it.copy(
                session = it.session.copy(
                    connectionState = ChatConnectionState.Ready,
                    assistantName = config.display.name,
                    avatarUrl = config.display.avatarUrl.orEmpty(),
                    welcomeMessage = config.display.welcomeMessage.orEmpty(),
                    subtitle = config.display.subtitle?.mapToUi(),
                    privacyPolicyUrl = config.display.privacyPolicyUrl
                ),
                theme = config.theme,
                livechat = it.livechat.copy(config = config.livechat)
            )
        }
    }

    fun onConnectionError(message: String) {
        _uiState.update {
            it.copy(session = it.session.copy(connectionState = ChatConnectionState.Error(message = message)))
        }
    }

    fun onRestartStarted() {
        _uiState.update {
            it.copy(session = it.session.copy(connectionState = ChatConnectionState.Restarting))
        }
    }

    fun onRestartFinished() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Ready),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList(),
                formModels = emptyMap(),
                lastMessageId = null,
                isResettingConversation = false
            )
        }
    }

    // The chat data is already gone at this point, so the conversation is cleared like a successful
    // restart. Only the session is missing, which leaves the connection in Error rather than Ready,
    // so the retry screen is shown. snackbarState is deliberately left untouched: the
    // onDeleteChatDataSucceeded() snackbar set just before this is the only place the user learns
    // that the delete itself worked, and the error screen already reports the missing session.
    fun onRestartFailed(message: String) {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Error(message = message)),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList(),
                formModels = emptyMap(),
                lastMessageId = null,
                isResettingConversation = false
            )
        }
    }

    // The optimistic messages are dropped so the refreshed history is the only source of the
    // conversation, rather than rendering it twice. Form drafts stay — history may re-show the
    // same part_ids, and wiping them would lose an in-progress fill on a stale open.
    fun onHistoryReloaded(latestMessageId: String?) {
        _uiState.update {
            it.copy(
                conversation = emptyList(),
                lastMessageId = latestMessageId
            )
        }
    }

    fun onLastMessageReceived(messageId: String?) {
        _uiState.update { it.copy(lastMessageId = messageId) }
    }

    // The token the conversation was bound to is gone, so the chat drops to the retry screen.
    fun onSessionExpired() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.SessionExpired),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList(),
                formModels = emptyMap(),
                lastMessageId = null,
                // The visitor session ended, so any livechat session ended with it.
                livechat = LivechatUiState(config = it.livechat.config)
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputMessage = text) }
    }

    // The user message is shown optimistically before the request goes out, and the draft is cleared.
    // pendingBotMessage starts fresh rather than copying the previous one, so hasStartedReplying
    // doesn't carry over and gate the typing bubble out of future replies.
    fun onSendStarted(message: ChatBubbleUiModel) {
        _uiState.update {
            it.copy(
                inputMessage = "",
                pendingBotMessage = PendingBotMessageState.AwaitingFirstPart,
                conversation = listOf(message) + it.conversation,
                messageToScrollTo = message.id
            )
        }
    }

    // Takes the message back off the screen and returns it to the input field. serverMessage is
    // null when the stream broke before the server could say why.
    fun onRetryableError(
        restoredInput: String,
        serverMessage: String? = null
    ) {
        _uiState.update { state ->
            // We will remove all messages until (and including) last message from the user
            val userMessageIndex = state.conversation.indexOfFirst { it.role == MessageRoleUiModel.USER }
            state.copy(
                inputMessage = restoredInput,
                snackbarState = if (serverMessage != null) {
                    SnackbarState(
                        type = SnackbarType.StreamError,
                        text = serverMessage
                    )
                } else {
                    SnackbarState(type = SnackbarType.MessageSendFailed)
                },
                pendingBotMessage = PendingBotMessageState.None,
                conversation = state.conversation.drop(userMessageIndex + 1)
            )
        }
    }

    fun onScrolledToMessage() {
        _uiState.update { it.copy(messageToScrollTo = null) }
    }

    fun onSnackbarConsumed() {
        _uiState.update { it.copy(snackbarState = null) }
    }

    fun onSheetRequested(sheet: SheetType) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun onSheetDismissed() {
        _uiState.update { it.copy(activeSheet = null) }
    }

    fun onReplyStreaming(parts: List<MessagePartUiModel>) {
        _uiState.update {
            it.copy(
                pendingBotMessage = PendingBotMessageState.Streaming(parts)
            )
        }
    }

    fun onReplyPartFinished(part: MessagePartUiModel) {
        _uiState.update {
            it.copy(
                conversation = listOf(
                    ChatBubbleUiModel(
                        role = MessageRoleUiModel.BOT,
                        part = part,
                        id = UUID.randomUUID().toString()
                    )
                ) + it.conversation,
                pendingBotMessage = PendingBotMessageState.Streaming(parts = emptyList())
            )
        }
    }

    fun onReplyFinished() {
        _uiState.update { it.copy(pendingBotMessage = PendingBotMessageState.None) }
    }

    /** Prepends livechat bubbles (newest first) into the in-memory conversation. */
    fun onLivechatMessages(bubbles: List<ChatBubbleUiModel>) {
        if (bubbles.isEmpty()) return
        _uiState.update { state ->
            // A bubble already in the conversation keeps its place; the list's keys must stay unique.
            val shown = state.conversation.mapTo(HashSet()) { it.id }
            val fresh = bubbles.filter { it.id !in shown }
            if (fresh.isEmpty()) state else state.copy(conversation = fresh + state.conversation)
        }
    }

    fun onLivechatStatus(
        status: ai.askdiverge.domain.model.livechat.LivechatState.Status,
        agentDisplayName: String?
    ) {
        _uiState.update {
            val dismissLeave = it.livechat.leaveAskedStatus != null &&
                it.livechat.leaveAskedStatus != status
            it.copy(
                livechat = it.livechat.copy(
                    status = status,
                    agentDisplayName = agentDisplayName?.trim()?.takeIf { name -> name.isNotEmpty() },
                    leaveAskedStatus = if (dismissLeave) null else it.livechat.leaveAskedStatus
                )
            )
        }
    }

    fun onLivechatBusy(busy: Boolean) {
        _uiState.update { it.copy(livechat = it.livechat.copy(isBusy = busy)) }
    }

    fun onLivechatLeaveRequested(status: ai.askdiverge.domain.model.livechat.LivechatState.Status) {
        _uiState.update { it.copy(livechat = it.livechat.copy(leaveAskedStatus = status)) }
    }

    fun onLivechatLeaveDismissed() {
        _uiState.update { it.copy(livechat = it.livechat.copy(leaveAskedStatus = null)) }
    }

    fun onHumanAgentDeclined(partId: String) {
        _uiState.update {
            it.copy(
                livechat = it.livechat.copy(
                    declinedHumanAgentPartIds = it.livechat.declinedHumanAgentPartIds + partId
                )
            )
        }
    }

    fun onLivechatOfflineNotice() {
        _uiState.update {
            it.copy(snackbarState = SnackbarState(type = SnackbarType.LivechatOffline))
        }
    }

    /** A handover or close failed for a reason other than offline / session expiry. */
    fun onLivechatActionFailed() {
        _uiState.update {
            it.copy(snackbarState = SnackbarState(type = SnackbarType.MessageSendFailed))
        }
    }

    fun onLivechatReset() {
        _uiState.update {
            it.copy(
                livechat = it.livechat.copy(
                    status = ai.askdiverge.domain.model.livechat.LivechatState.Status.INACTIVE,
                    agentDisplayName = null,
                    declinedHumanAgentPartIds = emptySet(),
                    isBusy = false,
                    leaveAskedStatus = null
                )
            )
        }
    }

    fun onDeleteChatDataStarted() {
        _uiState.update { it.copy(isDeletingChatData = true) }
    }

    fun onDeleteChatDataSucceeded() {
        _uiState.update {
            it.copy(
                isDeletingChatData = false,
                activeSheet = null,
                snackbarState = SnackbarState(type = SnackbarType.DeleteChatDataSucceeded)
            )
        }
    }

    fun onDeleteChatDataFailed() {
        _uiState.update {
            it.copy(
                isDeletingChatData = false,
                snackbarState = SnackbarState(type = SnackbarType.DeleteChatDataFailed)
            )
        }
    }

    fun onConversationResetStarted() {
        _uiState.update { it.copy(isResettingConversation = true) }
    }

    fun onConversationResetFailed() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Ready),
                isResettingConversation = false,
                snackbarState = SnackbarState(type = SnackbarType.ConversationResetFailed)
            )
        }
    }

    /**
     * Creates a draft the first time [form] appears. Returns `true` when a thin custom form
     * still needs its definition fetched.
     */
    fun ensureForm(form: ConversationForm): Boolean {
        var needsHydration = false
        _uiState.update { state ->
            if (state.formModels.containsKey(form.partId)) return@update state
            val model = FormSubmissionModel(form = form)
            val kind = form.kind
            needsHydration = kind is ConversationForm.Kind.Custom && form.fields.isEmpty()
            state.copy(formModels = state.formModels + (form.partId to model))
        }
        return needsHydration
    }

    fun updateForm(
        partId: String,
        transform: (FormSubmissionModel) -> FormSubmissionModel?
    ) {
        _uiState.update { state ->
            val current = state.formModels[partId] ?: return@update state
            val next = transform(current) ?: return@update state
            state.copy(formModels = state.formModels + (partId to next))
        }
    }

    fun onPhotoAttachFailed() {
        _uiState.update { it.copy(snackbarState = SnackbarState(type = SnackbarType.PhotoAttachFailed)) }
    }

    fun setEncodingAttachment(encoding: Boolean) {
        _uiState.update { it.copy(isEncodingAttachment = encoding) }
    }
}
