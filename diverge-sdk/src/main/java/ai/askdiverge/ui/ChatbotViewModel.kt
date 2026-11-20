package ai.askdiverge.ui

import ai.askdiverge.ChatbotAttachments
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.di.ChatbotDependencies
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.livechat.LivechatBootstrapOutcome
import ai.askdiverge.domain.livechat.LivechatSession
import ai.askdiverge.domain.livechat.LivechatSnapshot
import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatMessage
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
import kotlinx.coroutines.currentCoroutineContext
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

    // Ids of the messages the current history Pager has loaded. Paging may apply its transforms off
    // the main thread, hence the concurrent set.
    private val historyMessageIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

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
            historyMessageIds.clear()
            getMessageHistoryUseCase(
                pagingConfig = PagingConfig(
                    pageSize = HISTORY_PAGE_SIZE,
                    prefetchDistance = HISTORY_PREFETCH_DISTANCE,
                    initialLoadSize = HISTORY_INITIAL_LOAD_SIZE,
                    enablePlaceholders = false
                )
            ).map { pagingData ->
                pagingData.flatMap { message ->
                    historyMessageIds += message.id
                    message.mapToUi()
                }
            }
        }
        .cachedIn(viewModelScope)

    private var initJob: Job? = null
    private var sendJob: Job? = null
    private var fetchLatestMessageIdJob: Job? = null
    private var livechatBootstrapJob: Job? = null
    private var livechatConfig: LivechatConfig? = null
    private var livechatFloor: Long = 0L
    private val seenLivechatMessageIds = mutableSetOf<String>()

    // The newest message a reloaded history was read with: that message and every one before it
    // are in the history, so the livechat log up to it is not added to the conversation.
    private var historyLatestMessageId: String? = null

    // Livechat sends are separate messages, so a new send never cancels an earlier one: each waits
    // for the previous to finish, keeping the order. They are children of this job so a teardown
    // can cancel them together.
    private val livechatSends = SupervisorJob(viewModelScope.coroutineContext[Job])
    private var lastLivechatSend: Job? = null

    // Messages synced while any livechat send is in flight wait until its server id is known, so
    // the echo of the visitor's own message is recognised instead of shown twice.
    private var livechatSendsInFlight = 0
    private val deferredLivechatMessages = mutableListOf<LivechatMessage>()

    // The text and client_operation_id of a livechat send that failed; resending the same text
    // reuses the id so the server can drop a duplicate.
    private var failedLivechatSend: Pair<String, String>? = null

    @Volatile
    private var inLivechatSession: Boolean = false

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
        if (inLivechatSession) {
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
            uiStateHolder.onDeleteChatDataStarted()
            teardownLivechat()
            deleteChatDataUseCase().onSuccess {
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
                restoreLivechat()
            }
        }
    }

    internal fun resetConversation() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            uiStateHolder.onConversationResetStarted()
            teardownLivechat()
            renewTokenUseCase().onSuccess {
                formCoordinator.cancelPending()
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
        historyLatestMessageId = latestMessageId
        uiStateHolder.onHistoryReloaded(latestMessageId)
        viewModelScope.launch { resyncEvent.emit(Unit) }
    }

    private fun initialize() {
        initJob?.cancel()
        sendJob?.cancel()
        fetchLatestMessageIdJob?.cancel()
        formCoordinator.cancelPending()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            teardownLivechat()
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    getLatestMessageIdUseCase()
                        .onSuccess(uiStateHolder::onLastMessageReceived)
                    livechatConfig = config.livechat
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
                uiStateHolder.onLastMessageReceived(stored.messageId)
            } catch (_: ChatbotException.SessionExpired) {
                expireFromLivechatSend()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireFromLivechatSend()
            } catch (e: CancellationException) {
                // A cancelled send leaves no bubble for a message that was never confirmed.
                uiStateHolder.onLivechatSendWithdrawn(bubble.id)
                throw e
            } catch (_: Exception) {
                failedLivechatSend = inputMessage to operationId
                uiStateHolder.onLivechatSendFailed(bubbleId = bubble.id, restoredInput = inputMessage)
            } finally {
                livechatSendsInFlight -= 1
                // A cancelled send went with its session, and so do the messages deferred behind it.
                if (livechatSendsInFlight == 0 && isActive) {
                    val deferred = deferredLivechatMessages.toList()
                    deferredLivechatMessages.clear()
                    showLivechatMessages(deferred)
                }
            }
        }
    }

    private fun startLivechatIfConfigured(config: LivechatConfig) {
        if (!config.configured) return
        livechatBootstrapJob = viewModelScope.launch {
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
        livechatConfig?.let(::startLivechatIfConfigured)
    }

    // Teardown cancels every livechat send, this one included, so it runs outside the send.
    private fun expireFromLivechatSend() {
        uiStateHolder.onSessionExpired()
        viewModelScope.launch { teardownLivechat() }
    }

    private suspend fun teardownLivechat() {
        // A bootstrap read holds the session's lock until it answers; cancel it rather than wait.
        // The bootstrap's own expiry handling calls this too, and must not cancel itself.
        livechatBootstrapJob?.takeIf { it != currentCoroutineContext()[Job] }?.cancel()
        livechatBootstrapJob = null
        livechatSends.cancelChildren()
        livechatFloor = livechatSession.teardown()
        inLivechatSession = false
        seenLivechatMessageIds.clear()
        deferredLivechatMessages.clear()
        failedLivechatSend = null
        historyLatestMessageId = null
    }

    /** The chat is on screen again: resume livechat polling. */
    internal fun onChatVisible() {
        viewModelScope.launch { livechatSession.resume() }
    }

    /** The chat left the screen or the host stopped: pause livechat polling. */
    internal fun onChatHidden() {
        livechatSession.pause()
    }

    private fun handleLivechatSnapshot(snapshot: LivechatSnapshot) {
        if (snapshot.generation < livechatFloor) return
        if (snapshot.sessionExpired) {
            inLivechatSession = false
            uiStateHolder.onSessionExpired()
            return
        }
        inLivechatSession = snapshot.state.isInSession
        when {
            snapshot.isRestored -> showRestoredLivechatMessages(snapshot.messages)
            livechatSendsInFlight > 0 -> deferredLivechatMessages += snapshot.messages
            else -> showLivechatMessages(snapshot.messages)
        }
    }

    // The log found when livechat starts is on screen up to the newest message shown, so only the
    // messages stored after it are added. When that message is not in the log, none are: the next
    // open finds the conversation changed and re-reads the history.
    private fun showRestoredLivechatMessages(messages: List<LivechatMessage>) {
        val shownThrough = messages.indexOfFirst { it.messageId == uiState.value.lastMessageId }
        val newer = if (shownThrough == -1) emptyList() else messages.drop(shownThrough + 1)
        seenLivechatMessageIds += messages.dropLast(newer.size).map { it.messageId }
        showLivechatMessages(newer)
    }

    // A message already in the history is never added to the conversation as well: the two
    // lists would then share a row key.
    private fun showLivechatMessages(messages: List<LivechatMessage>) {
        val inHistoryThrough = messages.indexOfFirst { it.messageId == historyLatestMessageId }
        val bubbles = messages
            .filterIndexed { index, message ->
                seenLivechatMessageIds.add(message.messageId) &&
                    index > inHistoryThrough &&
                    message.messageId !in historyMessageIds
            }
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
