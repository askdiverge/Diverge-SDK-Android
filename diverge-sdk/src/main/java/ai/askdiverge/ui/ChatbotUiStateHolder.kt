package ai.askdiverge.ui

import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.form.FormSubmissionModel
import ai.askdiverge.ui.form.WaitingFormModel
import ai.askdiverge.ui.livechat.LivechatRatingModel
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
                pendingAttachments = emptyList(),
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
    // [messages] are newest-first (images then text) so reverseLayout keeps send order.
    fun onSendStarted(messages: List<ChatBubbleUiModel>) {
        if (messages.isEmpty()) return
        _uiState.update {
            it.copy(
                inputMessage = "",
                pendingAttachments = emptyList(),
                pendingBotMessage = PendingBotMessageState.AwaitingFirstPart,
                conversation = messages + it.conversation,
                messageToScrollTo = messages.first().id
            )
        }
    }

    // Takes the failed send's bubbles, and any reply that had started above them, back off the
    // screen and returns the draft to the input field. Other visitor bubbles stay: an earlier
    // livechat send was delivered, and one queued behind this send is still on its way.
    // serverMessage is null when the stream broke before the server could say why.
    fun onRetryableError(
        failedBubbleIds: Set<String>,
        restoredInput: String,
        restoredAttachments: List<PendingComposerAttachment>,
        serverMessage: String? = null
    ) {
        _uiState.update { state ->
            val oldestFailed = state.conversation.indexOfLast { it.id in failedBubbleIds }
            val conversation = state.conversation.filterIndexed { index, bubble ->
                val partialReply = index < oldestFailed && bubble.role == MessageRoleUiModel.BOT
                bubble.id !in failedBubbleIds && !partialReply
            }
            state.copy(
                inputMessage = restoredInput,
                pendingAttachments = restoredAttachments + state.pendingAttachments,
                snackbarState = if (serverMessage != null) {
                    SnackbarState(
                        type = SnackbarType.StreamError,
                        text = serverMessage
                    )
                } else {
                    SnackbarState(type = SnackbarType.MessageSendFailed)
                },
                pendingBotMessage = PendingBotMessageState.None,
                conversation = conversation
            )
        }
    }

    // The cap applies to picking, so a photo added after a failed send's restore never pushes one
    // of the restored photos out.
    fun addPendingAttachment(attachment: PendingComposerAttachment) {
        _uiState.update { state -> state.copy(pendingAttachments = state.pendingAttachments + attachment) }
    }

    fun removePendingAttachment(id: String) {
        _uiState.update { state ->
            state.copy(pendingAttachments = state.pendingAttachments.filterNot { it.id == id })
        }
    }

    fun onLivechatAttachmentsDropped() {
        _uiState.update {
            it.copy(
                pendingAttachments = emptyList(),
                snackbarState = SnackbarState(type = SnackbarType.LivechatAttachmentsDropped)
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
        agentDisplayName: String?,
        isAgentTyping: Boolean = false
    ) {
        _uiState.update {
            val dismissLeave = it.livechat.leaveAskedStatus != null &&
                it.livechat.leaveAskedStatus != status
            it.copy(
                livechat = it.livechat.copy(
                    status = status,
                    agentDisplayName = agentDisplayName?.trim()?.takeIf { name -> name.isNotEmpty() },
                    isAgentTyping = status == ai.askdiverge.domain.model.livechat.LivechatState.Status.ACTIVE &&
                        isAgentTyping,
                    leaveAskedStatus = if (dismissLeave) null else it.livechat.leaveAskedStatus
                )
            )
        }
    }

    fun onLivechatBusy(busy: Boolean) {
        _uiState.update { it.copy(livechat = it.livechat.copy(isBusy = busy)) }
    }

    fun setWaitingForm(model: WaitingFormModel?) {
        _uiState.update { it.copy(livechat = it.livechat.copy(waitingForm = model)) }
    }

    fun updateWaitingForm(transform: (WaitingFormModel) -> WaitingFormModel) {
        _uiState.update { state ->
            val current = state.livechat.waitingForm ?: return@update state
            state.copy(livechat = state.livechat.copy(waitingForm = transform(current)))
        }
    }

    fun setLivechatRating(model: LivechatRatingModel?) {
        _uiState.update { it.copy(livechat = it.livechat.copy(rating = model)) }
    }

    fun updateLivechatRating(transform: (LivechatRatingModel) -> LivechatRatingModel) {
        _uiState.update { state ->
            val current = state.livechat.rating ?: return@update state
            state.copy(livechat = state.livechat.copy(rating = transform(current)))
        }
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
                    isAgentTyping = false,
                    waitingForm = null,
                    rating = null,
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

    companion object {
        /**
         * Most photos the visitor can pick for one message: attach is disabled at the cap. A failed
         * send's restored photos may exceed it, so none is lost.
         */
        const val MAX_PENDING_ATTACHMENTS = 8
    }
}
