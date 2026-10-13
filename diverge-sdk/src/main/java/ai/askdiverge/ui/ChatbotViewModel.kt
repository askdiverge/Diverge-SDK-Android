package ai.askdiverge.ui

import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.di.ChatbotDependencies
import ai.askdiverge.domain.exception.ChatbotException
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
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageIdUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.mapper.toUserMessageItem
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.streaming.DefaultPartBuilderFactory
import ai.askdiverge.ui.streaming.StreamingMessageBuilder
import android.util.Log
import androidx.paging.flatMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

private const val HISTORY_PAGE_SIZE = 10
private const val HISTORY_PREFETCH_DISTANCE = 3
private const val HISTORY_INITIAL_LOAD_SIZE = 10

internal class ChatbotViewModel(
    private val initializeUseCase: InitializeUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val invalidateSessionUseCase: InvalidateSessionUseCase,
    private val getSessionInvalidationsUseCase: GetSessionInvalidationsUseCase,
    private val renewTokenUseCase: RenewTokenUseCase,
    private val deleteChatDataUseCase: DeleteChatDataUseCase,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
    private val getLatestMessageIdUseCase: GetLatestMessageIdUseCase,
    private val streamingMessageBuilder: StreamingMessageBuilder,
    private val uiStateHolder: ChatbotUiStateHolder,
    private val currentPage: String?
) : ViewModel() {

    internal val uiState: StateFlow<ChatbotUiState> = uiStateHolder.uiState

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

    init {
        initialize()
        viewModelScope.launch {
            getSessionInvalidationsUseCase().collect {
                invalidateSessionUseCase()
                initialize()
            }
        }

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

    internal fun postMessage() {
        val inputMessage = uiState.value.inputMessage
        if (uiState.value.session.connectionState !is ChatConnectionState.Ready) return
        // The session is being replaced, so there is nowhere to send it yet.
        if (uiState.value.isResettingConversation) return

        if (inputMessage.isBlank() || uiState.value.pendingBotMessage != PendingBotMessageState.None) return

        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
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
            } catch (e: ChatbotException.SessionExpired) {
                Log.e("Diverge", e.message ?: "")
                uiStateHolder.onSessionExpired()
            } catch (e: ChatbotException.NoTokenAvailable) {
                Log.e("Diverge", e.message ?: "")
                uiStateHolder.onSessionExpired()
            } catch (e: CancellationException) {
                // A deliberate cancel, not a failed send.
                throw e
            } catch (e: Exception) {
                // Anything the stream throws, rather than taking the app down with it.
                Log.e("Diverge", e.message ?: "")
                uiStateHolder.onRetryableError(restoredInput = inputMessage)
            } finally {
                uiStateHolder.onReplyFinished()
            }
        }
    }

    internal fun deleteChatData() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            uiStateHolder.onDeleteChatDataStarted()
            deleteChatDataUseCase().onSuccess {
                uiStateHolder.onDeleteChatDataSucceeded()
                uiStateHolder.onRestartStarted()
                invalidateSessionUseCase()
                startSessionUseCase().onSuccess {
                    uiStateHolder.onRestartFinished()
                }.onFailure { error ->
                    Log.e("Diverge", error.message ?: "")
                    uiStateHolder.onRestartFailed(error.message.orEmpty())
                }
            }.onFailure { error ->
                Log.e("Diverge", error.message ?: "")
                uiStateHolder.onDeleteChatDataFailed()
            }
        }
    }

    internal fun resetConversation() {
        fetchLatestMessageIdJob?.cancel()
        sendJob?.cancel()
        viewModelScope.launch {
            uiStateHolder.onConversationResetStarted()
            renewTokenUseCase().onSuccess {
                uiStateHolder.onRestartFinished()
                resyncEvent.emit(Unit)
            }.onFailure { error ->
                Log.e("Diverge", error.message ?: "")
                uiStateHolder.onConversationResetFailed()
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
                .onFailure { error ->
                    Log.e("Diverge", error.message ?: "")
                    reloadHistory(latestMessageId = null)
                }
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
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
                    getLatestMessageIdUseCase()
                        .onSuccess(uiStateHolder::onLastMessageReceived)
                        .onFailure { Log.e("Diverge", it.message ?: "")}
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

            is StreamPartEvent -> event.part.mapToUi()?.let(uiStateHolder::onReplyPartFinished)

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
            currentPage: String? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val dependencies = ChatbotDependencies(chatbotCallbacks = chatbotCallbacks)
                ChatbotViewModel(
                    initializeUseCase = dependencies.initializeUseCase,
                    sendMessageUseCase = dependencies.sendMessageUseCase,
                    startSessionUseCase = dependencies.startSessionUseCase,
                    invalidateSessionUseCase = dependencies.invalidateSessionUseCase,
                    getSessionInvalidationsUseCase = dependencies.getSessionInvalidationsUseCase,
                    renewTokenUseCase = dependencies.renewTokenUseCase,
                    deleteChatDataUseCase = dependencies.deleteChatDataUseCase,
                    getMessageHistoryUseCase = dependencies.getMessageHistoryUseCase,
                    getLatestMessageIdUseCase = dependencies.getLatestMessageIdUseCase,
                    streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
                    uiStateHolder = ChatbotUiStateHolder(),
                    currentPage = currentPage
                )
            }
        }
    }
}
