package ai.askdiverge.ui

import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.MessageRole
import androidx.paging.PagingData
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.domain.model.form.SubmitActionResponse
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.event.StreamErrorEvent
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageIdUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.livechat.LivechatSession
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.domain.usecase.SendLivechatMessageUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.livechat.LivechatControl
import ai.askdiverge.ui.model.message.part.LivechatNoteKind
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
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
    private val sessionInvalidations = MutableSharedFlow<Unit>()
    private val getSessionInvalidationsUseCase: GetSessionInvalidationsUseCase = mockk {
        every { this@mockk.invoke() } returns sessionInvalidations
    }

    private val renewTokenUseCase = mockk<RenewTokenUseCase>()
    private val deleteChatDataUseCase = mockk<DeleteChatDataUseCase>()
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase = mockk {
        every { this@mockk.invoke(pagingConfig = any()) } returns flowOf(PagingData.empty())
    }
    private val getLatestMessageIdUseCase: GetLatestMessageIdUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success("message-1")
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
        sendLivechatMessageUseCase = mockk<SendLivechatMessageUseCase>(relaxed = true),
        startSessionUseCase = startSessionUseCase,
        invalidateSessionUseCase = invalidateSessionUseCase,
        getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
        renewTokenUseCase = renewTokenUseCase,
        deleteChatDataUseCase = deleteChatDataUseCase,
        getMessageHistoryUseCase = getMessageHistoryUseCase,
        getLatestMessageIdUseCase = getLatestMessageIdUseCase,
        streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
        uiStateHolder = ChatbotUiStateHolder(),
        currentPage = currentPage,
        apiBaseUrl = "https://api.example/",
        offersAttachments = true,
        createLivechatSession = { scope ->
            LivechatSession(
                repository = mockk<ChatbotRepository>(relaxed = true),
                scope = scope,
                delayMs = { throw CancellationException() }
            )
        },
        submitActionUseCase = mockk(relaxed = true),
        fetchFormUseCase = mockk(relaxed = true),
        getChatSessionUseCase = mockk(relaxed = true),
        patchFormValuesUseCase = mockk(relaxed = true),
        submitLivechatFeedbackUseCase = mockk(relaxed = true)
    )

    @Test
    fun `retryable stream error clears the pending message and restores input`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
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
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
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
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns MutableSharedFlow()
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

        verify(exactly = 0) { sendMessageUseCase(message = any(), images = any(), currentPage = any()) }
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
        coEvery { renewTokenUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.onInputChanged("half typed")
        viewModel.resetConversation()

        assertEquals(expected = "half typed", actual = viewModel.uiState.value.inputMessage)
    }

    @Test
    fun `conversation reset stops collecting a reply that is still streaming`() {
        val events = MutableSharedFlow<StreamEvent>()
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns events
        coEvery { renewTokenUseCase() } returns Result.success(Unit)

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
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
            flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()

        viewModel.onSnackbarConsumed()

        assertNull(viewModel.uiState.value.snackbarState)
    }

    @Test
    fun `reopening a chat that changed elsewhere drops the messages the refreshed history will provide again`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns emptyFlow()

        val viewModel = createViewModel()
        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)

        coEvery { getLatestMessageIdUseCase() } returns Result.success("message-2")
        viewModel.onSheetOpened()

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `reopening an unchanged chat keeps the messages on screen`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns emptyFlow()

        val viewModel = createViewModel()
        viewModel.send()

        viewModel.onSheetOpened()

        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `reopening the chat after its own reply finished keeps the messages on screen`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
                flowOf(StreamDoneEvent(message = sampleMessage(id = "reply-1")))

        val viewModel = createViewModel()
        viewModel.send()

        coEvery { getLatestMessageIdUseCase() } returns Result.success("reply-1")
        viewModel.onSheetOpened()

        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `when checking the chat for changes fails expect the conversation to be re-read`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns emptyFlow()

        val viewModel = createViewModel()
        collectHistory(viewModel)
        viewModel.send()

        coEvery { getLatestMessageIdUseCase() } returns Result.failure(IllegalStateException("boom"))
        viewModel.onSheetOpened()

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
        verifyHistoryLoads(2)
    }

    @Test
    fun `when the chat is reopened during a conversation reset expect only the reset to reload the history`() {
        val newToken = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase() } coAnswers { newToken.await() }

        val viewModel = createViewModel()
        collectHistory(viewModel)
        viewModel.resetConversation()

        coEvery { getLatestMessageIdUseCase() } returns Result.success("message-2")
        viewModel.onSheetOpened()
        newToken.complete(Result.success(Unit))

        verifyHistoryLoads(2)
    }

    @Test
    fun `a message sent while the chat is checked for changes is not wiped by the check`() {
        val latestMessageId = CompletableDeferred<Result<String?>>()
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns MutableSharedFlow()

        val viewModel = createViewModel()
        coEvery { getLatestMessageIdUseCase() } coAnswers { latestMessageId.await() }
        viewModel.onSheetOpened()
        viewModel.send()

        latestMessageId.complete(Result.success("message-2"))

        assertEquals(expected = 1, actual = viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `reopening the chat while a reply is streaming keeps the messages on screen`() {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns MutableSharedFlow()

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
                images = any(),
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
                images = any(),
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
                images = any(),
                currentPage = any()
            )
        }
    }

    @Test
    fun `when a reply is still streaming expect a second message not to be sent`() {
        every {
            sendMessageUseCase(
                message = any(),
                images = any(),
                currentPage = null
            )
        } returns MutableSharedFlow()

        val viewModel = createViewModel()
        viewModel.send()
        viewModel.send("are you there?")

        verify(exactly = 1) {
            sendMessageUseCase(
                message = "hello",
                images = any(),
                currentPage = null
            )
        }
        verify(exactly = 0) {
            sendMessageUseCase(
                message = "are you there?",
                images = any(),
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
                images = any(),
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
                images = any(),
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
                images = any(),
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
    fun `when the chat is reopened after it changed elsewhere expect the history to be reloaded`() {
        val viewModel = createViewModel()
        collectHistory(viewModel)

        coEvery { getLatestMessageIdUseCase() } returns Result.success("message-2")
        viewModel.onSheetOpened()

        verifyHistoryLoads(2)
    }

    @Test
    fun `when the chat is reopened unchanged expect the history not to be reloaded`() {
        val viewModel = createViewModel()
        collectHistory(viewModel)

        viewModel.onSheetOpened()

        verifyHistoryLoads(1)
    }

    @Test
    fun `when the host invalidates the session expect the token dropped before reconnecting`() = runTest {
        val viewModel = createViewModel()

        sessionInvalidations.emit(Unit)

        coVerifyOrder {
            invalidateSessionUseCase()
            initializeUseCase()
        }
        coVerify(exactly = 2) { initializeUseCase() }
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `when the host invalidates the session expect the previous conversation cleared`() = runTest {
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns MutableSharedFlow()

        val viewModel = createViewModel()
        viewModel.send()

        sessionInvalidations.emit(Unit)

        val state = viewModel.uiState.value
        assertTrue(state.conversation.isEmpty())
        assertEquals(expected = PendingBotMessageState.None, actual = state.pendingBotMessage)
    }

    @Test
    fun `when the host invalidates the session expect the history reloaded with the new session`() = runTest {
        val viewModel = createViewModel()
        collectHistory(viewModel)

        sessionInvalidations.emit(Unit)

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

    @Test
    fun `a livechat message synced while its send is in flight is shown once`() = runTest {
        val tick = Channel<Unit>()
        val stored = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            stored.await()
            Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        }
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(livechatMessage(id = "server-1", role = MessageRole.USER))
        )
        val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }

        viewModel.onInputChanged("hi")
        viewModel.postMessage()
        tick.send(Unit)
        stored.complete(Unit)

        assertEquals(1, viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `a second livechat send waits for the first instead of cancelling it`() = runTest {
        val firstStored = CompletableDeferred<Unit>()
        val sent = mutableListOf<String>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "one", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            firstStored.await()
            sent += "one"
            Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        }
        coEvery { sendLivechat(message = "two", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            sent += "two"
            Result.success(livechatMessage(id = "server-2", role = MessageRole.USER))
        }
        val viewModel = createLivechatViewModel(livechatRepository(livechatSync()), sendLivechat) {
            awaitCancellation()
        }

        viewModel.onInputChanged("one")
        viewModel.postMessage()
        viewModel.onInputChanged("two")
        viewModel.postMessage()
        firstStored.complete(Unit)

        assertEquals(listOf("one", "two"), sent)
    }

    @Test
    fun `retrying a failed livechat send reuses its client operation id`() = runTest {
        val operationIds = mutableListOf<String>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = capture(operationIds))
        } returns Result.failure(IOException("timeout")) andThen
            Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }

        viewModel.onInputChanged("hi")
        viewModel.postMessage()
        viewModel.postMessage()

        assertEquals(2, operationIds.size)
        assertEquals(operationIds[0], operationIds[1])
    }

    @Test
    fun `the livechat log found when the chat opens is not added on top of history`() = runTest {
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync(livechatMessage(id = "m1", role = MessageRole.AGENT))),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        // Neither the restored log nor a note about how the session started.
        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `accepting a human agent marker hands over with its part id`() = runTest {
        val inactive = LivechatSync(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val waiting = LivechatSync(
            state = LivechatState.of(LivechatState.Status.WAITING),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(inactive)
        ) andThen Result.success(waiting)
        coEvery { repository.requestLivechatHandover(any()) } returns Result.success(
            LivechatState.Status.WAITING
        )
        val viewModel = createLivechatViewModel(
            repository,
            mockk(relaxed = true)
        ) { awaitCancellation() }

        viewModel.onAcceptHumanAgent("marker-9")

        coVerify {
            repository.requestLivechatHandover(
                match {
                    it.partId == "marker-9" &&
                        it.source == LivechatHandoverRequest.Source.ASSISTANT_MARKER
                }
            )
        }
        assertEquals(LivechatState.Status.WAITING, viewModel.uiState.value.livechat.status)
        assertEquals(LivechatControl.END, viewModel.uiState.value.livechat.control)
    }

    @Test
    fun `declining a human agent marker disables that card`() = runTest {
        val viewModel = createLivechatViewModel(
            livechatRepository(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.INACTIVE),
                    messages = emptyList(),
                    hasMore = false,
                    syncCursor = null
                )
            ),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        viewModel.onDeclineHumanAgent("marker-2")

        assertTrue("marker-2" in viewModel.uiState.value.livechat.declinedHumanAgentPartIds)
        assertFalse(viewModel.canAcceptHumanAgent("marker-2"))
    }

    @Test
    fun `waiting then active snapshots insert queued and agent joined notes`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val inactive = LivechatSync(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val active = LivechatSync(
            state = LivechatState(
                status = LivechatState.Status.ACTIVE,
                activeAgent = LivechatAgent(
                    agentId = "a1",
                    displayName = "Alex",
                    avatarUrl = null
                ),
                isAgentTyping = false,
                agentJoinedAt = "2026-01-01T00:00:00Z",
                closedBy = null,
                closedByAgentId = null,
                closeReason = null,
                feedback = LivechatState.Feedback(LivechatState.Feedback.Status.NOT_AVAILABLE),
                stateVersion = 2
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(inactive),
            Result.success(active)
        ) andThen Result.success(active)
        coEvery { repository.requestLivechatHandover(any()) } returns Result.success(
            LivechatState.Status.WAITING
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            tick.receive()
        }

        assertEquals(LivechatControl.START, viewModel.uiState.value.livechat.control)
        viewModel.onLivechatControlClick()
        // Handover starts an immediate poll; release the delayed tick that follows.
        tick.send(Unit)

        val kinds = viewModel.uiState.value.conversation.mapNotNull { bubble ->
            (bubble.part as? MessagePartUiModel.LivechatNote)?.kind
        }
        assertTrue(LivechatNoteKind.QUEUED in kinds)
        assertTrue(LivechatNoteKind.AGENT_JOINED in kinds)
        assertEquals("Alex", viewModel.uiState.value.livechat.agentDisplayName)
    }

    @Test
    fun `a waiting visitor's message goes to the assistant`() = runTest {
        every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns emptyFlow()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync(status = LivechatState.Status.WAITING)),
            sendLivechat
        ) { awaitCancellation() }

        viewModel.onInputChanged("hi")
        viewModel.postMessage()

        verify { sendMessageUseCase(message = "hi", images = any(), currentPage = null) }
        coVerify(exactly = 0) { sendLivechat(any(), any(), any(), any()) }
    }

    @Test
    fun `input while active tells the livechat session the visitor is typing`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }

        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
        viewModel.onInputChanged("hello agent")

        coVerify { repository.sendLivechatTyping(true) }
    }

    @Test
    fun `a waiting form loads while queued and clears when an agent joins`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val inactive = livechatSync(status = LivechatState.Status.INACTIVE)
        val waiting = livechatSync(status = LivechatState.Status.WAITING)
        val active = LivechatSync(
            state = LivechatState(
                status = LivechatState.Status.ACTIVE,
                activeAgent = LivechatAgent(
                    agentId = "a1",
                    displayName = "Alex",
                    avatarUrl = null
                ),
                isAgentTyping = false,
                agentJoinedAt = "2026-01-01T00:00:00Z",
                closedBy = null,
                closedByAgentId = null,
                closeReason = null,
                feedback = LivechatState.Feedback(LivechatState.Feedback.Status.NOT_AVAILABLE),
                stateVersion = 2
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(inactive),
            Result.success(waiting),
            Result.success(active)
        ) andThen Result.success(active)
        coEvery { repository.requestLivechatHandover(any()) } returns Result.success(
            LivechatState.Status.WAITING
        )
        coEvery { repository.fetchForm("wait-form") } returns Result.success(
            ai.askdiverge.domain.model.form.ChatFormDefinition(
                formId = "wait-form",
                name = "Waiting",
                confirmationText = null,
                fields = listOf(
                    ai.askdiverge.domain.model.form.FormField(
                        key = "email",
                        label = "Email",
                        placeholder = null,
                        type = ai.askdiverge.domain.model.form.FormField.FieldType.EMAIL,
                        required = false,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                ),
                minFilledFields = 0
            )
        )
        coEvery { repository.getSession() } returns Result.success(
            ai.askdiverge.domain.model.form.ChatSessionState(emptyList())
        )
        coEvery { initializeUseCase() } returns Result.success(
            sampleConfig(
                livechat = LivechatConfig.serverDefaults.copy(
                    enabled = true,
                    configured = true,
                    availabilityStatus = LivechatConfig.AvailabilityStatus.LIVE,
                    showLivechatLogo = true
                ),
                forms = listOf(
                    ai.askdiverge.domain.model.form.ChatFormReference(
                        formId = "wait-form",
                        name = "Waiting",
                        trigger = ai.askdiverge.domain.model.form.ChatFormReference.Trigger.LIVECHAT_WAITING
                    )
                )
            )
        )
        val viewModel = ChatbotViewModel(
            initializeUseCase = initializeUseCase,
            sendMessageUseCase = sendMessageUseCase,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            startSessionUseCase = startSessionUseCase,
            invalidateSessionUseCase = invalidateSessionUseCase,
            getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
            renewTokenUseCase = renewTokenUseCase,
            deleteChatDataUseCase = deleteChatDataUseCase,
            getMessageHistoryUseCase = getMessageHistoryUseCase,
            getLatestMessageIdUseCase = getLatestMessageIdUseCase,
            streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
            uiStateHolder = ChatbotUiStateHolder(),
            currentPage = null,
            apiBaseUrl = "https://api.example/",
            offersAttachments = true,
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = { tick.receive() })
            },
            submitActionUseCase = mockk(relaxed = true),
            fetchFormUseCase = ai.askdiverge.domain.usecase.FetchFormUseCase(repository),
            getChatSessionUseCase = ai.askdiverge.domain.usecase.GetChatSessionUseCase(repository),
            patchFormValuesUseCase = ai.askdiverge.domain.usecase.PatchFormValuesUseCase(repository),
            submitLivechatFeedbackUseCase = mockk(relaxed = true)
        )

        viewModel.onLivechatControlClick()

        // Handover publishes WAITING immediately; the first poll (also waiting) does not need a tick.
        assertEquals(LivechatState.Status.WAITING, viewModel.uiState.value.livechat.status)
        assertNotNull(viewModel.uiState.value.livechat.waitingForm)

        tick.send(Unit)
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
        assertNull(viewModel.uiState.value.livechat.waitingForm)
    }

    @Test
    fun `an active snapshot with agent typing sets the typing flag`() = runTest {
        val typing = LivechatSync(
            state = LivechatState(
                status = LivechatState.Status.ACTIVE,
                activeAgent = LivechatAgent(
                    agentId = "a1",
                    displayName = "Alex",
                    avatarUrl = null
                ),
                isAgentTyping = true,
                agentJoinedAt = "2026-01-01T00:00:00Z",
                closedBy = null,
                closedByAgentId = null,
                closeReason = null,
                feedback = LivechatState.Feedback(LivechatState.Feedback.Status.NOT_AVAILABLE),
                stateVersion = 1
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val viewModel = createLivechatViewModel(
            livechatRepository(typing),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        assertTrue(viewModel.uiState.value.livechat.isAgentTyping)
        assertEquals("Alex", viewModel.uiState.value.livechat.agentDisplayName)
    }

    @Test
    fun `an expired livechat session clears the agent header and the leave dialog`() = runTest {
        val tick = Channel<Unit>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync()),
            Result.failure(ChatbotException.SessionExpired())
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }
        viewModel.onLivechatControlClick()
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.leaveAskedStatus)

        tick.send(Unit)

        val livechat = viewModel.uiState.value.livechat
        assertEquals(ChatConnectionState.SessionExpired, viewModel.uiState.value.session.connectionState)
        assertEquals(LivechatState.Status.INACTIVE, livechat.status)
        assertEquals(null, livechat.leaveAskedStatus)
        assertEquals(null, livechat.headerTitleOverride)
    }

    @Test
    fun `the ended note follows the agent's last message and survives a failed send`() = runTest {
        val tick = Channel<Unit>()
        val stored = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "thanks", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            stored.await()
            Result.failure(IOException("closed"))
        }
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(
                livechatMessage(id = "goodbye", role = MessageRole.AGENT),
                status = LivechatState.Status.CLOSED
            )
        )
        val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }

        viewModel.onInputChanged("thanks")
        viewModel.postMessage()
        tick.send(Unit)
        stored.complete(Unit)

        val conversation = viewModel.uiState.value.conversation
        assertEquals(2, conversation.size)
        assertEquals(
            LivechatNoteKind.ENDED,
            (conversation[0].part as? MessagePartUiModel.LivechatNote)?.kind
        )
        assertTrue(conversation[1].id.startsWith("goodbye"))
    }

    @Test
    fun `reset closes an open livechat session and restores it when the reset fails`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        coEvery { renewTokenUseCase() } returns Result.failure(IllegalStateException("host"))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()

        coVerify { repository.closeLivechat("Chat reset") }
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
        assertEquals(LivechatControl.END, viewModel.uiState.value.livechat.control)
    }

    @Test
    fun `livechat actions wait while a reset is in progress`() = runTest {
        val renewed = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase() } coAnswers { renewed.await() }
        val repository = livechatRepository(livechatSync(status = LivechatState.Status.INACTIVE))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()
        viewModel.onLivechatControlClick()

        assertFalse(viewModel.canAcceptHumanAgent("marker-1"))
        coVerify(exactly = 0) { repository.requestLivechatHandover(any()) }
        renewed.complete(Result.success(Unit))
    }

    @Test
    fun `a form submit that starts livechat shows the queue without reopening the chat`() = runTest {
        val form = ShowForm(
            partId = "form-1",
            formId = "lead",
            name = "Lead",
            confirmationText = null,
            fields = listOf(
                FormField(
                    key = "email",
                    label = "Email",
                    placeholder = null,
                    type = FormField.FieldType.EMAIL,
                    required = true,
                    options = emptyList(),
                    visibleWhen = emptyList()
                )
            ),
            minFilledFields = 0
        )
        every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns flowOf(
            StreamPartEvent(part = form),
            StreamDoneEvent(message = sampleMessage(id = "reply-1", parts = listOf(form)))
        )
        val repository = livechatRepository(
            livechatSync(status = LivechatState.Status.INACTIVE),
            livechatSync(status = LivechatState.Status.WAITING)
        )
        coEvery { repository.submitAction(any()) } returns Result.success(
            SubmitActionResponse(submissionId = "s1", confirmationText = null)
        )
        val viewModel = createLivechatViewModel(
            repository = repository,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            submitActionUseCase = SubmitActionUseCase(repository)
        ) { awaitCancellation() }
        viewModel.onInputChanged("hi")
        viewModel.postMessage()
        assertEquals(LivechatState.Status.INACTIVE, viewModel.uiState.value.livechat.status)

        viewModel.onFormValueChanged(partId = "form-1", key = "email", value = "a@b.c")
        viewModel.onFormSubmit("form-1")

        assertEquals(LivechatState.Status.WAITING, viewModel.uiState.value.livechat.status)
        val kinds = viewModel.uiState.value.conversation.mapNotNull { bubble ->
            (bubble.part as? MessagePartUiModel.LivechatNote)?.kind
        }
        assertTrue(LivechatNoteKind.QUEUED in kinds)
    }

    @Test
    fun `a live close with pending feedback offers the CSAT prompt`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val active = livechatSync(status = LivechatState.Status.ACTIVE)
        val closed = LivechatSync(
            state = LivechatState.of(
                status = LivechatState.Status.CLOSED,
                feedback = LivechatState.Feedback.Status.PENDING
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(active),
            Result.success(closed)
        ) andThen Result.success(closed)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            tick.receive()
        }
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)

        tick.send(Unit)

        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
        assertNotNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `a restored closed session does not offer CSAT`() = runTest {
        val closed = LivechatSync(
            state = LivechatState.of(
                status = LivechatState.Status.CLOSED,
                feedback = LivechatState.Feedback.Status.PENDING
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val viewModel = createLivechatViewModel(
            livechatRepository(closed),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
        assertNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `skipping CSAT posts nothing`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val active = livechatSync(status = LivechatState.Status.ACTIVE)
        val closed = LivechatSync(
            state = LivechatState.of(
                status = LivechatState.Status.CLOSED,
                feedback = LivechatState.Feedback.Status.PENDING
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(active),
            Result.success(closed)
        ) andThen Result.success(closed)
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(
            repository = repository,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase =
                ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase(repository)
        ) { tick.receive() }
        tick.send(Unit)
        assertNotNull(viewModel.uiState.value.livechat.rating)

        viewModel.onLivechatRatingSkip()

        assertNull(viewModel.uiState.value.livechat.rating)
        coVerify(exactly = 0) { repository.submitLivechatFeedback(any(), any()) }
    }

    @Test
    fun `a close while hidden offers CSAT once the chat is visible again`() = runTest {
        val active = livechatSync(status = LivechatState.Status.ACTIVE)
        val closed = LivechatSync(
            state = LivechatState.of(
                status = LivechatState.Status.CLOSED,
                feedback = LivechatState.Feedback.Status.PENDING
            ),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(active)
        ) andThen Result.success(closed)
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
        viewModel.onChatHidden()
        viewModel.onLivechatControlClick()
        viewModel.onConfirmLeaveLivechat()
        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
        assertNull(viewModel.uiState.value.livechat.rating)

        viewModel.onChatVisible()

        assertNotNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `attach is gated by the host attachment offer`() {
        val viewModel = ChatbotViewModel(
            initializeUseCase = initializeUseCase,
            sendMessageUseCase = sendMessageUseCase,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            startSessionUseCase = startSessionUseCase,
            invalidateSessionUseCase = invalidateSessionUseCase,
            getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
            renewTokenUseCase = renewTokenUseCase,
            deleteChatDataUseCase = deleteChatDataUseCase,
            getMessageHistoryUseCase = getMessageHistoryUseCase,
            getLatestMessageIdUseCase = getLatestMessageIdUseCase,
            streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
            uiStateHolder = ChatbotUiStateHolder(),
            currentPage = null,
            apiBaseUrl = "https://api.example/",
            offersAttachments = false,
            createLivechatSession = { scope ->
                LivechatSession(
                    repository = mockk(relaxed = true),
                    scope = scope,
                    delayMs = { throw CancellationException() }
                )
            },
            submitActionUseCase = mockk(relaxed = true),
            fetchFormUseCase = mockk(relaxed = true),
            getChatSessionUseCase = mockk(relaxed = true),
            patchFormValuesUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase = mockk(relaxed = true)
        )

        assertFalse(viewModel.canAttach())
        assertFalse(viewModel.canAttachConsideringLivechat())
    }

    @Test
    fun `a failed livechat send restores text and chips`() = runTest {
        val image = samplePendingImage()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(
                message = "hi",
                images = any(),
                currentPage = null,
                clientOperationId = any()
            )
        } returns Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }
        viewModel.seedPendingPhoto(image)
        viewModel.onInputChanged("hi")

        viewModel.postMessage()

        assertEquals("hi", viewModel.uiState.value.inputMessage)
        assertEquals(1, viewModel.uiState.value.pendingAttachments.size)
        assertEquals(image, viewModel.uiState.value.pendingAttachments.single().image)
        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `an assistant send posts pending images`() {
        val image = samplePendingImage()
        every {
            sendMessageUseCase(message = "hi", images = any(), currentPage = null)
        } returns emptyFlow()
        val viewModel = createViewModel()
        viewModel.seedPendingPhoto(image)
        viewModel.onInputChanged("hi")

        viewModel.postMessage()

        verify {
            sendMessageUseCase(message = "hi", images = listOf(image), currentPage = null)
        }
        assertTrue(viewModel.uiState.value.pendingAttachments.isEmpty())
    }

    @Test
    fun `a livechat send posts pending images with the text`() = runTest {
        val image = samplePendingImage()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(
                message = "hi",
                images = any(),
                currentPage = null,
                clientOperationId = any()
            )
        } returns Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }
        viewModel.seedPendingPhoto(image)
        viewModel.onInputChanged("hi")

        viewModel.postMessage()

        coVerify {
            sendLivechat(
                message = "hi",
                images = listOf(image),
                currentPage = null,
                clientOperationId = any()
            )
        }
        assertTrue(viewModel.uiState.value.pendingAttachments.isEmpty())
        assertEquals(2, viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `photos-only send is allowed`() = runTest {
        val image = samplePendingImage()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(message = "", images = any(), currentPage = null, clientOperationId = any())
        } returns Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }
        viewModel.seedPendingPhoto(image)

        viewModel.postMessage()

        coVerify {
            sendLivechat(message = "", images = listOf(image), currentPage = null, clientOperationId = any())
        }
        assertEquals(1, viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `entering active without attachments enabled drops pending chips`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val inactive = LivechatSync(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            hasMore = false,
            syncCursor = null
        )
        val active = livechatSync(status = LivechatState.Status.ACTIVE)
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(inactive)
        ) andThen Result.success(active)
        coEvery { repository.requestLivechatHandover(any()) } returns Result.success(
            LivechatState.Status.ACTIVE
        )
        coEvery { initializeUseCase() } returns Result.success(
            sampleConfig(
                livechat = LivechatConfig.serverDefaults.copy(
                    enabled = true,
                    configured = true,
                    availabilityStatus = LivechatConfig.AvailabilityStatus.LIVE,
                    showLivechatLogo = true,
                    attachmentsEnabled = false
                )
            )
        )
        val viewModel = ChatbotViewModel(
            initializeUseCase = initializeUseCase,
            sendMessageUseCase = sendMessageUseCase,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            startSessionUseCase = startSessionUseCase,
            invalidateSessionUseCase = invalidateSessionUseCase,
            getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
            renewTokenUseCase = renewTokenUseCase,
            deleteChatDataUseCase = deleteChatDataUseCase,
            getMessageHistoryUseCase = getMessageHistoryUseCase,
            getLatestMessageIdUseCase = getLatestMessageIdUseCase,
            streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
            uiStateHolder = ChatbotUiStateHolder(),
            currentPage = null,
            apiBaseUrl = "https://api.example/",
            offersAttachments = true,
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = { tick.receive() })
            },
            submitActionUseCase = mockk(relaxed = true),
            fetchFormUseCase = mockk(relaxed = true),
            getChatSessionUseCase = mockk(relaxed = true),
            patchFormValuesUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase = mockk(relaxed = true)
        )
        viewModel.seedPendingPhoto()
        assertEquals(1, viewModel.uiState.value.pendingAttachments.size)

        viewModel.onLivechatControlClick()

        assertTrue(viewModel.uiState.value.pendingAttachments.isEmpty())
        assertEquals(
            SnackbarType.LivechatAttachmentsDropped,
            viewModel.uiState.value.snackbarState?.type
        )
        assertFalse(viewModel.canAttachConsideringLivechat())
    }

    @Test
    fun `a refused rating re-reads the state and closes once the server holds it`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
            Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING)),
            Result.success(closedSync(feedback = LivechatState.Feedback.Status.SUBMITTED))
        )
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns
            Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(
            repository = repository,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase =
                ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase(repository)
        ) { tick.receive() }
        tick.send(Unit)
        assertNotNull(viewModel.uiState.value.livechat.rating)

        viewModel.onLivechatRatingScore(5)

        coVerify(exactly = 1) { repository.submitLivechatFeedback(rating = 5, feedback = null) }
        assertNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `a session expiry while a rating is up does not bring it back`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
            Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING)),
            Result.failure(ChatbotException.SessionExpired())
        )
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns
            Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(
            repository = repository,
            sendLivechatMessageUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase =
                ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase(repository)
        ) { tick.receive() }
        tick.send(Unit)
        viewModel.onLivechatRatingScore(5)
        assertEquals(ChatConnectionState.SessionExpired, viewModel.uiState.value.session.connectionState)

        viewModel.onChatHidden()
        viewModel.onChatVisible()

        assertNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `the CSAT prompt waits for a send in flight and follows the ended note`() = runTest {
        val tick = Channel<Unit>()
        val stored = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "thanks", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            stored.await()
            Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        }
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
            Result.success(
                closedSync(
                    LivechatState.Feedback.Status.PENDING,
                    livechatMessage(id = "goodbye", role = MessageRole.AGENT)
                )
            )
        )
        val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }
        viewModel.onInputChanged("thanks")
        viewModel.postMessage()
        tick.send(Unit)
        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
        assertNull(viewModel.uiState.value.livechat.rating)

        stored.complete(Unit)

        assertTrue(
            viewModel.uiState.value.conversation.any {
                (it.part as? MessagePartUiModel.LivechatNote)?.kind == LivechatNoteKind.ENDED
            }
        )
        assertNotNull(viewModel.uiState.value.livechat.rating)
    }

    private fun closedSync(
        feedback: LivechatState.Feedback.Status,
        vararg messages: LivechatMessage
    ) = LivechatSync(
        state = LivechatState.of(status = LivechatState.Status.CLOSED, feedback = feedback),
        messages = messages.toList(),
        hasMore = false,
        syncCursor = null
    )

    private fun createLivechatViewModel(
        repository: ChatbotRepository,
        sendLivechatMessageUseCase: SendLivechatMessageUseCase,
        submitActionUseCase: SubmitActionUseCase = mockk(relaxed = true),
        submitLivechatFeedbackUseCase: ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase =
            mockk(relaxed = true),
        delayMs: suspend (Long) -> Unit
    ): ChatbotViewModel {
        coEvery { initializeUseCase() } returns Result.success(
            sampleConfig(
                livechat = LivechatConfig.serverDefaults.copy(
                    enabled = true,
                    configured = true,
                    availabilityStatus = LivechatConfig.AvailabilityStatus.LIVE,
                    showLivechatLogo = true,
                    attachmentsEnabled = true
                )
            )
        )
        return ChatbotViewModel(
            initializeUseCase = initializeUseCase,
            sendMessageUseCase = sendMessageUseCase,
            sendLivechatMessageUseCase = sendLivechatMessageUseCase,
            startSessionUseCase = startSessionUseCase,
            invalidateSessionUseCase = invalidateSessionUseCase,
            getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
            renewTokenUseCase = renewTokenUseCase,
            deleteChatDataUseCase = deleteChatDataUseCase,
            getMessageHistoryUseCase = getMessageHistoryUseCase,
            getLatestMessageIdUseCase = getLatestMessageIdUseCase,
            streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
            uiStateHolder = ChatbotUiStateHolder(),
            currentPage = null,
            apiBaseUrl = "https://api.example/",
            offersAttachments = true,
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = delayMs)
            },
            submitActionUseCase = submitActionUseCase,
            fetchFormUseCase = mockk(relaxed = true),
            getChatSessionUseCase = mockk(relaxed = true),
            patchFormValuesUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase = submitLivechatFeedbackUseCase
        )
    }

    private fun ChatbotViewModel.seedPendingPhoto(image: OutgoingImageInput = samplePendingImage()) {
        val holderField = ChatbotViewModel::class.java.getDeclaredField("uiStateHolder").apply {
            isAccessible = true
        }
        val holder = holderField.get(this) as ChatbotUiStateHolder
        holder.addPendingAttachment(
            PendingComposerAttachment(
                image = image,
                displayUrl = "data:${image.mimeType};base64,${image.dataBase64}"
            )
        )
    }

    private fun samplePendingImage() = OutgoingImageInput.create(
        data = "QQ==",
        mime = "image/jpeg",
        filename = null
    )!!

    /** A repository whose livechat reads return [syncs] in order, repeating the last. */
    private fun livechatRepository(vararg syncs: LivechatSync): ChatbotRepository {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany syncs.map { Result.success(it) }
        return repository
    }

    private fun livechatSync(
        vararg messages: LivechatMessage,
        status: LivechatState.Status = LivechatState.Status.ACTIVE
    ) = LivechatSync(
        state = LivechatState.of(status),
        messages = messages.toList(),
        hasMore = false,
        syncCursor = null
    )

    private fun livechatMessage(
        id: String,
        role: MessageRole
    ) = LivechatMessage(
        messageId = id,
        role = role,
        agent = null,
        parts = listOf(sampleRichTextPart(id = "$id-part")),
        createdAt = "2026-01-01T00:00:00Z",
        sequenceNumber = 1
    )

    private fun verifyHistoryLoads(times: Int) {
        verify(exactly = times) { getMessageHistoryUseCase(pagingConfig = any()) }
    }
}
