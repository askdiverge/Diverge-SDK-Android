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
import ai.askdiverge.domain.model.AttachmentUrl
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
import ai.askdiverge.domain.usecase.GetChatSessionUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageIdUseCase
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
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.form.FormCoordinator
import ai.askdiverge.ui.form.FormPhotoDestination
import ai.askdiverge.ui.form.ImageAttachmentEncoder
import ai.askdiverge.ui.form.WaitingFormCoordinator
import ai.askdiverge.ui.livechat.LivechatRatingCoordinator
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.mapper.outgoingUserBubbles
import kotlinx.coroutines.withContext
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.streaming.DefaultPartBuilderFactory
import ai.askdiverge.ui.streaming.StreamingMessageBuilder
import androidx.paging.flatMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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
    private val apiBaseUrl: String,
    internal val offersAttachments: Boolean,
    private val encodeDispatcher: CoroutineDispatcher,
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
        encodeDispatcher = encodeDispatcher,
        onSessionExpired = uiStateHolder::onSessionExpired,
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
        onSessionExpired = {
            uiStateHolder.onSessionExpired()
            viewModelScope.launch { teardownLivechat() }
        }
    )

    private val ratingCoordinator = LivechatRatingCoordinator(
        uiStateHolder = uiStateHolder,
        submitLivechatFeedbackUseCase = submitLivechatFeedbackUseCase,
        scope = viewModelScope,
        onSessionExpired = ::expireLivechat,
        refresh = { livechatSession.refresh() }
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
                pagingData.flatMap { message -> message.mapToUi(apiBaseUrl) }
            }
        }
        .cachedIn(viewModelScope)

    private var initJob: Job? = null
    private var sendJob: Job? = null
    private var fetchLatestMessageIdJob: Job? = null
    // A composer photo still encoding belongs to the draft it was picked for; a reset, delete or
    // reconnect cancels it so it never lands in the next conversation.
    private var composerPhotoJob: Job? = null
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

    // A livechat send that failed; resending the same text and images reuses the id so the server
    // can drop a duplicate.
    private var failedLivechatSend: FailedLivechatSend? = null

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
        if (uiState.value.livechat.status == LivechatState.Status.ACTIVE) {
            livechatSession.visitorTyping(isEmpty = text.isBlank())
        }
    }

    /**
     * Opens an attachment: re-signs via the livechat log when the stored link is temporary,
     * otherwise opens [displayUrl] while it is still valid.
     */
    internal fun onAttachmentOpen(
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ) {
        viewModelScope.launch {
            val url = resolveAttachmentUrl(displayUrl, attachmentId, urlExpiresAt) ?: return@launch
            _openUrlEvents.emit(url)
        }
    }

    private suspend fun resolveAttachmentUrl(
        displayUrl: String,
        attachmentId: String?,
        urlExpiresAt: String?
    ): String? {
        val id = attachmentId?.takeIf { it.isNotBlank() }
        val temporary = !urlExpiresAt.isNullOrBlank() && id != null
        if (temporary) {
            val fresh = livechatSession.freshAttachmentUrl(id)
            val resolvedFresh = fresh?.let { AttachmentUrl.resolve(it, apiBaseUrl) }
            if (resolvedFresh != null) return resolvedFresh
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
        val state = uiState.value
        val inputMessage = state.inputMessage
        val pending = state.pendingAttachments
        if (state.session.connectionState !is ChatConnectionState.Ready) return
        // The session is being replaced, so there is nowhere to send it yet.
        if (state.isResettingConversation) return
        if (state.isEncodingAttachment) return
        if (state.pendingBotMessage != PendingBotMessageState.None) return

        val hasText = inputMessage.isNotBlank()
        if (!hasText && pending.isEmpty()) return

        // An agent session that refuses photos gets none; the text stays for the next send.
        if (pending.isNotEmpty() &&
            state.livechat.status == LivechatState.Status.ACTIVE &&
            !state.livechat.config.attachmentsEnabled
        ) {
            uiStateHolder.onLivechatAttachmentsDropped()
            return
        }

        fetchLatestMessageIdJob?.cancel()
        if (sendsToAgent) {
            postLivechatMessage(inputMessage, pending)
        } else {
            sendJob?.cancel()
            postAssistantMessage(inputMessage, pending)
        }
    }

    /**
     * Encodes a composer photo pick. [open] is called off the main thread for EXIF, bounds and
     * decode streams.
     */
    internal fun onComposerPhotoPicked(open: () -> InputStream?) {
        if (!canAttachConsideringLivechat()) {
            uiStateHolder.onPhotoAttachFailed()
            return
        }
        val livechat = uiState.value.livechat
        val maxDecoded =
            if (livechat.config.attachmentsEnabled && livechat.status.isInSession) {
                livechat.config.maxAttachmentSizeBytes
            } else {
                null
            }
        uiStateHolder.setEncodingAttachment(true)
        composerPhotoJob = viewModelScope.launch {
            val pending = try {
                withContext(encodeDispatcher) {
                    ImageAttachmentEncoder.encodeForMessage(
                        open = open,
                        maxDecodedBytes = maxDecoded
                    )
                }
            } finally {
                uiStateHolder.setEncodingAttachment(false)
            }
            if (pending == null) {
                uiStateHolder.onPhotoAttachFailed()
                return@launch
            }
            uiStateHolder.addPendingAttachment(pending)
        }
    }

    internal fun onRemovePendingAttachment(id: String) {
        uiStateHolder.removePendingAttachment(id)
    }

    /**
     * Host offers photos, fewer than [ChatbotUiStateHolder.MAX_PENDING_ATTACHMENTS] are pending,
     * nothing is encoding, no reply is streaming, and no reset is replacing the conversation.
     */
    internal fun canAttach(): Boolean {
        val state = uiState.value
        return offersAttachments &&
            state.pendingAttachments.size < ChatbotUiStateHolder.MAX_PENDING_ATTACHMENTS &&
            !state.isEncodingAttachment &&
            !state.isResettingConversation &&
            state.pendingBotMessage == PendingBotMessageState.None
    }

    /** [canAttach], and while an agent is connected also `attachments_enabled`. */
    internal fun canAttachConsideringLivechat(): Boolean {
        if (!canAttach()) return false
        val livechat = uiState.value.livechat
        if (livechat.status == LivechatState.Status.ACTIVE) {
            return livechat.config.attachmentsEnabled
        }
        return true
    }

    internal fun deleteChatData() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        composerPhotoJob?.cancel()
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
        composerPhotoJob?.cancel()
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
        composerPhotoJob?.cancel()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            teardownLivechat()
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    waitingFormCoordinator.configure(config.livechatWaitingFormId)
                    getLatestMessageIdUseCase()
                        .onSuccess(uiStateHolder::onLastMessageReceived)
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

    private fun postAssistantMessage(
        inputMessage: String,
        pending: List<PendingComposerAttachment>
    ) {
        val images = pending.map { it.image }
        val bubbles = outgoingUserBubbles(text = inputMessage, images = images)
        val bubbleIds = bubbles.mapTo(HashSet()) { it.id }
        sendJob = viewModelScope.launch {
            try {
                uiStateHolder.onSendStarted(bubbles)
                sendMessageUseCase(
                    message = inputMessage,
                    images = images,
                    currentPage = currentPage
                )
                    // Emits to handle event and stops collecting once the event is either 'StreamDoneEvent' or 'StreamErrorEvent'.
                    // We want to emit the StreamDoneEvent and StreamErrorEvent to handle the events and then stop collecting.
                    .transformWhile { event ->
                        emit(event)
                        event !is StreamDoneEvent && event !is StreamErrorEvent
                    }
                    .collect { event ->
                        handleStreamEvent(
                            event = event,
                            sentMessage = inputMessage,
                            sentBubbleIds = bubbleIds,
                            restoredAttachments = pending
                        )
                    }
            } catch (_: ChatbotException.SessionExpired) {
                uiStateHolder.onSessionExpired()
            } catch (_: ChatbotException.NoTokenAvailable) {
                uiStateHolder.onSessionExpired()
            } catch (e: CancellationException) {
                // A deliberate cancel, not a failed send.
                throw e
            } catch (_: Exception) {
                uiStateHolder.onRetryableError(
                    failedBubbleIds = bubbleIds,
                    restoredInput = inputMessage,
                    restoredAttachments = pending
                )
            } finally {
                uiStateHolder.onReplyFinished()
                showDeferredLivechat()
            }
        }
    }

    private fun postLivechatMessage(
        inputMessage: String,
        pending: List<PendingComposerAttachment>
    ) {
        val images = pending.map { it.image }
        val operationId = failedLivechatSend
            ?.takeIf { it.text == inputMessage && it.images == images }
            ?.operationId
            ?: UUID.randomUUID().toString()
        failedLivechatSend = null
        val bubbles = outgoingUserBubbles(text = inputMessage, images = images)
        val bubbleIds = bubbles.mapTo(HashSet()) { it.id }
        val previous = lastLivechatSend
        livechatSendsInFlight += 1
        lastLivechatSend = viewModelScope.launch(livechatSends) {
            try {
                uiStateHolder.onSendStarted(bubbles)
                // Livechat has no streaming reply bubble; clear the awaiting state immediately.
                uiStateHolder.onReplyFinished()
                previous?.join()
                val stored = sendLivechatMessageUseCase(
                    message = inputMessage,
                    images = images,
                    currentPage = currentPage,
                    clientOperationId = operationId
                ).getOrThrow()
                seenLivechatMessageIds += stored.messageId
                uiStateHolder.onLastMessageReceived(stored.messageId)
            } catch (_: ChatbotException.SessionExpired) {
                expireLivechat()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireLivechat()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failedLivechatSend = FailedLivechatSend(
                    text = inputMessage,
                    images = images,
                    operationId = operationId
                )
                uiStateHolder.onRetryableError(
                    failedBubbleIds = bubbleIds,
                    restoredInput = inputMessage,
                    restoredAttachments = pending
                )
            } finally {
                livechatSendsInFlight -= 1
                showDeferredLivechat()
            }
        }
    }

    private fun startLivechatIfConfigured(
        config: LivechatConfig,
        isRestore: Boolean
    ) {
        if (!config.configured) return
        viewModelScope.launch {
            val outcome = if (isRestore) livechatSession.bootstrap() else livechatSession.pickUp()
            when (outcome) {
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
        if (!livechatSession.isInSession) return
        try {
            livechatSession.close(LivechatCloseRequestRemote.RESET_REASON)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The reset continues.
        }
    }

    // The visitor session ended: livechat goes with it. Teardown cancels every livechat send,
    // including one this is called from, so it runs in a coroutine of its own.
    private fun expireLivechat() {
        uiStateHolder.onSessionExpired()
        viewModelScope.launch { teardownLivechat() }
    }

    private suspend fun teardownLivechat() {
        waitingFormCoordinator.clear()
        ratingCoordinator.clear()
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
            expireLivechat()
            return
        }
        val status = snapshot.state.status
        sendsToAgent = status == LivechatState.Status.ACTIVE
        if (snapshot.previousStatus != LivechatState.Status.ACTIVE &&
            status == LivechatState.Status.ACTIVE &&
            !uiState.value.livechat.config.attachmentsEnabled &&
            uiState.value.pendingAttachments.isNotEmpty()
        ) {
            uiStateHolder.onLivechatAttachmentsDropped()
        }
        uiStateHolder.onLivechatStatus(
            status = status,
            agentDisplayName = snapshot.state.activeAgent?.displayName,
            isAgentTyping = snapshot.state.isAgentTyping
        )
        waitingFormCoordinator.statusChanged(status)
        when {
            // The log found when the chat opens, and how its session started, are already in the
            // history from `/messages`.
            snapshot.isRestored -> {
                seenLivechatMessageIds += snapshot.messages.map { it.messageId }
                snapshot.messages.lastOrNull()?.messageId?.let(uiStateHolder::onLastMessageReceived)
                updateRating(snapshot)
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

    private fun showLivechatMessages(messages: List<LivechatMessage>) {
        val bubbles = messages
            .filter { seenLivechatMessageIds.add(it.messageId) }
            .asReversed()
            .flatMap { it.toMessage().mapToUi(apiBaseUrl) }
        uiStateHolder.onLivechatMessages(bubbles)
        messages.lastOrNull()?.messageId?.let(uiStateHolder::onLastMessageReceived)
    }

    private fun handleStreamEvent(
        event: StreamEvent,
        sentMessage: String,
        sentBubbleIds: Set<String>,
        restoredAttachments: List<PendingComposerAttachment>
    ) {
        when (event) {
            is StreamDeltaEvent -> {
                val parts = streamingMessageBuilder.process(event)
                if (parts.isNotEmpty()) {
                    parts.forEach(formCoordinator::ensureFormPart)
                    uiStateHolder.onReplyStreaming(parts)
                }
            }

            is StreamPartEvent -> event.part.mapToUi(apiBaseUrl)?.let { part ->
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
                    failedBubbleIds = sentBubbleIds,
                    restoredInput = sentMessage,
                    restoredAttachments = restoredAttachments,
                    serverMessage = event.message
                )
            }
        }
    }

    private data class FailedLivechatSend(
        val text: String,
        val images: List<OutgoingImageInput>,
        val operationId: String
    )

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
                    apiBaseUrl = environment.baseUrl,
                    offersAttachments = attachments == ChatbotAttachments.PHOTO_LIBRARY,
                    encodeDispatcher = Dispatchers.Default,
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
