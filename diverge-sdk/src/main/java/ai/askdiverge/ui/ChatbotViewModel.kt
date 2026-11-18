package ai.askdiverge.ui

import ai.askdiverge.ChatbotAttachments
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.di.ChatbotDependencies
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.livechat.LivechatBootstrapOutcome
import ai.askdiverge.domain.livechat.LivechatNote
import ai.askdiverge.domain.livechat.LivechatSession
import ai.askdiverge.domain.livechat.LivechatSnapshot
import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatCloseReason
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.toEpochMillisOrNull
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
import ai.askdiverge.domain.usecase.GetChatSessionUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.PatchFormValuesUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendLivechatMessageUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase
import ai.askdiverge.ui.form.FormCoordinator
import ai.askdiverge.ui.form.FormPhotoDestination
import ai.askdiverge.ui.form.WaitingFormCoordinator
import ai.askdiverge.ui.livechat.LivechatRatingCoordinator
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

private const val HISTORY_PAGE_SIZE = 10
private const val HISTORY_PREFETCH_DISTANCE = 3
private const val HISTORY_INITIAL_LOAD_SIZE = 10

// Earlier than any created_at: nothing has been shown yet.
private const val NOTHING_SHOWN = Long.MIN_VALUE

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
    private val getLatestMessageUseCase: GetLatestMessageUseCase,
    private val streamingMessageBuilder: StreamingMessageBuilder,
    private val uiStateHolder: ChatbotUiStateHolder,
    private val currentPage: String?,
    internal val offersAttachments: Boolean,
    createLivechatSession: (CoroutineScope) -> LivechatSession,
    submitActionUseCase: SubmitActionUseCase,
    fetchFormUseCase: FetchFormUseCase,
    getChatSessionUseCase: GetChatSessionUseCase,
    patchFormValuesUseCase: PatchFormValuesUseCase,
    submitLivechatFeedbackUseCase: SubmitLivechatFeedbackUseCase
) : ViewModel() {

    internal val uiState: StateFlow<ChatbotUiState> = uiStateHolder.uiState

    private val _openUrlEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Absolute http(s) URLs the UI should open (attachment taps after refresh). */
    internal val openUrlEvents: SharedFlow<String> = _openUrlEvents.asSharedFlow()

    private val livechatSession = createLivechatSession(viewModelScope)

    private val formCoordinator = FormCoordinator(
        uiStateHolder = uiStateHolder,
        submitActionUseCase = submitActionUseCase,
        fetchFormUseCase = fetchFormUseCase,
        scope = viewModelScope,
        currentPage = currentPage,
        offersAttachments = offersAttachments,
        encodeDispatcher = Dispatchers.Default,
        onSessionExpired = ::expireLivechat,
        onSubmitted = ::pickUpLivechatAfterForm,
        // Empty: the form card resolves blank confirmations to chat_form_submitted_default.
        defaultConfirmation = { "" }
    )

    private val waitingFormCoordinator = WaitingFormCoordinator(
        uiStateHolder = uiStateHolder,
        fetchFormUseCase = fetchFormUseCase,
        getChatSessionUseCase = getChatSessionUseCase,
        patchFormValuesUseCase = patchFormValuesUseCase,
        scope = viewModelScope,
        delayMs = { delay(it) },
        onSessionExpired = ::expireLivechat
    )

    private val ratingCoordinator = LivechatRatingCoordinator(
        uiStateHolder = uiStateHolder,
        submitLivechatFeedbackUseCase = submitLivechatFeedbackUseCase,
        scope = viewModelScope,
        onSessionExpired = ::expireLivechat,
        refresh = { livechatSession.refresh() }
    )

    private val resyncEvent = MutableSharedFlow<Unit>(replay = 1)

    // The newest created_at, in epoch milliseconds, of the messages history has shown. History
    // holds every livechat message stored up to then under an id of its own, so the livechat log's
    // copies up to this point are already on screen. Paging may apply its transforms off the main
    // thread, hence the atomic.
    private val historyHead = AtomicLong(NOTHING_SHOWN)

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
                pagingData.flatMap { message ->
                    raiseHistoryHead(message.createdAt)
                    message.mapToUi()
                }
            }
        }
        .cachedIn(viewModelScope)

    private var initJob: Job? = null
    private var sendJob: Job? = null
    private var fetchLatestMessageIdJob: Job? = null
    private var attachmentOpenJob: Job? = null
    private var livechatBootstrapJob: Job? = null
    private var livechatFloor: Long = 0L
    private val seenLivechatMessageIds = mutableSetOf<String>()

    // The newest created_at, in epoch milliseconds, of the livechat messages shown from the log
    // and from this device's sends. A teardown keeps it: a failed reset or delete keeps the
    // conversation too, and the log read again then repeats the messages up to this point.
    private var livechatHead = NOTHING_SHOWN

    // Set while a failed reset or delete reads the session it tore down again, so that read shows
    // what changed meanwhile instead of being taken as the log already in the history.
    private var livechatRestorePoint: LivechatRestorePoint? = null

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
        viewModelScope.launch {
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
        if (uiState.value.livechat.status == LivechatState.Status.ACTIVE) {
            livechatSession.visitorTyping(isEmpty = text.isBlank())
        }
    }

    /**
     * Opens an attachment: re-signs via the livechat log when the stored link is temporary,
     * otherwise opens [displayUrl] while it is still valid. Only http(s) links are opened; when
     * there is none, a snackbar says so. Taps while one is being resolved are ignored.
     */
    internal fun onAttachmentOpen(
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ) {
        if (attachmentOpenJob?.isActive == true) return
        attachmentOpenJob = viewModelScope.launch {
            val url = resolveAttachmentUrl(displayUrl, attachmentId, urlExpiresAt)
            if (url == null) {
                uiStateHolder.onAttachmentOpenFailed()
            } else {
                _openUrlEvents.emit(url)
            }
        }
    }

    /** Nothing on the device could open a link from [openUrlEvents]; a snackbar says so. */
    internal fun onOpenUrlFailed() {
        uiStateHolder.onAttachmentOpenFailed()
    }

    /**
     * A freshly signed link for attachment [attachmentId], read from the livechat log, for an image
     * whose stored link is past its expiry or failed to load. Null unless the log still holds an
     * http(s) link for it.
     */
    internal suspend fun freshAttachmentUrl(attachmentId: String): String? =
        livechatSession.freshAttachmentUrl(attachmentId)?.takeIf(AttachmentUrl::isWebUrl)

    private suspend fun resolveAttachmentUrl(
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ): String? {
        val id = attachmentId?.takeIf { it.isNotBlank() }
        val temporary = !urlExpiresAt.isNullOrBlank() && id != null
        if (temporary) {
            freshAttachmentUrl(id)?.let { return it }
            if (AttachmentUrl.isExpired(urlExpiresAt)) return null
        }
        return displayUrl.takeIf(AttachmentUrl::isWebUrl)
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
        // Whether a livechat session is open is not known until the bootstrap read answers.
        if (livechatBootstrapJob?.isActive == true) return

        fetchLatestMessageIdJob?.cancel()
        if (sendsToAgent) {
            livechatSession.endTyping()
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
            val restorePoint = livechatRestorePoint()
            uiStateHolder.onDeleteChatDataStarted()
            teardownLivechat()
            deleteChatDataUseCase().onSuccess {
                forgetShownMessages()
                uiStateHolder.onDeleteChatDataSucceeded()
                formCoordinator.cancelPending()
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
                restoreLivechat(restorePoint)
            }
        }
    }

    internal fun resetConversation() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            uiStateHolder.onConversationResetStarted()
            val restorePoint = livechatRestorePoint()
            val closeOnServer = livechatSession.isInSession
            teardownLivechat()
            // The open session is closed only once the host has supplied the new token, while the
            // current one is still in place, so a failed reset leaves it open.
            renewTokenUseCase(
                beforeReplacing = { if (closeOnServer) closeLivechatForReset() }
            ).onSuccess {
                teardownLivechat()
                forgetShownMessages()
                formCoordinator.cancelPending()
                uiStateHolder.onRestartFinished()
                resyncEvent.emit(Unit)
            }.onFailure { error ->
                ChatbotLogger.logError(error)
                uiStateHolder.onConversationResetFailed()
                restoreLivechat(restorePoint)
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
            getLatestMessageUseCase()
                .onSuccess { latest -> if (isHistoryOutdated(latest)) reloadHistory(latest) }
                .onFailure { reloadHistory(latest = null) }
        }
    }

    // History's latest message is on screen when it is the one the conversation was last read or
    // answered with, or when it is no newer than what history and the livechat log have shown. A
    // livechat message has one id in history and another in the log, so only its time tells that
    // history's copy of it is the bubble already shown.
    private fun isHistoryOutdated(latest: Message?): Boolean {
        if (latest?.id == uiState.value.lastMessageId) return false
        val createdAt = latest?.createdAt?.toEpochMillisOrNull() ?: return true
        return createdAt > maxOf(historyHead.get(), livechatHead)
    }

    private fun reloadHistory(latest: Message?) {
        raiseHistoryHead(latest?.createdAt)
        uiStateHolder.onHistoryReloaded(latest?.id)
        viewModelScope.launch { resyncEvent.emit(Unit) }
    }

    private fun raiseHistoryHead(createdAt: String?) {
        val shown = createdAt?.toEpochMillisOrNull() ?: return
        historyHead.accumulateAndGet(shown) { head, newer -> maxOf(head, newer) }
    }

    private fun raiseLivechatHead(createdAt: String) {
        val shown = createdAt.toEpochMillisOrNull() ?: return
        livechatHead = maxOf(livechatHead, shown)
    }

    // A conversation that replaces the one on screen has shown nothing yet.
    private fun forgetShownMessages() {
        historyHead.set(NOTHING_SHOWN)
        livechatHead = NOTHING_SHOWN
    }

    private fun initialize() {
        initJob?.cancel()
        sendJob?.cancel()
        fetchLatestMessageIdJob?.cancel()
        formCoordinator.cancelPending()
        forgetShownMessages()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            teardownLivechat()
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    waitingFormCoordinator.configure(config.livechatWaitingFormId)
                    getLatestMessageUseCase().onSuccess { latest ->
                        uiStateHolder.onLastMessageReceived(latest?.id)
                        raiseHistoryHead(latest?.createdAt)
                    }
                    startLivechatIfConfigured(config.livechat, isRestore = true)
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
                expireLivechat()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireLivechat()
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
            val bubble = inputMessage.toUserMessageItem()
            try {
                uiStateHolder.onSendStarted(bubble)
                // Livechat has no streaming reply bubble; clear the awaiting state immediately.
                uiStateHolder.onReplyFinished()
                previous?.join()
                val stored = sendLivechatMessageUseCase(
                    message = inputMessage,
                    currentPage = currentPage,
                    clientOperationId = operationId
                ).getOrThrow()
                // The server answers a resend with the message it stored the first time. When that
                // copy is already on screen, this send's bubble would show the message twice.
                if (!seenLivechatMessageIds.add(stored.messageId)) {
                    uiStateHolder.onLivechatSendWithdrawn(bubble.id)
                }
                raiseLivechatHead(stored.createdAt)
            } catch (_: ChatbotException.SessionExpired) {
                expireLivechat()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireLivechat()
            } catch (e: CancellationException) {
                // A cancelled send leaves no bubble for a message that was never confirmed.
                uiStateHolder.onLivechatSendWithdrawn(bubble.id)
                throw e
            } catch (_: Exception) {
                failedLivechatSend = inputMessage to operationId
                uiStateHolder.onLivechatSendFailed(bubbleId = bubble.id, restoredInput = inputMessage)
            } finally {
                livechatSendsInFlight -= 1
                // A cancelled send went with its session, and so do the snapshots deferred behind it.
                if (isActive) showDeferredLivechat()
            }
        }
    }

    private fun startLivechatIfConfigured(
        config: LivechatConfig,
        isRestore: Boolean
    ) {
        if (!config.configured) return
        livechatBootstrapJob = viewModelScope.launch {
            val outcome = if (isRestore) livechatSession.bootstrap() else livechatSession.pickUp()
            when (outcome) {
                LivechatBootstrapOutcome.SESSION_EXPIRED -> expireLivechat()
                LivechatBootstrapOutcome.IN_SESSION,
                LivechatBootstrapOutcome.NOT_IN_SESSION -> Unit
            }
        }
    }

    private fun livechatRestorePoint() = LivechatRestorePoint(
        status = uiState.value.livechat.status,
        shownMessageIds = seenLivechatMessageIds.toSet(),
        declinedHumanAgentPartIds = uiState.value.livechat.declinedHumanAgentPartIds
    )

    // A failed reset or delete keeps the conversation and its token, so the cards the visitor
    // declined stay declined and the session it tore down is read again.
    private fun restoreLivechat(restorePoint: LivechatRestorePoint) {
        restorePoint.declinedHumanAgentPartIds.forEach(uiStateHolder::onHumanAgentDeclined)
        livechatRestorePoint = restorePoint
        startLivechatIfConfigured(uiState.value.livechat.config, isRestore = true)
    }

    // A form whose submit actions include `start_livechat` queues the visitor on the server, so
    // read the state now rather than when the chat next opens.
    private fun pickUpLivechatAfterForm() {
        if (livechatSession.isInSession) return
        startLivechatIfConfigured(uiState.value.livechat.config, isRestore = false)
    }

    // Ends an open session on the server before the reset replaces the visitor. Best effort: the
    // reset goes ahead whatever the close returns.
    private suspend fun closeLivechatForReset() {
        try {
            livechatSession.close(LivechatCloseReason.RESET)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The reset continues.
        }
    }

    // The visitor session ended: livechat goes with it. Every expiry comes through here so the
    // livechat coordinators forget it too. Teardown cancels every livechat send, including one
    // this is called from, so it runs in a coroutine of its own.
    private fun expireLivechat() {
        uiStateHolder.onSessionExpired()
        viewModelScope.launch { teardownLivechat() }
    }

    private suspend fun teardownLivechat() {
        // A bootstrap read holds the session's lock until it answers; cancel it rather than wait.
        // The bootstrap's own expiry handling calls this too, and must not cancel itself.
        livechatBootstrapJob?.takeIf { it != currentCoroutineContext()[Job] }?.cancel()
        livechatBootstrapJob = null
        waitingFormCoordinator.clear()
        ratingCoordinator.clear()
        livechatSends.cancelChildren()
        livechatFloor = livechatSession.teardown()
        sendsToAgent = false
        livechatRestorePoint = null
        seenLivechatMessageIds.clear()
        deferredLivechatSnapshots.clear()
        failedLivechatSend = null
        uiStateHolder.onLivechatReset()
    }

    /** The chat is on screen again: resume livechat polling. */
    internal fun onChatVisible() {
        waitingFormCoordinator.setVisible(true)
        ratingCoordinator.setVisible(true)
        viewModelScope.launch { livechatSession.resume() }
    }

    /** The chat left the screen or the host stopped: pause livechat polling. */
    internal fun onChatHidden() {
        waitingFormCoordinator.setVisible(false)
        ratingCoordinator.setVisible(false)
        livechatSession.pause()
    }

    internal fun onWaitingFormValueChanged(
        key: String,
        value: String
    ) {
        waitingFormCoordinator.onValueChanged(key = key, value = value)
    }

    internal fun onWaitingFormSave() {
        waitingFormCoordinator.save()
    }

    internal fun onLivechatRatingScore(score: Int) {
        ratingCoordinator.selectScore(score)
    }

    internal fun onLivechatRatingCommentChanged(value: String) {
        ratingCoordinator.setComment(value)
    }

    internal fun onLivechatRatingSend() {
        ratingCoordinator.send()
    }

    internal fun onLivechatRatingSkip() {
        ratingCoordinator.skip()
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

    private fun livechatActionsAllowed(): Boolean = uiState.value.canRunLivechatAction

    internal fun onAcceptHumanAgent(partId: String) {
        val state = uiState.value
        if (!state.canRequestHumanAgent || partId in state.livechat.declinedHumanAgentPartIds) return
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
            expireLivechat()
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
            livechatSession.close(LivechatCloseReason.ENDED_BY_VISITOR)
        } catch (_: ChatbotException.SessionExpired) {
            // Snapshot path ends the chat.
        } catch (_: ChatbotException.NoTokenAvailable) {
            expireLivechat()
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
            expireLivechat()
            return
        }
        val status = snapshot.state.status
        sendsToAgent = status == LivechatState.Status.ACTIVE
        uiStateHolder.onLivechatStatus(
            status = status,
            agentDisplayName = snapshot.state.activeAgent?.displayName,
            isAgentTyping = snapshot.state.isAgentTyping
        )
        waitingFormCoordinator.statusChanged(status)
        when {
            snapshot.isRestored -> showRestoredLivechat(snapshot)
            isSendInFlight() -> deferredLivechatSnapshots += snapshot
            else -> showLivechatSnapshot(snapshot)
        }
    }

    private fun showRestoredLivechat(snapshot: LivechatSnapshot) {
        val restorePoint = livechatRestorePoint
        livechatRestorePoint = null
        if (restorePoint == null) {
            // The chat opened: how the session started is already in the history from `/messages`.
            showRestoredLivechatMessages(snapshot.messages)
            updateRating(snapshot)
            return
        }
        // A failed reset or delete: what changed since the teardown is shown as if it was polled.
        seenLivechatMessageIds += restorePoint.shownMessageIds
        val sinceTeardown = snapshot.copy(previousStatus = restorePoint.status)
        if (isSendInFlight()) {
            deferredLivechatSnapshots += sinceTeardown
        } else {
            showLivechatSnapshot(sinceTeardown)
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
        updateRating(snapshot)
    }

    // After the snapshot's notes and messages, so a prompt never covers how the chat ended.
    private fun updateRating(snapshot: LivechatSnapshot) {
        ratingCoordinator.update(
            previousStatus = snapshot.previousStatus,
            state = snapshot.state,
            isRestored = snapshot.isRestored
        )
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

    // The log found when livechat starts is on screen up to the newest message shown, from history
    // or as a bubble that outlived a teardown, so only the messages stored after it are added.
    // When nothing shown has a time to compare with, none are: the next open finds the conversation
    // changed and re-reads the history.
    private fun showRestoredLivechatMessages(messages: List<LivechatMessage>) {
        val shownThrough = maxOf(historyHead.get(), livechatHead)
        val (newer, shown) = messages.partition {
            shownThrough != NOTHING_SHOWN && it.isNewerThan(shownThrough)
        }
        seenLivechatMessageIds += shown.map { it.messageId }
        showLivechatMessages(newer)
    }

    // A message history shows is never added to the conversation as well. History gives it an id
    // of its own, so its copy in the log is told by time: it is no newer than the history head.
    // Within the log a message is told by its id.
    private fun showLivechatMessages(messages: List<LivechatMessage>) {
        val head = historyHead.get()
        val added = messages.filter { seenLivechatMessageIds.add(it.messageId) && it.isNewerThan(head) }
        added.forEach { raiseLivechatHead(it.createdAt) }
        uiStateHolder.onLivechatMessages(added.asReversed().flatMap { it.toMessage().mapToUi() })
    }

    // A created_at that does not parse has no place in the order, so it is never taken as shown.
    private fun LivechatMessage.isNewerThan(shownThrough: Long): Boolean {
        val createdAt = createdAt.toEpochMillisOrNull() ?: return true
        return createdAt > shownThrough
    }

    private fun handleStreamEvent(
        event: StreamEvent,
        sentMessage: String
    ) {
        when (event) {
            is StreamDeltaEvent -> {
                val parts = streamingMessageBuilder.process(event)
                if (parts.isNotEmpty()) {
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
            attachments: ChatbotAttachments = ChatbotAttachments.DISABLED
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
                    getLatestMessageUseCase = dependencies.getLatestMessageUseCase,
                    streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
                    uiStateHolder = ChatbotUiStateHolder(),
                    currentPage = currentPage,
                    offersAttachments = attachments == ChatbotAttachments.PHOTO_LIBRARY,
                    createLivechatSession = { scope -> dependencies.createLivechatSession(scope) },
                    submitActionUseCase = dependencies.submitActionUseCase,
                    fetchFormUseCase = dependencies.fetchFormUseCase,
                    getChatSessionUseCase = dependencies.getChatSessionUseCase,
                    patchFormValuesUseCase = dependencies.patchFormValuesUseCase,
                    submitLivechatFeedbackUseCase = dependencies.submitLivechatFeedbackUseCase
                )
            }
        }
    }
}

/** What livechat showed when a reset or delete tore it down. */
private class LivechatRestorePoint(
    val status: LivechatState.Status,
    val shownMessageIds: Set<String>,
    val declinedHumanAgentPartIds: Set<String>
)
