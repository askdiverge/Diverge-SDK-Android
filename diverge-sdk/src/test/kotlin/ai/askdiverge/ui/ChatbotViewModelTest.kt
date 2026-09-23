package ai.askdiverge.ui

import androidx.paging.PagingData
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.StreamErrorEvent
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarState
import ai.askdiverge.ui.state.SnackbarType
import ai.askdiverge.ui.streaming.DefaultPartBuilderFactory
import ai.askdiverge.ui.streaming.StreamingMessageBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatbotViewModelTest {

    private val initializeUseCase: InitializeUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(sampleConfig())
    }
    private val sendMessageUseCase = mockk<SendMessageUseCase>()
    private val startSessionUseCase: StartSessionUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(Unit)
    }
    private val invalidateSessionUseCase = mockk<InvalidateSessionUseCase>(relaxed = true)
    private val renewTokenUseCase = mockk<RenewTokenUseCase>()
    private val deleteChatDataUseCase = mockk<DeleteChatDataUseCase>()
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase = mockk {
        every { this@mockk.invoke(pagingConfig = any()) } returns flowOf(PagingData.empty())
    }

    private val collectorScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        collectorScope.cancel()
        Dispatchers.resetMain()
    }

    private fun createViewModel(currentPage: String? = null): ChatbotViewModel = ChatbotViewModel(
        initializeUseCase = initializeUseCase,
        sendMessageUseCase = sendMessageUseCase,
        startSessionUseCase = startSessionUseCase,
        invalidateSessionUseCase = invalidateSessionUseCase,
        renewTokenUseCase = renewTokenUseCase,
        deleteChatDataUseCase = deleteChatDataUseCase,
        getMessageHistoryUseCase = getMessageHistoryUseCase,
        streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
        uiStateHolder = ChatbotUiStateHolder(),
        currentPage = currentPage
    )

    @Test
    fun `retryable stream error clears the pending message and restores input`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns
            flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        val state = viewModel.uiState.value
        assertTrue(state.conversation.isEmpty())
        assertEquals("hello", state.inputMessage)
        assertEquals(
            SnackbarState(type = SnackbarType.StreamError, text = "Please try again."),
            state.snackbarState
        )
        assertEquals(expected = PendingBotMessageState.None, actual = state.pendingBotMessage)
    }

    @Test
    fun `non-retryable stream error clears the pending message and restores input`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns
            flowOf(StreamErrorEvent(message = "Fatal.", retryable = false))

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        val state = viewModel.uiState.value
        assertTrue(state.conversation.isEmpty())
        assertEquals("hello", state.inputMessage)
        assertEquals(
            SnackbarState(type = SnackbarType.StreamError, text = "Fatal."),
            state.snackbarState
        )
        assertEquals(expected = PendingBotMessageState.None, actual = state.pendingBotMessage)
    }

    @Test
    fun `deleteChatData success restarts the conversation and reports the result`() {
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        val state = viewModel.uiState.value
        assertEquals(false, state.isDeletingChatData)
        assertEquals(
            SnackbarState(type = SnackbarType.DeleteChatDataSucceeded),
            state.snackbarState
        )
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
    }

    @Test
    fun `deleting chat data acquires a fresh token without re-fetching the config`() {
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        // The cache is cleared only after the session is marked Restarting, so no in-flight
        // history load can hit the interceptor with an empty cache while the state still says Ready.
        coVerifyOrder {
            deleteChatDataUseCase()
            invalidateSessionUseCase()
            startSessionUseCase()
        }
        coVerify(exactly = 1) { initializeUseCase() }
    }

    @Test
    fun `deleting chat data closes the sheet it was started from`() {
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.onSheetRequested(SheetType.DeleteChatData)
        viewModel.deleteChatData()

        assertNull(viewModel.uiState.value.activeSheet)
    }

    @Test
    fun `failing to start the session after a delete still confirms the delete succeeded`() {
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)
        coEvery { startSessionUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        val state = viewModel.uiState.value
        assertEquals(
            expected = ChatConnectionState.Error(message = "boom"),
            actual = state.session.connectionState
        )
        assertEquals(
            expected = SnackbarState(type = SnackbarType.DeleteChatDataSucceeded),
            actual = state.snackbarState
        )
    }

    @Test
    fun `deleted messages leave the screen even when the new session fails to start`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns MutableSharedFlow()
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)
        coEvery { startSessionUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        viewModel.deleteChatData()

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `a message typed while the session is restarting is not sent`() {
        val resetResult = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase() } coAnswers { resetResult.await() }

        val viewModel = createViewModel()
        viewModel.resetConversation()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        verify(exactly = 0) { sendMessageUseCase(message = any(), currentPage = any()) }
    }

    @Test
    fun `deleteChatData failure reports the result and stops the progress indicator`() {
        coEvery { deleteChatDataUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        val state = viewModel.uiState.value
        assertEquals(false, state.isDeletingChatData)
        assertEquals(
            SnackbarState(type = SnackbarType.DeleteChatDataFailed),
            state.snackbarState
        )
    }

    @Test
    fun `conversation reset adopts the new token without re-fetching the config`() {
        coEvery { renewTokenUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.resetConversation()

        val state = viewModel.uiState.value
        assertEquals(
            expected = false,
            actual = state.isResettingConversation
        )
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
        // renewToken caches the new token before the session is marked ready, otherwise the history
        // reload authenticates with the token of the session that was just replaced.
        coVerify { renewTokenUseCase() }
        coVerify(exactly = 1) { initializeUseCase() }
    }

    @Test
    fun `conversation reset keeps the config-derived session fields`() {
        coEvery { renewTokenUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.resetConversation()

        coVerify { renewTokenUseCase() }

        val session = viewModel.uiState.value.session
        assertEquals(expected = "Bot", actual = session.assistantName)
        assertEquals(expected = "https://example.com/privacy-policy", actual = session.privacyPolicyUrl)
    }

    @Test
    fun `conversation reset keeps a draft the user had already typed`() {
        val viewModel = createViewModel()

        viewModel.onInputChanged("half typed")
        viewModel.resetConversation()

        assertEquals(expected = "half typed", actual = viewModel.uiState.value.inputMessage)
    }

    @Test
    fun `conversation reset stops collecting a reply that is still streaming`() {
        val events = MutableSharedFlow<StreamEvent>()
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns events

        val viewModel = createViewModel()

        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        viewModel.resetConversation()

        assertEquals(expected = 0, actual = events.subscriptionCount.value)
    }

    @Test
    fun `conversation reset shows the progress indicator while the token call is in flight`() {
        val resetResult = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase() } coAnswers { resetResult.await() }

        val viewModel = createViewModel()
        viewModel.resetConversation()

        assertTrue(viewModel.uiState.value.isResettingConversation)

        resetResult.complete(Result.success(Unit))

        assertEquals(
            expected = false,
            actual = viewModel.uiState.value.isResettingConversation
        )
    }

    @Test
    fun `conversation reset failure reports the result and stops the progress indicator`() {
        coEvery { renewTokenUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.resetConversation()
        val state = viewModel.uiState.value

        assertEquals(
            expected = false,
            actual = state.isResettingConversation
        )
        assertEquals(
            expected = SnackbarState(type = SnackbarType.ConversationResetFailed),
            actual = state.snackbarState
        )
    }

    @Test
    fun `conversation reset failure leaves the chat usable instead of stuck restarting`() {
        coEvery { renewTokenUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.resetConversation()

        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `onSnackbarConsumed clears the snackbar`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns
            flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        viewModel.onSnackbarConsumed()

        assertNull(viewModel.uiState.value.snackbarState)
    }

    @Test
    fun `reopening the chat drops the messages the refreshed history will provide again`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns emptyFlow()

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)

        viewModel.onSheetOpened()

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `reopening the chat while a reply is streaming keeps the messages on screen`() {
        every { sendMessageUseCase(message = "hello", currentPage = null) } returns MutableSharedFlow()

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        viewModel.onSheetOpened()

        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `reopening the chat after a failed connect reconnects`() {
        coEvery { initializeUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.onSheetOpened()

        coVerify(exactly = 2) { initializeUseCase() }
    }

    @Test
    fun `reopening the chat while the first connect is in flight does not start a second one`() {
        val config = CompletableDeferred<Result<Config>>()
        coEvery { initializeUseCase() } coAnswers { config.await() }

        val viewModel = createViewModel()
        viewModel.onSheetOpened()

        coVerify(exactly = 1) { initializeUseCase() }
    }

    @Test
    fun `retrying after a failed connect makes the chat usable again`() {
        coEvery { initializeUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        assertEquals(
            expected = ChatConnectionState.Error(message = "boom"),
            actual = viewModel.uiState.value.session.connectionState
        )

        coEvery { initializeUseCase() } returns Result.success(sampleConfig())
        viewModel.retry()

        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `retrying while the connection keeps failing leaves the chat in error`() {
        coEvery { initializeUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.retry()

        assertEquals(
            expected = ChatConnectionState.Error(message = "boom"),
            actual = viewModel.uiState.value.session.connectionState
        )
        coVerify(exactly = 2) { initializeUseCase() }
    }

    @Test
    fun `when the chat reconnects expect a streaming reply to stop being collected`() {
        val events = MutableSharedFlow<StreamEvent>()
        every {
            sendMessageUseCase(
                message = "hello",
                currentPage = null
            )
        } returns events

        val viewModel = createViewModel()
        viewModel.send()
        viewModel.retry()

        assertEquals(
            expected = 0,
            actual = events.subscriptionCount.value
        )
    }

    @Test
    fun `when the message is blank expect nothing to be sent`() {
        val viewModel = createViewModel()
        viewModel.send("   ")

        verify(exactly = 0) {
            sendMessageUseCase(
                message = any(),
                currentPage = any()
            )
        }
    }

    @Test
    fun `when the chat is still connecting expect the message not to be sent`() {
        val config = CompletableDeferred<Result<Config>>()
        coEvery { initializeUseCase() } coAnswers { config.await() }

        val viewModel = createViewModel()
        viewModel.send()

        verify(exactly = 0) {
            sendMessageUseCase(
                message = any(),
                currentPage = any()
            )
        }
    }

    @Test
    fun `when a reply is still streaming expect a second message not to be sent`() {
        every {
            sendMessageUseCase(
                message = any(),
                currentPage = null
            )
        } returns MutableSharedFlow()

        val viewModel = createViewModel()
        viewModel.send()
        viewModel.send("are you there?")

        verify(exactly = 1) {
            sendMessageUseCase(
                message = "hello",
                currentPage = null
            )
        }
        verify(exactly = 0) {
            sendMessageUseCase(
                message = "are you there?",
                currentPage = null
            )
        }
    }

    @Test
    fun `when text arrives in deltas expect it streamed into the pending bubble`() {
        val events = MutableSharedFlow<StreamEvent>(replay = 1)
        every {
            sendMessageUseCase(
                message = "hello",
                currentPage = null
            )
        } returns events

        val viewModel = createViewModel()
        viewModel.send()
        events.tryEmit(
            StreamDeltaEvent.PartStarted(
                partId = "part-1",
                partType = DeltaPartType.RICH_TEXT
            )
        )
        events.tryEmit(
            StreamDeltaEvent.BlockStarted(
                partId = "part-1",
                blockIndex = 0,
                blockType = DeltaBlockType.PARAGRAPH
            )
        )
        events.tryEmit(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "Sure, "
            )
        )
        events.tryEmit(
            StreamDeltaEvent.TextAppended(
                partId = "part-1",
                blockIndex = 0,
                text = "here you go."
            )
        )

        val streaming = assertIs<PendingBotMessageState.Streaming>(viewModel.uiState.value.pendingBotMessage)
        val part = assertIs<MessagePartUiModel.RichText>(streaming.parts.single())
        val paragraph = assertIs<RichTextBlockUiModel.Paragraph>(part.blocks.single())
        assertEquals(
            expected = listOf("Sure, ", "here you go."),
            actual = paragraph.spans.map { it.text }
        )
    }

    @Test
    fun `when the session expired expect it surfaced to the UI`() {
        every {
            sendMessageUseCase(
                message = "hello",
                currentPage = null
            )
        } returns
            flow { throw ChatbotException.SessionExpired() }

        val viewModel = createViewModel()
        viewModel.send()

        val state = viewModel.uiState.value
        assertEquals(
            expected = ChatConnectionState.SessionExpired,
            actual = state.session.connectionState
        )
        assertEquals(
            expected = PendingBotMessageState.None,
            actual = state.pendingBotMessage
        )
    }

    @Test
    fun `when there is no token expect the session re-established`() {
        every {
            sendMessageUseCase(
                message = "hello",
                currentPage = null
            )
        } returns
            flow { throw ChatbotException.NoTokenAvailable() }

        val viewModel = createViewModel()
        viewModel.send()

        val state = viewModel.uiState.value
        // Resending cannot recover a missing token, so the chat asks for the session to be rebuilt.
        assertEquals(
            expected = ChatConnectionState.SessionExpired,
            actual = state.session.connectionState
        )
        assertEquals(
            expected = PendingBotMessageState.None,
            actual = state.pendingBotMessage
        )
    }

    @Test
    fun `when the session is not ready expect no history to be requested`() {
        val config = CompletableDeferred<Result<Config>>()
        coEvery { initializeUseCase() } coAnswers { config.await() }

        collectHistory(createViewModel())

        verifyHistoryLoads(0)
    }

    @Test
    fun `when the session is ready expect the history to be loaded`() {
        collectHistory(createViewModel())

        verifyHistoryLoads(1)
    }

    @Test
    fun `when the conversation reset has a new token expect the history to be reloaded`() {
        val newToken = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase() } coAnswers { newToken.await() }

        val viewModel = createViewModel()
        collectHistory(viewModel)

        viewModel.resetConversation()

        // Loading history while the token is being replaced would authenticate with the
        // session that is on its way out.
        verifyHistoryLoads(1)

        newToken.complete(Result.success(Unit))

        verifyHistoryLoads(2)
    }

    @Test
    fun `when the chat data is deleted and the new session is up expect the history to be reloaded`() {
        val newSession = CompletableDeferred<Result<Unit>>()
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)
        coEvery { startSessionUseCase() } coAnswers { newSession.await() }

        val viewModel = createViewModel()
        collectHistory(viewModel)

        viewModel.deleteChatData()

        // There is no session to load history with until the new token is in place.
        verifyHistoryLoads(1)

        newSession.complete(Result.success(Unit))

        verifyHistoryLoads(2)
    }

    @Test
    fun `when the chat data is deleted and the new session fails expect no history to be reloaded`() {
        val newSession = CompletableDeferred<Result<Unit>>()
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)
        coEvery { startSessionUseCase() } coAnswers { newSession.await() }

        val viewModel = createViewModel()
        collectHistory(viewModel)

        viewModel.deleteChatData()
        newSession.complete(Result.failure(IllegalStateException("boom")))

        // Loading history without a session would only fail on the missing token; the retry
        // screen is shown instead.
        verifyHistoryLoads(1)
    }

    @Test
    fun `when the chat is reopened expect the history to be reloaded`() {
        val viewModel = createViewModel()
        collectHistory(viewModel)

        viewModel.onSheetOpened()

        verifyHistoryLoads(2)
    }

    @Test
    fun `when the delete call is in flight expect the progress indicator`() {
        val deleteResult = CompletableDeferred<Result<Unit>>()
        coEvery { deleteChatDataUseCase() } coAnswers { deleteResult.await() }

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        assertTrue(viewModel.uiState.value.isDeletingChatData)

        deleteResult.complete(Result.success(Unit))

        assertFalse(viewModel.uiState.value.isDeletingChatData)
    }

    @Test
    fun `when the chat data delete fails expect the session to be kept as it was`() {
        coEvery { deleteChatDataUseCase() } returns Result.failure(IllegalStateException("boom"))

        val viewModel = createViewModel()
        viewModel.deleteChatData()

        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
        verify(exactly = 0) { invalidateSessionUseCase() }
        coVerify(exactly = 0) { startSessionUseCase() }
    }

    private fun ChatbotViewModel.send(message: String = "hello") {
        onInputChanged(message)
        postMessage()
    }

    // The pager is only built while something collects it, so a history test has to keep collecting
    // for as long as it runs. Each rebuild calls the use case once, so verifying it counts loads.
    private fun collectHistory(viewModel: ChatbotViewModel) {
        collectorScope.launch { viewModel.historyMessages.collect() }
    }

    private fun verifyHistoryLoads(times: Int) {
        verify(exactly = times) { getMessageHistoryUseCase(pagingConfig = any()) }
    }
}
