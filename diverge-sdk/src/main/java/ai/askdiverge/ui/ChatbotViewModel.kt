package ai.askdiverge.ui

import ai.askdiverge.ChatbotAttachments
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.ChatbotRating
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
import ai.askdiverge.domain.model.export.ExportDestination
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatCloseReason
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.toEpochMillisOrNull
import ai.askdiverge.domain.trimToNull
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
import ai.askdiverge.domain.usecase.ExportMyDataUseCase
import ai.askdiverge.domain.usecase.FetchBannersUseCase
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.model.banner.ChatBanner
import ai.askdiverge.domain.usecase.GetChatSessionUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.PatchFormValuesUseCase
import ai.askdiverge.domain.usecase.RateConversationUseCase
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
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import kotlinx.coroutines.withContext
import ai.askdiverge.ui.rating.ConversationRatingCoordinator
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
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
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val exportMyDataUseCase: ExportMyDataUseCase,
    private val fetchBannersUseCase: FetchBannersUseCase,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
    private val getLatestMessageUseCase: GetLatestMessageUseCase,
    private val streamingMessageBuilder: StreamingMessageBuilder,
    private val uiStateHolder: ChatbotUiStateHolder,
    private val currentPage: String?,
    internal val offersAttachments: Boolean,
    private val encodeDispatcher: CoroutineDispatcher,
    createLivechatSession: (CoroutineScope) -> LivechatSession,
    submitActionUseCase: SubmitActionUseCase,
    fetchFormUseCase: FetchFormUseCase,
    getChatSessionUseCase: GetChatSessionUseCase,
    patchFormValuesUseCase: PatchFormValuesUseCase,
    submitLivechatFeedbackUseCase: SubmitLivechatFeedbackUseCase,
    rateConversationUseCase: RateConversationUseCase,
    ratingEnabled: Boolean
) : ViewModel() {

    internal val uiState: StateFlow<ChatbotUiState> = uiStateHolder.uiState

    private val _openUrlEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Absolute http(s) URLs the UI should open (attachment taps after refresh). */
    internal val openUrlEvents: SharedFlow<String> = _openUrlEvents.asSharedFlow()

    private val _dismissEvents = Channel<Unit>(Channel.CONFLATED)
    /**
     * Emitted when the sheet should finish dismissing after a conversation-rating skip/submit.
     * Held until collected, so a dismiss that lands while the sheet is recomposing is not lost;
     * a sheet the host opens again drops one left from an earlier sheet ([onNewPresentation]).
     */
    internal val dismissEvents: Flow<Unit> = _dismissEvents.receiveAsFlow()

    private val livechatSession = createLivechatSession(viewModelScope)

    /**
     * True once a message the visitor sent this ViewModel lifetime was delivered. One still on its
     * way is a bubble in the conversation; one that failed is neither.
     */
    private var visitorHasSentMessage = false

    private val formCoordinator = FormCoordinator(
        uiStateHolder = uiStateHolder,
        submitActionUseCase = submitActionUseCase,
        fetchFormUseCase = fetchFormUseCase,
        scope = viewModelScope,
        currentPage = currentPage,
        offersAttachments = offersAttachments,
        encodeDispatcher = encodeDispatcher,
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

    private val conversationRatingCoordinator = ConversationRatingCoordinator(
        uiStateHolder = uiStateHolder,
        rateConversationUseCase = rateConversationUseCase,
        scope = viewModelScope,
        ratingEnabled = ratingEnabled,
        hasVisitorMessage = {
            visitorHasSentMessage ||
                uiStateHolder.uiState.value.conversation.any { it.role == MessageRoleUiModel.USER }
        },
        isLivechatBlocking = {
            val livechat = uiStateHolder.uiState.value.livechat
            // A close held back behind a send has its own CSAT still to offer.
            livechat.status.isInSession || livechat.isBusy ||
                deferredLivechatSnapshots.isNotEmpty()
        },
        livechatOwnsClose = ratingCoordinator::suppressesConversationRating,
        onSessionExpired = ::expireLivechat,
        onReadyToDismiss = { _dismissEvents.trySend(Unit) }
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
                    message.mapToUi(offersAttachments = offersAttachments)
                }
            }
        }
        .cachedIn(viewModelScope)

    private var initJob: Job? = null
    private var sendJob: Job? = null
    private var fetchLatestMessageIdJob: Job? = null
    private var attachmentOpenJob: Job? = null

    // A composer photo still encoding belongs to the draft it was picked for; a reset, delete or
    // reconnect cancels it so it never lands in the next conversation.
    private var composerPhotoJob: Job? = null
    // An export belongs to the session that started it; dismissing Privacy, a session expiry or a
    // reconnect cancels it.
    private var exportJob: Job? = null
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

    // A livechat send that failed; resending the same text and images reuses the id so the server
    // can drop a duplicate.
    private var failedLivechatSend: FailedLivechatSend? = null

    // The same for suggestion-card prompts, kept apart so a tap never drops the id of a failed
    // draft still in the composer.
    private var failedPromptSend: FailedLivechatSend? = null

    // Set when the visitor taps Add photo on a `request_image_upload` card; consumed by the next
    // composer encode so the marker's max_size_bytes wins over the default message budget.
    private var pendingImageUploadMaxBytes: Int? = null

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
                // The visitor may be a different one.
                clearConversationRatingGate()
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
        exportJob?.cancel()
        uiStateHolder.onSheetDismissed()
    }

    /**
     * Saves the visitor's GDPR export to [destination], the document they picked in the system save
     * dialog. Runs only while Privacy is open on a Ready chat and no other export runs; otherwise
     * [destination] is discarded, so a save that arrives after the activity was recreated, after a
     * reconnect, or twice leaves nothing behind. Session expiry dismisses Privacy and ends the
     * session; other failures stay on the sheet with an inline error. The visitor token is kept.
     */
    internal fun exportMyData(destination: ExportDestination) {
        val state = uiState.value
        if (state.isExportingChatData ||
            state.activeSheet != SheetType.Privacy ||
            state.session.connectionState !is ChatConnectionState.Ready
        ) {
            viewModelScope.launch { exportMyDataUseCase.discard(destination) }
            return
        }
        val previousExport = exportJob
        exportJob = viewModelScope.launch {
            uiStateHolder.onExportStarted()
            // A cancelled export finishes removing its document before this one opens its own.
            // Cancelled while it waits, this export discards the document it was given.
            try {
                previousExport?.join()
            } catch (e: CancellationException) {
                withContext(NonCancellable) { exportMyDataUseCase.discard(destination) }
                throw e
            }
            exportMyDataUseCase(destination)
                .onSuccess { uiStateHolder.onExportSucceeded() }
                .onFailure { error ->
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> {
                            uiStateHolder.onSheetDismissed()
                            expireLivechat()
                        }
                        else -> uiStateHolder.onExportFailed()
                    }
                }
        }
    }

    /** The device has no system save dialog ([error] says why), so the export cannot start. */
    internal fun onExportUnavailable(error: Throwable) {
        ChatbotLogger.logError(error)
        uiStateHolder.onExportUnavailable()
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
        if (!canSend(state)) return

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

        send(inputMessage = inputMessage, pending = pending, fromComposer = true)
    }

    /**
     * Sends [prompt] as the next user message (suggestion-card tap). Leaves the composer draft and
     * pending photos untouched, so a failed send gives nothing back to the composer.
     */
    internal fun postPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || !canPostPrompt()) return
        send(inputMessage = trimmed, pending = emptyList(), fromComposer = false)
    }

    /**
     * Whether a suggestion card can send now: when the composer could, no livechat handover or
     * close is in flight, and no earlier card's prompt is still on its way to the agent. The cards
     * are enabled from it.
     */
    internal fun canPostPrompt(): Boolean {
        val state = uiState.value
        return canSend(state) && !state.livechat.isBusy && !state.livechat.isPromptSending
    }

    // A ready session that is not being replaced (there is nowhere to send to yet), no photo still
    // encoding and no reply in flight. Whether a livechat session is open is not known until the
    // bootstrap read answers.
    private fun canSend(state: ChatbotUiState): Boolean =
        state.session.connectionState is ChatConnectionState.Ready &&
            !state.isResettingConversation &&
            !state.isEncodingAttachment &&
            state.pendingBotMessage == PendingBotMessageState.None &&
            !state.livechat.isBootstrapping

    // [fromComposer] says whether [inputMessage] and [pending] were taken from the composer, which
    // a failed send then gives back.
    private fun send(
        inputMessage: String,
        pending: List<PendingComposerAttachment>,
        fromComposer: Boolean
    ) {
        fetchLatestMessageIdJob?.cancel()
        if (sendsToAgent) {
            livechatSession.endTyping()
            postLivechatMessage(inputMessage, pending, fromComposer)
        } else {
            sendJob?.cancel()
            // The assistant reads one photo per message; any others, left from a draft for a live
            // agent, stay for the next send.
            postAssistantMessage(
                inputMessage = inputMessage,
                pending = pending.take(ChatbotUiStateHolder.MAX_ASSISTANT_ATTACHMENTS),
                kept = pending.drop(ChatbotUiStateHolder.MAX_ASSISTANT_ATTACHMENTS),
                fromComposer = fromComposer
            )
        }
    }

    /**
     * Arms a `request_image_upload` pick: checks the attach gate, then stores [maxSizeBytes] for
     * the next [onComposerPhotoPicked]. Returns true when the Photo Picker should open.
     */
    internal fun onImageUploadPromptAddPhoto(maxSizeBytes: Int): Boolean {
        if (!canAttachConsideringLivechat()) {
            uiStateHolder.onPhotoAttachFailed()
            return false
        }
        pendingImageUploadMaxBytes = maxSizeBytes
        return true
    }

    /**
     * Encodes a composer photo pick. [open] is called off the main thread for EXIF, bounds and
     * decode streams.
     */
    internal fun onComposerPhotoPicked(open: () -> InputStream?) {
        if (!canAttachConsideringLivechat()) {
            pendingImageUploadMaxBytes = null
            uiStateHolder.onPhotoAttachFailed()
            return
        }
        val promptMax = pendingImageUploadMaxBytes
        pendingImageUploadMaxBytes = null
        val livechat = uiState.value.livechat
        val livechatMax =
            if (livechat.config.attachmentsEnabled && livechat.status.isInSession) {
                livechat.config.maxAttachmentSizeBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            } else {
                null
            }
        val maxDecoded = listOfNotNull(promptMax, livechatMax).minOrNull()
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

    /** Clears a `request_image_upload` budget when the Photo Picker is dismissed without a pick. */
    internal fun onComposerPhotoPickDismissed() {
        pendingImageUploadMaxBytes = null
    }

    internal fun onRemovePendingAttachment(id: String) {
        uiStateHolder.removePendingAttachment(id)
    }

    /**
     * Host offers photos, fewer are pending than the message can carry (one for the assistant,
     * [ChatbotUiStateHolder.MAX_PENDING_ATTACHMENTS] for a live agent), nothing is encoding, no
     * reply is streaming, and no reset is replacing the conversation.
     */
    internal fun canAttach(): Boolean {
        val state = uiState.value
        val cap = if (state.livechat.status == LivechatState.Status.ACTIVE) {
            ChatbotUiStateHolder.MAX_PENDING_ATTACHMENTS
        } else {
            ChatbotUiStateHolder.MAX_ASSISTANT_ATTACHMENTS
        }
        return offersAttachments &&
            state.pendingAttachments.size < cap &&
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
            val restorePoint = livechatRestorePoint()
            uiStateHolder.onDeleteChatDataStarted()
            teardownLivechat()
            deleteChatDataUseCase().onSuccess {
                clearConversationRatingGate()
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
        composerPhotoJob?.cancel()
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
                clearConversationRatingGate()
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
        composerPhotoJob?.cancel()
        exportJob?.cancel()
        forgetShownMessages()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            teardownLivechat()
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    waitingFormCoordinator.configure(config.livechatWaitingFormId)
                    loadBanners()
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

    /**
     * Soft-fails to an empty list on transport / missing-route / decode so an older API does not
     * brick bootstrap. A 401 ends the session like send / export.
     */
    private suspend fun loadBanners() {
        fetchBannersUseCase(pageUrl = currentPage)
            .onSuccess(uiStateHolder::setBanners)
            .onFailure { error ->
                when (error) {
                    is ChatbotException.SessionExpired,
                    is ChatbotException.NoTokenAvailable -> {
                        uiStateHolder.setBanners(emptyList())
                        uiStateHolder.onSessionExpired()
                    }
                    else -> {
                        ChatbotLogger.logError(error)
                        uiStateHolder.setBanners(emptyList())
                    }
                }
            }
    }

    internal fun dismissBanner(banner: ChatBanner) {
        uiStateHolder.dismissBanner(banner)
    }

    internal fun retry() {
        if (uiState.value.session.connectionState is ChatConnectionState.SessionExpired) {
            invalidateSessionUseCase()
        }
        initialize()
    }

    /**
     * A history page failed. When the visitor session has ended, the chat expires the same way as
     * on other 401s, livechat included, and the expiry screen's Retry starts a new session. Only a
     * Ready chat expires: a failure replayed while the chat reconnects is ignored.
     */
    internal fun onHistoryLoadFailed(error: Throwable) {
        if (uiState.value.session.connectionState !is ChatConnectionState.Ready) return
        when (error) {
            is ChatbotException.SessionExpired,
            is ChatbotException.NoTokenAvailable -> expireLivechat()
            else -> Unit
        }
    }

    private fun postAssistantMessage(
        inputMessage: String,
        pending: List<PendingComposerAttachment>,
        kept: List<PendingComposerAttachment>,
        fromComposer: Boolean
    ) {
        val images = pending.map { it.image }
        val bubbles = outgoingUserBubbles(text = inputMessage, images = images)
        val bubbleIds = bubbles.mapTo(HashSet()) { it.id }
        val restoredInput = inputMessage.takeIf { fromComposer }
        sendJob = viewModelScope.launch {
            try {
                uiStateHolder.onSendStarted(bubbles, clearComposer = fromComposer)
                ratingCoordinator.onVisitorMessage()
                kept.forEach(uiStateHolder::addPendingAttachment)
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
                            restoredInput = restoredInput,
                            sentBubbleIds = bubbleIds,
                            restoredAttachments = pending
                        )
                    }
            } catch (_: ChatbotException.SessionExpired) {
                expireLivechat()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireLivechat()
            } catch (e: CancellationException) {
                // A deliberate cancel, not a failed send.
                throw e
            } catch (_: Exception) {
                uiStateHolder.onRetryableError(
                    failedBubbleIds = bubbleIds,
                    restoredInput = restoredInput,
                    restoredAttachments = pending
                )
                conversationRatingCoordinator.onVisitorMessageFailed()
            } finally {
                uiStateHolder.onReplyFinished()
                showDeferredLivechat()
            }
        }
    }

    private fun postLivechatMessage(
        inputMessage: String,
        pending: List<PendingComposerAttachment>,
        fromComposer: Boolean
    ) {
        val images = pending.map { it.image }
        val failed = if (fromComposer) failedLivechatSend else failedPromptSend
        val operationId = failed
            ?.takeIf { it.text == inputMessage && it.images == images }
            ?.operationId
            ?: UUID.randomUUID().toString()
        if (fromComposer) failedLivechatSend = null else failedPromptSend = null
        val bubbles = outgoingUserBubbles(text = inputMessage, images = images)
        val bubbleIds = bubbles.mapTo(HashSet()) { it.id }
        val previous = lastLivechatSend
        livechatSendsInFlight += 1
        lastLivechatSend = viewModelScope.launch(livechatSends) {
            try {
                // Further card taps are held until this prompt has reached the agent.
                if (!fromComposer) uiStateHolder.onLivechatPromptSending(true)
                uiStateHolder.onSendStarted(bubbles, clearComposer = fromComposer)
                // Livechat has no streaming reply bubble; clear the awaiting state immediately.
                uiStateHolder.onReplyFinished()
                previous?.join()
                val stored = sendLivechatMessageUseCase(
                    message = inputMessage,
                    images = images,
                    currentPage = currentPage,
                    clientOperationId = operationId
                ).getOrThrow()
                // The server answers a resend with the message it stored the first time. When that
                // copy is already on screen, this send's bubbles would show the message twice.
                if (!seenLivechatMessageIds.add(stored.messageId)) {
                    uiStateHolder.onLivechatSendWithdrawn(bubbleIds)
                }
                raiseLivechatHead(stored.createdAt)
                visitorHasSentMessage = true
            } catch (_: ChatbotException.SessionExpired) {
                expireLivechat()
            } catch (_: ChatbotException.NoTokenAvailable) {
                expireLivechat()
            } catch (e: CancellationException) {
                // A cancelled send leaves no bubble for a message that was never confirmed.
                uiStateHolder.onLivechatSendWithdrawn(bubbleIds)
                throw e
            } catch (_: Exception) {
                val failedSend = FailedLivechatSend(
                    text = inputMessage,
                    images = images,
                    operationId = operationId
                )
                if (fromComposer) failedLivechatSend = failedSend else failedPromptSend = failedSend
                uiStateHolder.onLivechatSendFailed(
                    failedBubbleIds = bubbleIds,
                    restoredInput = inputMessage.takeIf { fromComposer },
                    restoredAttachments = pending
                )
            } finally {
                if (!fromComposer) uiStateHolder.onLivechatPromptSending(false)
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
        uiStateHolder.onLivechatBootstrapping(true)
        livechatBootstrapJob = viewModelScope.launch {
            try {
                val outcome = if (isRestore) livechatSession.bootstrap() else livechatSession.pickUp()
                when (outcome) {
                    LivechatBootstrapOutcome.SESSION_EXPIRED -> expireLivechat()
                    LivechatBootstrapOutcome.IN_SESSION,
                    LivechatBootstrapOutcome.NOT_IN_SESSION -> Unit
                }
            } finally {
                // A newer read that is still running keeps sends held.
                val latest = livechatBootstrapJob
                if (latest == null || latest == coroutineContext[Job] || !latest.isActive) {
                    uiStateHolder.onLivechatBootstrapping(false)
                }
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
        exportJob?.cancel()
        clearConversationRatingGate()
        uiStateHolder.onSessionExpired()
        viewModelScope.launch { teardownLivechat() }
    }

    private fun clearConversationRatingGate() {
        visitorHasSentMessage = false
        conversationRatingCoordinator.clear()
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
        failedPromptSend = null
        uiStateHolder.onLivechatReset()
    }

    /**
     * Intercepts sheet dismiss. Returns true when the host may call `onDismiss` now; false when a
     * conversation-rating dialog was shown instead (or is already open).
     */
    internal fun onSheetDismissRequested(): Boolean =
        conversationRatingCoordinator.onDismissRequested()

    /**
     * The host presented the sheet afresh, not across a configuration change. Drops a rating
     * dismiss no sheet collected, so it cannot close this one, and closes a rating prompt the
     * earlier sheet left open; a rating that was on its way finishes without dismissing this one.
     */
    internal fun onNewPresentation() {
        _dismissEvents.tryReceive()
        conversationRatingCoordinator.onNewPresentation()
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

    internal fun onConversationRatingScore(score: Int) {
        conversationRatingCoordinator.selectScore(score)
    }

    internal fun onConversationRatingCommentChanged(value: String) {
        conversationRatingCoordinator.setComment(value)
    }

    internal fun onConversationRatingSend() {
        conversationRatingCoordinator.send()
    }

    internal fun onConversationRatingSkip() {
        conversationRatingCoordinator.skip()
    }

    internal fun onConversationRatingCancel() {
        conversationRatingCoordinator.cancel()
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
            val name = agentName.trimToNull() ?: ""
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
        uiStateHolder.onLivechatMessages(
            added.asReversed().flatMap { it.toMessage().mapToUi(offersAttachments = offersAttachments) }
        )
    }

    // A created_at that does not parse has no place in the order, so it is never taken as shown.
    private fun LivechatMessage.isNewerThan(shownThrough: Long): Boolean {
        val createdAt = createdAt.toEpochMillisOrNull() ?: return true
        return createdAt > shownThrough
    }

    private fun handleStreamEvent(
        event: StreamEvent,
        restoredInput: String?,
        sentBubbleIds: Set<String>,
        restoredAttachments: List<PendingComposerAttachment>
    ) {
        when (event) {
            is StreamDeltaEvent -> {
                val parts = streamingMessageBuilder.process(event)
                if (parts.isNotEmpty()) {
                    uiStateHolder.onReplyStreaming(parts)
                }
            }

            is StreamPartEvent -> event.part.mapToUi(offersAttachments = offersAttachments)?.let { part ->
                formCoordinator.ensureFormPart(part)
                uiStateHolder.onReplyPartFinished(part)
            }

            is StreamDoneEvent -> {
                uiStateHolder.onLastMessageReceived(event.message.id)
                uiStateHolder.onReplyFinished()
                visitorHasSentMessage = true
            }


            is StreamStatusEvent -> Unit

            is StreamErrorEvent -> {
                streamingMessageBuilder.reset()
                uiStateHolder.onRetryableError(
                    failedBubbleIds = sentBubbleIds,
                    restoredInput = restoredInput,
                    restoredAttachments = restoredAttachments,
                    serverMessage = event.message
                )
                conversationRatingCoordinator.onVisitorMessageFailed()
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
            attachments: ChatbotAttachments = ChatbotAttachments.DISABLED,
            rating: ChatbotRating = ChatbotRating.ENABLED
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
                    exportMyDataUseCase = dependencies.exportMyDataUseCase,
                    fetchBannersUseCase = dependencies.fetchBannersUseCase,
                    getMessageHistoryUseCase = dependencies.getMessageHistoryUseCase,
                    getLatestMessageUseCase = dependencies.getLatestMessageUseCase,
                    streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
                    uiStateHolder = ChatbotUiStateHolder(),
                    currentPage = currentPage,
                    offersAttachments = attachments == ChatbotAttachments.PHOTO_LIBRARY,
                    encodeDispatcher = Dispatchers.Default,
                    createLivechatSession = { scope -> dependencies.createLivechatSession(scope) },
                    submitActionUseCase = dependencies.submitActionUseCase,
                    fetchFormUseCase = dependencies.fetchFormUseCase,
                    getChatSessionUseCase = dependencies.getChatSessionUseCase,
                    patchFormValuesUseCase = dependencies.patchFormValuesUseCase,
                    submitLivechatFeedbackUseCase = dependencies.submitLivechatFeedbackUseCase,
                    rateConversationUseCase = dependencies.rateConversationUseCase,
                    ratingEnabled = rating == ChatbotRating.ENABLED
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
