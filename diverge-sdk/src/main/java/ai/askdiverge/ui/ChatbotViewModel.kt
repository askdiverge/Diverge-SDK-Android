package ai.askdiverge.ui

import ai.askdiverge.ChatbotAttachments
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.di.ChatbotDependencies
import ai.askdiverge.data.model.livechat.LivechatCloseRequestRemote
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.livechat.LivechatBootstrapOutcome
import ai.askdiverge.domain.livechat.LivechatNote
import ai.askdiverge.domain.livechat.LivechatSession
import ai.askdiverge.domain.livechat.LivechatSnapshot
import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.ui.livechat.LivechatControl
import ai.askdiverge.ui.model.mapper.toBubble
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.model.event.StreamErrorEvent
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.event.StreamStatusEvent
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageIdUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendLivechatMessageUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.ui.form.FormCoordinator
import ai.askdiverge.ui.form.FormPhotoDestination
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.mapper.toUserMessageItem
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.streaming.DefaultPartBuilderFactory
import ai.askdiverge.ui.streaming.StreamingMessageBuilder
import androidx.paging.flatMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

private const val HISTORY_PAGE_SIZE = 10
private const val HISTORY_PREFETCH_DISTANCE = 3
private const val HISTORY_INITIAL_LOAD_SIZE = 10

internal class ChatbotViewModel(
    private val initializeUseCase: InitializeUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendLivechatMessageUseCase: SendLivechatMessageUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val invalidateSessionUseCase: InvalidateSessionUseCase,
    private val getSessionInvalidationsUseCase: GetSessionInvalidationsUseCase,
    private val renewTokenUseCase: RenewTokenUseCase,
    private val deleteChatDataUseCase: DeleteChatDataUseCase,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
    private val getLatestMessageIdUseCase: GetLatestMessageIdUseCase,
    private val streamingMessageBuilder: StreamingMessageBuilder,
    private val uiStateHolder: ChatbotUiStateHolder,
    private val currentPage: String?,
    internal val offersAttachments: Boolean,
    createLivechatSession: (CoroutineScope) -> LivechatSession,
    submitActionUseCase: SubmitActionUseCase,
    fetchFormUseCase: FetchFormUseCase
) : ViewModel() {

    internal val uiState: StateFlow<ChatbotUiState> = uiStateHolder.uiState

    private val livechatSession = createLivechatSession(viewModelScope)

    private val formCoordinator = FormCoordinator(
        uiStateHolder = uiStateHolder,
        submitActionUseCase = submitActionUseCase,
        fetchFormUseCase = fetchFormUseCase,
        scope = viewModelScope,
        currentPage = currentPage,
        offersAttachments = offersAttachments,
        encodeDispatcher = Dispatchers.Default,
        onSessionExpired = uiStateHolder::onSessionExpired,
        // Empty: the form card resolves blank confirmations to chat_form_submitted_default.
        defaultConfirmation = { "" }
    )

    private val resyncEvent = MutableSharedFlow<Unit>(replay = 1)

    // Builds the history Pager once the session is Ready (normally just the initial connect).
    // A restart rebuilds it because onRestartStarted() dips to Restarting, so the next Ready is
    // distinct, and so does a resync, which emits while the state stays Ready.
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val historyMessages: Flow<PagingData<ChatBubbleUiModel>> = combine(
        uiStateHolder.uiState
            .map { it.session.connectionState }
            .distinctUntilChanged(),
        resyncEvent.onStart { emit(Unit) } // To let the initial connect through
    ) { connectionState, _ -> connectionState }
        .filterIsInstance<ChatConnectionState.Ready>()
        .flatMapLatest {
            getMessageHistoryUseCase(
                pagingConfig = PagingConfig(
                    pageSize = HISTORY_PAGE_SIZE,
                    prefetchDistance = HISTORY_PREFETCH_DISTANCE,
                    initialLoadSize = HISTORY_INITIAL_LOAD_SIZE,
                    enablePlaceholders = false
                )
            ).map { pagingData ->
                pagingData.flatMap { message -> message.mapToUi() }
            }
        }
        .cachedIn(viewModelScope)

    private var initJob: Job? = null
    private var sendJob: Job? = null
    private var fetchLatestMessageIdJob: Job? = null
    private var livechatCollectJob: Job? = null
    private var livechatFloor: Long = 0L
    private val seenLivechatMessageIds = mutableSetOf<String>()

    // Livechat sends are separate messages, so a new send never cancels an earlier one: each waits
    // for the previous to finish, keeping the order. They are children of this job so a teardown
    // can cancel them together.
    private val livechatSends = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var lastLivechatSend: Job? = null

    // Snapshots synced while a send or reply is in flight wait for it: a livechat send's server id
    // must be known so its echo is recognised, and notes and messages must not land above the
    // visitor's bubble, which a failed send rolls back.
    private var livechatSendsInFlight = 0
    private val deferredLivechatSnapshots = mutableListOf<LivechatSnapshot>()

    // The text and client_operation_id of a livechat send that failed; resending the same text
    // reuses the id so the server can drop a duplicate.
    private var failedLivechatSend: Pair<String, String>? = null

    // Visitor sends go to the agent only while one is connected (`active`): the backend refuses
    // `POST /livechat/messages` while the visitor is queued, so waiting visitors keep the assistant.
    @Volatile
    private var sendsToAgent: Boolean = false

    init {
        livechatCollectJob = viewModelScope.launch {
            livechatSession.snapshots.collect(::handleLivechatSnapshot)
        }
        initialize()
        viewModelScope.launch {
            getSessionInvalidationsUseCase().collect {
                invalidateSessionUseCase()
                initialize()
            }
        }
    }

    override fun onCleared() {
        livechatSession.stop()
        super.onCleared()
    }

    // The conversation lives on the server, so it may have been continued, reset or deleted outside
    // this ViewModel (another device, or another screen hosting the chat). Every open, including a
    // return from a link, checks for that and only re-reads the conversation when it has changed, so
    // an unchanged chat keeps its scroll position.
    internal fun onSheetOpened() {
        when (uiState.value.session.connectionState) {
            is ChatConnectionState.Error,
            is ChatConnectionState.SessionExpired -> retry()

            is ChatConnectionState.Ready -> reloadHistoryIfOutdated()

            is ChatConnectionState.Connecting,
            is ChatConnectionState.Restarting -> Unit
        }
    }

    internal fun onInputChanged(text: String) {
        uiStateHolder.onInputChanged(text)
    }

    internal fun onSheetRequested(sheet: SheetType) {
        uiStateHolder.onSheetRequested(sheet)
    }

    internal fun onSheetDismissed() {
        uiStateHolder.onSheetDismissed()
    }

    internal fun onScrolledToMessage() {
        uiStateHolder.onScrolledToMessage()
    }

    internal fun onSnackbarConsumed() {
        uiStateHolder.onSnackbarConsumed()
    }

    internal fun onFormEnsure(form: ConversationForm) {
        formCoordinator.ensureForm(form)
    }

    internal fun onFormValueChanged(
        partId: String,
        key: String,
        value: String
    ) {
        formCoordinator.onValueChanged(partId = partId, key = key, value = value)
    }

    internal fun onFormSubmit(partId: String) {
        formCoordinator.onSubmit(partId)
    }

    internal fun onFormReload(partId: String) {
        formCoordinator.onReload(partId)
    }

    internal fun onFormPhotoPicked(
        destination: FormPhotoDestination,
        open: () -> InputStream?
    ) {
        formCoordinator.onPhotoPicked(destination, open)
    }

    internal fun onFormFileRemoved(
        partId: String,
        key: String
    ) {
        formCoordinator.onRemoveFile(partId, key)
    }

    internal fun onTicketAttachmentRemoved(partId: String) {
        formCoordinator.onRemoveTicketAttachment(partId)
    }

    internal fun postMessage() {
        val inputMessage = uiState.value.inputMessage
        if (uiState.value.session.connectionState !is ChatConnectionState.Ready) return
        // The session is being replaced, so there is nowhere to send it yet.
        if (uiState.value.isResettingConversation) return

        if (inputMessage.isBlank() || uiState.value.pendingBotMessage != PendingBotMessageState.None) return

        fetchLatestMessageIdJob?.cancel()
        if (sendsToAgent) {
            postLivechatMessage(inputMessage)
        } else {
            sendJob?.cancel()
            postAssistantMessage(inputMessage)
        }
    }

    internal fun deleteChatData() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            teardownLivechat()
            uiStateHolder.onDeleteChatDataStarted()
            deleteChatDataUseCase().onSuccess {
                uiStateHolder.onDeleteChatDataSucceeded()
                uiStateHolder.onRestartStarted()
                invalidateSessionUseCase()
                startSessionUseCase().onSuccess {
                    uiStateHolder.onRestartFinished()
                }.onFailure { error ->
                    ChatbotLogger.logError(error)
                    uiStateHolder.onRestartFailed(error.message.orEmpty())
                }
            }.onFailure { error ->
                ChatbotLogger.logError(error)
                uiStateHolder.onDeleteChatDataFailed()
                restoreLivechat()
            }
        }
    }

    internal fun resetConversation() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            uiStateHolder.onConversationResetStarted()
            closeLivechatForReset()
            teardownLivechat()
            renewTokenUseCase().onSuccess {
                uiStateHolder.onRestartFinished()
                resyncEvent.emit(Unit)
            }.onFailure { error ->
                ChatbotLogger.logError(error)
                uiStateHolder.onConversationResetFailed()
                restoreLivechat()
            }
        }
    }

    // A reply in flight has not reached the session yet, so refreshing would drop it, and a reset
    // or delete in progress replaces the conversation anyway. The next open checks again instead.
    // A failed check re-reads the conversation as before, so a session problem still surfaces.
    private fun reloadHistoryIfOutdated() {
        val state = uiState.value
        if (state.pendingBotMessage != PendingBotMessageState.None) return
        if (state.isResettingConversation || state.isDeletingChatData) return
        fetchLatestMessageIdJob?.cancel()
        fetchLatestMessageIdJob = viewModelScope.launch {
            getLatestMessageIdUseCase()
                .onSuccess { latestMessageId ->
                    if (latestMessageId != uiState.value.lastMessageId) {
                        reloadHistory(latestMessageId)
                    }
                }
                .onFailure { reloadHistory(latestMessageId = null) }
        }
    }

    private fun reloadHistory(latestMessageId: String?) {
        uiStateHolder.onHistoryReloaded(latestMessageId)
        viewModelScope.launch { resyncEvent.emit(Unit) }
    }

    private fun initialize() {
        initJob?.cancel()
        sendJob?.cancel()
        fetchLatestMessageIdJob?.cancel()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            teardownLivechat()
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    getLatestMessageIdUseCase()
                        .onSuccess(uiStateHolder::onLastMessageReceived)
                    startLivechatIfConfigured(config.livechat)
                }
                .onFailure { error ->
                    uiStateHolder.onConnectionError(error.message.orEmpty())
                }
        }
    }

    internal fun retry() {
        if (uiState.value.session.connectionState is ChatConnectionState.SessionExpired) {
            invalidateSessionUseCase()
        }
        initialize()
    }

    private fun postAssistantMessage(inputMessage: String) {
        sendJob = viewModelScope.launch {
            try {
                uiStateHolder.onSendStarted(inputMessage.toUserMessageItem())
                sendMessageUseCase(
                    message = inputMessage,
                    currentPage = currentPage
                )
                    // Emits to handle event and stops collecting once the event is either 'StreamDoneEvent' or 'StreamErrorEvent'.
                    // We want to emit the StreamDoneEvent and StreamErrorEvent to handle the events and then stop collecting.
                    .transformWhile { event ->
                        emit(event)
                        event !is StreamDoneEvent && event !is StreamErrorEvent
                    }
                    .collect { event -> handleStreamEvent(event, sentMessage = inputMessage) }
            } catch (_: ChatbotException.SessionExpired) {
                uiStateHolder.onSessionExpired()
            } catch (_: ChatbotException.NoTokenAvailable) {
                uiStateHolder.onSessionExpired()
            } catch (e: CancellationException) {
                // A deliberate cancel, not a failed send.
                throw e
            } catch (_: Exception) {
                uiStateHolder.onRetryableError(restoredInput = inputMessage)
            } finally {
                uiStateHolder.onReplyFinished()
                showDeferredLivechat()
            }
        }
    }

    private fun postLivechatMessage(inputMessage: String) {
        val operationId = failedLivechatSend?.takeIf { it.first == inputMessage }?.second
            ?: UUID.randomUUID().toString()
        failedLivechatSend = null
        val previous = lastLivechatSend
        livechatSendsInFlight += 1
        lastLivechatSend = viewModelScope.launch(livechatSends) {
            try {
                uiStateHolder.onSendStarted(inputMessage.toUserMessageItem())
                // Livechat has no streaming reply bubble; clear the awaiting state immediately.
                uiStateHolder.onReplyFinished()
                previous?.join()
                val stored = sendLivechatMessageUseCase(
                    message = inputMessage,
                    currentPage = currentPage,
                    clientOperationId = operationId
                ).getOrThrow()
                seenLivechatMessageIds += stored.messageId
                uiStateHolder.onLastMessageReceived(stored.messageId)
            } catch (_: ChatbotException.SessionExpired) {
                expireFromLivechatSend()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireFromLivechatSend()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failedLivechatSend = inputMessage to operationId
                uiStateHolder.onRetryableError(restoredInput = inputMessage)
            } finally {
                livechatSendsInFlight -= 1
                showDeferredLivechat()
            }
        }
    }

    private fun startLivechatIfConfigured(config: LivechatConfig) {
        if (!config.configured) return
        viewModelScope.launch {
            when (livechatSession.bootstrap()) {
                LivechatBootstrapOutcome.SESSION_EXPIRED -> {
                    teardownLivechat()
                    uiStateHolder.onSessionExpired()
                }
                LivechatBootstrapOutcome.IN_SESSION,
                LivechatBootstrapOutcome.NOT_IN_SESSION -> Unit
            }
        }
    }

    // A failed reset or delete keeps the conversation and its token, so the session it tore down is
    // read again.
    private fun restoreLivechat() {
        startLivechatIfConfigured(uiState.value.livechat.config)
    }

    // Ends an open session on the server before the reset replaces the visitor. Best effort: the
    // reset goes ahead whatever the close returns.
    private suspend fun closeLivechatForReset() {
        if (!livechatSession.isInSession) return
        try {
            livechatSession.close(LivechatCloseRequestRemote.RESET_REASON)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The reset continues.
        }
    }

    // Teardown cancels every livechat send, this one included, so it runs outside the send.
    private fun expireFromLivechatSend() {
        uiStateHolder.onSessionExpired()
        viewModelScope.launch { teardownLivechat() }
    }

    private suspend fun teardownLivechat() {
        livechatSends.cancelChildren()
        livechatFloor = livechatSession.teardown()
        sendsToAgent = false
        seenLivechatMessageIds.clear()
        deferredLivechatSnapshots.clear()
        failedLivechatSend = null
        uiStateHolder.onLivechatReset()
    }

    /** The chat is on screen again: resume livechat polling. */
    internal fun onChatVisible() {
        viewModelScope.launch { livechatSession.resume() }
    }

    /** The chat left the screen or the host stopped: pause livechat polling. */
    internal fun onChatHidden() {
        livechatSession.pause()
    }

    /**
     * Header livechat control: start / retry offline → handover; end → leave confirmation.
     */
    internal fun onLivechatControlClick() {
        if (!livechatActionsAllowed()) return
        val livechat = uiState.value.livechat
        when (livechat.control) {
            LivechatControl.START,
            LivechatControl.OFFLINE -> viewModelScope.launch { requestLivechatHandover(partId = null) }

            LivechatControl.END -> uiStateHolder.onLivechatLeaveRequested(livechat.status)

            LivechatControl.HIDDEN -> Unit
        }
    }

    internal fun onConfirmLeaveLivechat() {
        val asked = uiState.value.livechat.leaveAskedStatus
        uiStateHolder.onLivechatLeaveDismissed()
        val livechat = uiState.value.livechat
        if (!livechatActionsAllowed() || livechat.control != LivechatControl.END || asked != livechat.status) {
            return
        }
        viewModelScope.launch { endLivechat() }
    }

    internal fun onDismissLeaveLivechat() {
        uiStateHolder.onLivechatLeaveDismissed()
    }

    internal fun canAcceptHumanAgent(partId: String): Boolean {
        val livechat = uiState.value.livechat
        return livechatActionsAllowed() &&
            !livechat.status.isInSession &&
            partId !in livechat.declinedHumanAgentPartIds &&
            uiState.value.pendingBotMessage == PendingBotMessageState.None
    }

    // Handover and close need the current visitor: not while connecting, restarting, resetting or
    // deleting, and one at a time.
    private fun livechatActionsAllowed(): Boolean {
        val state = uiState.value
        return state.session.connectionState is ChatConnectionState.Ready &&
            !state.isResettingConversation &&
            !state.isDeletingChatData &&
            !state.livechat.isBusy
    }

    internal fun onAcceptHumanAgent(partId: String) {
        if (!canAcceptHumanAgent(partId)) return
        viewModelScope.launch { requestLivechatHandover(partId = partId) }
    }

    internal fun onDeclineHumanAgent(partId: String) {
        uiStateHolder.onHumanAgentDeclined(partId)
    }

    private suspend fun requestLivechatHandover(partId: String?) {
        if (!livechatActionsAllowed()) return
        if (uiState.value.pendingBotMessage != PendingBotMessageState.None) return
        uiStateHolder.onLivechatBusy(true)
        try {
            val source = if (partId == null) {
                LivechatHandoverRequest.Source.MANUAL_BUTTON
            } else {
                LivechatHandoverRequest.Source.ASSISTANT_MARKER
            }
            livechatSession.handover(
                source = source,
                partId = partId,
                clientContext = LivechatClientContext.native(page = currentPage)
            )
        } catch (_: LivechatRequestException.Conflict) {
            uiStateHolder.onLivechatOfflineNotice()
        } catch (_: ChatbotException.SessionExpired) {
            // Snapshot / poll path ends the chat.
        } catch (_: ChatbotException.NoTokenAvailable) {
            uiStateHolder.onSessionExpired()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            uiStateHolder.onLivechatActionFailed()
        } finally {
            uiStateHolder.onLivechatBusy(false)
        }
    }

    private suspend fun endLivechat() {
        uiStateHolder.onLivechatBusy(true)
        try {
            livechatSession.close(LivechatCloseRequestRemote.ENDED_BY_VISITOR_REASON)
        } catch (_: ChatbotException.SessionExpired) {
            // Snapshot path ends the chat.
        } catch (_: ChatbotException.NoTokenAvailable) {
            uiStateHolder.onSessionExpired()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            uiStateHolder.onLivechatActionFailed()
        } finally {
            uiStateHolder.onLivechatBusy(false)
        }
    }

    private fun handleLivechatSnapshot(snapshot: LivechatSnapshot) {
        if (snapshot.generation < livechatFloor) return
        if (snapshot.sessionExpired) {
            sendsToAgent = false
            uiStateHolder.onSessionExpired()
            return
        }
        val status = snapshot.state.status
        sendsToAgent = status == LivechatState.Status.ACTIVE
        uiStateHolder.onLivechatStatus(
            status = status,
            agentDisplayName = snapshot.state.activeAgent?.displayName
        )
        when {
            // The log found when the chat opens, and how its session started, are already in the
            // history from `/messages`.
            snapshot.isRestored -> {
                seenLivechatMessageIds += snapshot.messages.map { it.messageId }
                snapshot.messages.lastOrNull()?.messageId?.let(uiStateHolder::onLastMessageReceived)
            }
            isSendInFlight() -> deferredLivechatSnapshots += snapshot
            else -> showLivechatSnapshot(snapshot)
        }
    }

    private fun isSendInFlight(): Boolean =
        livechatSendsInFlight > 0 || uiState.value.pendingBotMessage != PendingBotMessageState.None

    private fun showDeferredLivechat() {
        if (isSendInFlight()) return
        val deferred = deferredLivechatSnapshots.toList()
        deferredLivechatSnapshots.clear()
        deferred.forEach(::showLivechatSnapshot)
    }

    /** Adds a snapshot's notes and messages in order: how the session started, its messages, its end. */
    private fun showLivechatSnapshot(snapshot: LivechatSnapshot) {
        val status = snapshot.state.status
        noteSessionStart(
            previous = snapshot.previousStatus,
            status = status,
            agentName = snapshot.state.activeAgent?.displayName
        )
        showLivechatMessages(snapshot.messages)
        if (snapshot.previousStatus.isInSession && status == LivechatState.Status.CLOSED) {
            val kind = if (snapshot.state.isClosedForInactivity) {
                LivechatNote.Kind.EndedForInactivity
            } else {
                LivechatNote.Kind.Ended
            }
            uiStateHolder.onLivechatMessages(listOf(LivechatNote(kind).toBubble()))
        }
    }

    private fun noteSessionStart(
        previous: LivechatState.Status,
        status: LivechatState.Status,
        agentName: String?
    ) {
        if (previous != LivechatState.Status.WAITING && status == LivechatState.Status.WAITING) {
            uiStateHolder.onLivechatMessages(listOf(LivechatNote(LivechatNote.Kind.Queued).toBubble()))
        }
        if (previous != LivechatState.Status.ACTIVE && status == LivechatState.Status.ACTIVE) {
            val name = agentName?.trim()?.takeIf { it.isNotEmpty() } ?: ""
            uiStateHolder.onLivechatMessages(
                listOf(LivechatNote(LivechatNote.Kind.AgentJoined(displayName = name)).toBubble())
            )
        }
    }

    private fun showLivechatMessages(messages: List<LivechatMessage>) {
        val bubbles = messages
            .filter { seenLivechatMessageIds.add(it.messageId) }
            .asReversed()
            .flatMap { it.toMessage().mapToUi() }
        uiStateHolder.onLivechatMessages(bubbles)
        messages.lastOrNull()?.messageId?.let(uiStateHolder::onLastMessageReceived)
    }

    private fun handleStreamEvent(
        event: StreamEvent,
        sentMessage: String
    ) {
        when (event) {
            is StreamDeltaEvent -> {
                val parts = streamingMessageBuilder.process(event)
                if (parts.isNotEmpty()) {
                    parts.forEach(formCoordinator::ensureFormPart)
                    uiStateHolder.onReplyStreaming(parts)
                }
            }

            is StreamPartEvent -> event.part.mapToUi()?.let { part ->
                formCoordinator.ensureFormPart(part)
                uiStateHolder.onReplyPartFinished(part)
            }

            is StreamDoneEvent -> {
                uiStateHolder.onLastMessageReceived(event.message.id)
                uiStateHolder.onReplyFinished()
            }


            is StreamStatusEvent -> Unit

            is StreamErrorEvent -> {
                streamingMessageBuilder.reset()
                uiStateHolder.onRetryableError(
                    restoredInput = sentMessage,
                    serverMessage = event.message
                )
            }
        }
    }

    companion object {
        fun factory(
            chatbotCallbacks: ChatbotCallbacks,
            environment: DivergeEnvironment,
            currentPage: String? = null,
            attachments: ChatbotAttachments = ChatbotAttachments.PHOTO_LIBRARY
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val dependencies = ChatbotDependencies(
                    chatbotCallbacks = chatbotCallbacks,
                    environment = environment
                )
                ChatbotViewModel(
                    initializeUseCase = dependencies.initializeUseCase,
                    sendMessageUseCase = dependencies.sendMessageUseCase,
                    sendLivechatMessageUseCase = dependencies.sendLivechatMessageUseCase,
                    startSessionUseCase = dependencies.startSessionUseCase,
                    invalidateSessionUseCase = dependencies.invalidateSessionUseCase,
                    getSessionInvalidationsUseCase = dependencies.getSessionInvalidationsUseCase,
                    renewTokenUseCase = dependencies.renewTokenUseCase,
                    deleteChatDataUseCase = dependencies.deleteChatDataUseCase,
                    getMessageHistoryUseCase = dependencies.getMessageHistoryUseCase,
                    getLatestMessageIdUseCase = dependencies.getLatestMessageIdUseCase,
                    streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
                    uiStateHolder = ChatbotUiStateHolder(),
                    currentPage = currentPage,
                    offersAttachments = attachments == ChatbotAttachments.PHOTO_LIBRARY,
                    createLivechatSession = { scope -> dependencies.createLivechatSession(scope) },
                    submitActionUseCase = dependencies.submitActionUseCase,
                    fetchFormUseCase = dependencies.fetchFormUseCase
                )
            }
        }
    }
}
