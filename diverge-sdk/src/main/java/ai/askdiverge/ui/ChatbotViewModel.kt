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
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
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
    private val renewTokenUseCase: RenewTokenUseCase,
    private val deleteChatDataUseCase: DeleteChatDataUseCase,
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase,
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

    init {
        initialize()
    }

    // The ViewModel outlives a single presentation of the chat, so another one may have continued
    // the conversation, reset it or deleted it in the meantime. Every open therefore re-reads the
    // conversation from the session instead of replaying the state left behind by the previous one.
    internal fun onSheetOpened() {
        when (uiState.value.session.connectionState) {
            is ChatConnectionState.Error,
            is ChatConnectionState.SessionExpired -> retry()

            is ChatConnectionState.Ready -> resyncConversation()

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

    // A reply in flight has not reached the session yet, so refreshing would drop it. The next
    // open resyncs instead.
    private fun resyncConversation() {
        if (uiState.value.pendingBotMessage != PendingBotMessageState.None) return
        uiStateHolder.onConversationResynced()
        viewModelScope.launch { resyncEvent.emit(Unit) }
    }

    private fun initialize() {
        initJob?.cancel()
        sendJob?.cancel()
        uiStateHolder.onConnectionStarted()
        initJob = viewModelScope.launch {
            initializeUseCase()
                .onSuccess { config ->
                    uiStateHolder.onConfigLoaded(config)
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

            is StreamDoneEvent -> uiStateHolder.onReplyFinished()

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
                    renewTokenUseCase = dependencies.renewTokenUseCase,
                    deleteChatDataUseCase = dependencies.deleteChatDataUseCase,
                    getMessageHistoryUseCase = dependencies.getMessageHistoryUseCase,
                    streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
                    uiStateHolder = ChatbotUiStateHolder(),
                    currentPage = currentPage
                )
            }
        }
    }
}
