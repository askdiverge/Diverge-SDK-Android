package ai.askdiverge.ui

import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatCloseReason
import ai.askdiverge.domain.model.livechat.LivechatConfig
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import ai.askdiverge.domain.model.message.incoming.part.RequestImageUpload
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
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
import ai.askdiverge.domain.usecase.RateConversationUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.domain.usecase.SendLivechatMessageUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.ui.attachment.PendingComposerAttachment
import ai.askdiverge.ui.form.ImageAttachmentEncoder
import ai.askdiverge.ui.livechat.LivechatControl
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.toUserMessageItem
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
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

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

    private val submitActionUseCase = mockk<SubmitActionUseCase>()

    private val collectorScope = CoroutineScope(UnconfinedTestDispatcher())

    // The holder the ViewModels below write to, so a test can put photos in the composer.
    private val uiStateHolder = ChatbotUiStateHolder()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        collectorScope.cancel()
        Dispatchers.resetMain()
    }

    private val rateConversationUseCase = mockk<RateConversationUseCase>(relaxed = true)

    private fun createViewModel(
        currentPage: String? = null,
        offersAttachments: Boolean = true,
        encodeDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
        ratingEnabled: Boolean = true,
        rateConversationUseCase: RateConversationUseCase = this.rateConversationUseCase
    ): ChatbotViewModel = ChatbotViewModel(
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
        uiStateHolder = uiStateHolder,
        currentPage = currentPage,
        offersAttachments = offersAttachments,
        encodeDispatcher = encodeDispatcher,
        createLivechatSession = { scope ->
            LivechatSession(
                repository = mockk<ChatbotRepository>(relaxed = true),
                scope = scope,
                delayMs = { throw CancellationException() }
            )
        },
        submitActionUseCase = submitActionUseCase,
        fetchFormUseCase = mockk(relaxed = true),
        getChatSessionUseCase = mockk(relaxed = true),
        patchFormValuesUseCase = mockk(relaxed = true),
        submitLivechatFeedbackUseCase = mockk(relaxed = true),
        rateConversationUseCase = rateConversationUseCase,
        ratingEnabled = ratingEnabled
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
        coEvery { renewTokenUseCase(any()) } coAnswers { resetResult.await() }

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
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)

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
        coVerify { renewTokenUseCase(any()) }
        coVerify(exactly = 1) { initializeUseCase() }
    }

    @Test
    fun `conversation reset keeps the config-derived session fields`() {
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.resetConversation()

        coVerify { renewTokenUseCase(any()) }

        val session = viewModel.uiState.value.session
        assertEquals(expected = "Bot", actual = session.assistantName)
        assertEquals(expected = "https://example.com/privacy-policy", actual = session.privacyPolicyUrl)
    }

    @Test
    fun `conversation reset keeps a draft the user had already typed`() {
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.onInputChanged("half typed")
        viewModel.resetConversation()

        assertEquals(expected = "half typed", actual = viewModel.uiState.value.inputMessage)
    }

    @Test
    fun `conversation reset stops collecting a reply that is still streaming`() {
        val events = MutableSharedFlow<StreamEvent>()
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns events
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        viewModel.resetConversation()

        assertEquals(expected = 0, actual = events.subscriptionCount.value)
    }

    @Test
    fun `conversation reset shows the progress indicator while the token call is in flight`() {
        val resetResult = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase(any()) } coAnswers { resetResult.await() }

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
    fun `a form sent before a conversation reset cannot end the new session when it fails late`() {
        val response = CompletableDeferred<Result<SubmitActionResponse>>()
        coEvery { submitActionUseCase(any()) } coAnswers { response.await() }
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.submitFilledContactForm()
        viewModel.resetConversation()
        response.complete(Result.failure(ChatbotException.SessionExpired()))

        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `a form sent before the chat data was deleted cannot end the new session when it fails late`() {
        val response = CompletableDeferred<Result<SubmitActionResponse>>()
        coEvery { submitActionUseCase(any()) } coAnswers { response.await() }
        coEvery { deleteChatDataUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.submitFilledContactForm()
        viewModel.deleteChatData()
        response.complete(Result.failure(ChatbotException.SessionExpired()))

        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `conversation reset failure reports the result and stops the progress indicator`() {
        coEvery { renewTokenUseCase(any()) } returns Result.failure(IllegalStateException("boom"))

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
        coEvery { renewTokenUseCase(any()) } returns Result.failure(IllegalStateException("boom"))

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
        coEvery { renewTokenUseCase(any()) } coAnswers { newToken.await() }

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
    fun `a history page failing with an ended session expires the chat`() {
        val viewModel = createViewModel()
        assertEquals(ChatConnectionState.Ready, viewModel.uiState.value.session.connectionState)

        viewModel.onHistoryLoadFailed(ChatbotException.SessionExpired())

        assertEquals(
            ChatConnectionState.SessionExpired,
            viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `a history page failing without a token expires the chat`() {
        val viewModel = createViewModel()

        viewModel.onHistoryLoadFailed(ChatbotException.NoTokenAvailable())

        assertEquals(
            ChatConnectionState.SessionExpired,
            viewModel.uiState.value.session.connectionState
        )
    }

    @Test
    fun `a history page failing for another reason keeps the chat ready`() {
        val viewModel = createViewModel()

        viewModel.onHistoryLoadFailed(IOException("timeout"))

        assertEquals(ChatConnectionState.Ready, viewModel.uiState.value.session.connectionState)
    }

    @Test
    fun `a history session expiry replayed while reconnecting is ignored`() {
        coEvery { initializeUseCase() } coAnswers { awaitCancellation() }
        val viewModel = createViewModel()
        val reconnecting = viewModel.uiState.value.session.connectionState

        viewModel.onHistoryLoadFailed(ChatbotException.SessionExpired())

        assertEquals(reconnecting, viewModel.uiState.value.session.connectionState)
        assertIs<ChatConnectionState.Connecting>(reconnecting)
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
        coEvery { renewTokenUseCase(any()) } coAnswers { newToken.await() }

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
            lastSequenceNumber = null,
            hasMore = false,
            syncCursor = null
        )
        val waiting = LivechatSync(
            state = LivechatState.of(LivechatState.Status.WAITING),
            messages = emptyList(),
            lastSequenceNumber = null,
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
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = null
                )
            ),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        viewModel.onDeclineHumanAgent("marker-2")

        assertTrue("marker-2" in viewModel.uiState.value.livechat.declinedHumanAgentPartIds)
    }

    @Test
    fun `waiting then active snapshots insert queued and agent joined notes`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val inactive = LivechatSync(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            lastSequenceNumber = null,
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
            lastSequenceNumber = null,
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
    fun `the livechat log read after a failed bootstrap is not added on top of history`() = runTest {
        val tick = Channel<Unit>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.failure(IOException("offline")),
            Result.success(livechatSync(livechatMessage(id = "m1", role = MessageRole.AGENT)))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }

        tick.send(Unit)

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `an agent message already in the reloaded history is not added again by the resumed sync`() = runTest {
        val resumedRead = CompletableDeferred<Unit>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returns
            Result.success(livechatSync()) coAndThen {
                resumedRead.await()
                Result.success(livechatSync(livechatMessage(id = "agent-1", role = MessageRole.AGENT)))
            }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }
        viewModel.onChatHidden()

        viewModel.onChatVisible()
        coEvery { getLatestMessageIdUseCase() } returns Result.success("agent-1")
        viewModel.onSheetOpened()
        resumedRead.complete(Unit)

        assertEquals("agent-1", viewModel.uiState.value.lastMessageId)
        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `a livechat reply stored after the history was read is shown when the chat opens`() = runTest {
        coEvery { getLatestMessageIdUseCase() } returns Result.success("agent-1")
        val viewModel = createLivechatViewModel(
            livechatRepository(
                livechatSync(
                    livechatMessage(id = "agent-1", role = MessageRole.AGENT, sequence = 1),
                    livechatMessage(id = "agent-2", role = MessageRole.AGENT, sequence = 2)
                )
            ),
            mockk(relaxed = true)
        ) { awaitCancellation() }

        assertEquals(listOf("agent-2-0"), viewModel.uiState.value.conversation.map { it.id })
        assertEquals("agent-2", viewModel.uiState.value.lastMessageId)
    }

    @Test
    fun `a failed livechat send takes back its own bubble and keeps a later queued one`() = runTest {
        val firstAnswers = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "one", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            firstAnswers.await()
            Result.failure(IOException("timeout"))
        }
        coEvery { sendLivechat(message = "two", images = any(), currentPage = null, clientOperationId = any()) } returns
            Result.success(livechatMessage(id = "server-2", role = MessageRole.USER))
        val viewModel = createLivechatViewModel(livechatRepository(livechatSync()), sendLivechat) { awaitCancellation() }

        viewModel.onInputChanged("one")
        viewModel.postMessage()
        viewModel.onInputChanged("two")
        viewModel.postMessage()
        val twoId = viewModel.uiState.value.conversation.first().id
        firstAnswers.complete(Unit)

        assertEquals(listOf(twoId), viewModel.uiState.value.conversation.map { it.id })
        assertEquals("one", viewModel.uiState.value.inputMessage)
    }

    @Test
    fun `messages deferred behind a livechat send are dropped when the session is replaced`() = runTest {
        val tick = Channel<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            awaitCancellation()
        }
        var reads = 0
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
        coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers {
            when (reads++) {
                1 -> Result.success(livechatSync(livechatMessage(id = "agent-1", role = MessageRole.AGENT)))
                2 -> awaitCancellation()
                else -> Result.success(livechatSync())
            }
        }
        val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }

        viewModel.onInputChanged("hi")
        viewModel.postMessage()
        tick.send(Unit)
        tick.send(Unit)
        sessionInvalidations.emit(Unit)

        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `a failed conversation reset resumes livechat syncing`() = runTest {
        coEvery { renewTokenUseCase(any()) } returns Result.failure(IOException("offline"))
        val repository = livechatRepository(livechatSync())
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()

        coVerify(exactly = 2) { repository.syncLivechat(afterSequenceNumber = null, waitMs = null, syncCursor = null) }
    }

    @Test
    fun `a failed chat data delete resumes livechat syncing`() = runTest {
        coEvery { deleteChatDataUseCase() } returns Result.failure(IOException("offline"))
        val repository = livechatRepository(livechatSync())
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.deleteChatData()

        coVerify(exactly = 2) { repository.syncLivechat(afterSequenceNumber = null, waitMs = null, syncCursor = null) }
    }

    @Test
    fun `a conversation reset does not wait for a stalled livechat bootstrap read`() = runTest {
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()

        coVerify(exactly = 1) { renewTokenUseCase(any()) }
        assertFalse(viewModel.uiState.value.isResettingConversation)
    }

    @Test
    fun `a conversation reset cancels a livechat send in flight`() = runTest {
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)
        var cancelled = false
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any()) } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        val viewModel = createLivechatViewModel(livechatRepository(livechatSync()), sendLivechat) { awaitCancellation() }
        viewModel.onInputChanged("hi")
        viewModel.postMessage()

        viewModel.resetConversation()

        assertTrue(cancelled)
    }

    @Test
    fun `a message is not sent until the livechat bootstrap read says where it goes`() = runTest {
        val bootstrapRead = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery { sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any()) } returns
            Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers {
            bootstrapRead.await()
            Result.success(livechatSync())
        }
        val viewModel = createLivechatViewModel(repository, sendLivechat) { awaitCancellation() }
        viewModel.onInputChanged("hi")

        viewModel.postMessage()
        val inputWhileReading = viewModel.uiState.value.inputMessage
        bootstrapRead.complete(Unit)
        viewModel.postMessage()

        assertEquals("hi", inputWhileReading)
        verify(exactly = 0) { sendMessageUseCase(message = any(), images = any(), currentPage = any()) }
        coVerify(exactly = 1) { sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any()) }
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
            lastSequenceNumber = null,
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
            uiStateHolder = uiStateHolder,
            currentPage = null,
            offersAttachments = true,
            encodeDispatcher = UnconfinedTestDispatcher(),
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = { tick.receive() })
            },
            submitActionUseCase = mockk(relaxed = true),
            fetchFormUseCase = ai.askdiverge.domain.usecase.FetchFormUseCase(repository),
            getChatSessionUseCase = ai.askdiverge.domain.usecase.GetChatSessionUseCase(repository),
            patchFormValuesUseCase = ai.askdiverge.domain.usecase.PatchFormValuesUseCase(repository),
            submitLivechatFeedbackUseCase = mockk(relaxed = true),
            rateConversationUseCase = mockk(relaxed = true),
            ratingEnabled = true
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
            lastSequenceNumber = null,
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
    fun `sending to the agent tells the agent the visitor stopped typing`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        viewModel.onInputChanged("hello agent")

        viewModel.postMessage()

        coVerifyOrder {
            repository.sendLivechatTyping(true)
            repository.sendLivechatTyping(false)
        }
    }

    @Test
    fun `a temporary attachment link is re-signed from the livechat log before opening`() = runTest {
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(attachmentMessage(url = "https://cdn.example/fresh.pdf"))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        val opened = collectOpenedUrls(viewModel)

        viewModel.onAttachmentOpen(
            displayUrl = "https://cdn.example/stale.pdf",
            attachmentId = "att-1",
            urlExpiresAt = "2000-01-01T00:00:00Z"
        )

        assertEquals(listOf("https://cdn.example/fresh.pdf"), opened)
    }

    @Test
    fun `a re-signed link that is not http is not opened`() = runTest {
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(attachmentMessage(url = "intent://cdn.example/fresh.pdf"))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        val opened = collectOpenedUrls(viewModel)

        viewModel.onAttachmentOpen(
            displayUrl = "https://cdn.example/stale.pdf",
            attachmentId = "att-1",
            urlExpiresAt = "2000-01-01T00:00:00Z"
        )

        assertTrue(opened.isEmpty())
        assertEquals(
            SnackbarState(type = SnackbarType.AttachmentOpenFailed),
            viewModel.uiState.value.snackbarState
        )
    }

    @Test
    fun `an attachment with no web link shows a snackbar instead of opening`() = runTest {
        val viewModel = createLivechatViewModel(livechatRepository(livechatSync()), mockk(relaxed = true)) {
            awaitCancellation()
        }
        val opened = collectOpenedUrls(viewModel)

        viewModel.onAttachmentOpen(
            displayUrl = "data:application/pdf;base64,abc",
            attachmentId = null,
            urlExpiresAt = null
        )

        assertTrue(opened.isEmpty())
        assertEquals(
            SnackbarState(type = SnackbarType.AttachmentOpenFailed),
            viewModel.uiState.value.snackbarState
        )
    }

    @Test
    fun `taps while an attachment is being re-signed are ignored`() = runTest {
        val gate = CompletableDeferred<Result<LivechatSync>>()
        var syncs = 0
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers {
            syncs += 1
            if (syncs == 1) Result.success(livechatSync()) else gate.await()
        }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        val opened = collectOpenedUrls(viewModel)

        repeat(2) {
            viewModel.onAttachmentOpen(
                displayUrl = "https://cdn.example/stale.pdf",
                attachmentId = "att-1",
                urlExpiresAt = "2000-01-01T00:00:00Z"
            )
        }
        gate.complete(Result.success(livechatSync(attachmentMessage(url = "https://cdn.example/fresh.pdf"))))

        assertEquals(2, syncs)
        assertEquals(listOf("https://cdn.example/fresh.pdf"), opened)
    }

    @Test
    fun `an expired image link is replaced by a freshly signed one from the livechat log`() = runTest {
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(attachmentMessage(url = "https://cdn.example/fresh.png"))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }

        assertEquals(
            expected = "https://cdn.example/fresh.png",
            actual = viewModel.freshAttachmentUrl("att-1")
        )
    }

    @Test
    fun `a freshly signed image link that is not http is not loaded`() = runTest {
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(attachmentMessage(url = "intent://cdn.example/fresh.png"))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }

        assertNull(viewModel.freshAttachmentUrl("att-1"))
    }

    @Test
    fun `an expired attachment whose log read fails shows a snackbar`() = runTest {
        var syncs = 0
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } answers {
            syncs += 1
            if (syncs == 1) Result.success(livechatSync()) else Result.failure(IOException("offline"))
        }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
            awaitCancellation()
        }
        val opened = collectOpenedUrls(viewModel)

        viewModel.onAttachmentOpen(
            displayUrl = "https://cdn.example/stale.pdf",
            attachmentId = "att-1",
            urlExpiresAt = "2000-01-01T00:00:00Z"
        )

        assertTrue(opened.isEmpty())
        assertEquals(
            SnackbarState(type = SnackbarType.AttachmentOpenFailed),
            viewModel.uiState.value.snackbarState
        )
    }

    @Test
    fun `an expired livechat session clears the agent header and the leave dialog`() = runTest {
        val tick = Channel<Unit>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(agentName = "Alex")),
            Result.failure(ChatbotException.SessionExpired())
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }
        viewModel.onLivechatControlClick()
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.leaveAskedStatus)
        assertEquals("Alex", viewModel.uiState.value.livechat.headerTitleOverride)

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
    fun `reset closes an open livechat session once the host supplies the new token`() = runTest {
        val repository = livechatRepository(livechatSync(), livechatSync(status = LivechatState.Status.CLOSED))
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        coEvery { renewTokenUseCase(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
            Result.success(Unit)
        }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()

        coVerify(exactly = 1) { repository.closeLivechat(LivechatCloseReason.RESET) }
        assertEquals(LivechatState.Status.INACTIVE, viewModel.uiState.value.livechat.status)
        assertTrue(viewModel.uiState.value.conversation.isEmpty())
    }

    @Test
    fun `a failed reset leaves the open livechat session open`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        // The host fails before any token is replaced, so nothing runs before the replacement.
        coEvery { renewTokenUseCase(any()) } returns Result.failure(IllegalStateException("host"))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()

        coVerify(exactly = 0) { repository.closeLivechat(any()) }
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
        assertEquals(LivechatControl.END, viewModel.uiState.value.livechat.control)
    }

    @Test
    fun `an agent message that arrives while a delete fails is shown once the session is restored`() = runTest {
        coEvery { deleteChatDataUseCase() } returns Result.failure(IllegalStateException("host"))
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(livechatMessage(id = "late", role = MessageRole.AGENT))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.deleteChatData()

        val state = viewModel.uiState.value
        assertEquals(LivechatState.Status.ACTIVE, state.livechat.status)
        // Only the message: the session was already active, so no agent-joined note again.
        assertEquals(1, state.conversation.size)
        assertTrue(state.conversation.single().id.startsWith("late"))
        assertEquals("late", state.lastMessageId)
    }

    @Test
    fun `a session that closes for inactivity adds the inactivity note`() = runTest {
        val tick = Channel<Unit>()
        val inactivityClose = livechatSync(status = LivechatState.Status.CLOSED).let { sync ->
            sync.copy(
                state = sync.state.copy(
                    closedBy = LivechatState.ClosedBy.AGENT,
                    closedByAgentId = null,
                    closeReason = LivechatState.INACTIVITY_CLOSE_REASON
                )
            )
        }
        val repository = livechatRepository(livechatSync(), inactivityClose)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }

        tick.send(Unit)

        assertEquals(listOf(LivechatNoteKind.ENDED_FOR_INACTIVITY), viewModel.noteKinds())
        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
    }

    @Test
    fun `confirming end chat closes the session and adds the ended note`() = runTest {
        val repository = livechatRepository(livechatSync(), livechatSync(status = LivechatState.Status.CLOSED))
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.leaveAskedStatus)
        coVerify(exactly = 0) { repository.closeLivechat(any()) }
        viewModel.onConfirmLeaveLivechat()

        coVerify(exactly = 1) { repository.closeLivechat(LivechatCloseReason.ENDED_BY_VISITOR) }
        val livechat = viewModel.uiState.value.livechat
        assertNull(livechat.leaveAskedStatus)
        assertEquals(LivechatState.Status.CLOSED, livechat.status)
        assertFalse(livechat.isBusy)
        assertEquals(listOf(LivechatNoteKind.ENDED), viewModel.noteKinds())
    }

    @Test
    fun `dismissing the end chat dialog keeps the session open`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()
        viewModel.onDismissLeaveLivechat()

        coVerify(exactly = 0) { repository.closeLivechat(any()) }
        assertNull(viewModel.uiState.value.livechat.leaveAskedStatus)
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
    }

    @Test
    fun `a handover while livechat is offline shows the offline notice`() = runTest {
        val repository = livechatRepository(livechatSync(status = LivechatState.Status.INACTIVE))
        coEvery { repository.requestLivechatHandover(any()) } returns Result.failure(
            LivechatRequestException.Conflict()
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()

        assertEquals(SnackbarType.LivechatOffline, viewModel.uiState.value.snackbarState?.type)
        assertEquals(LivechatControl.START, viewModel.uiState.value.livechat.control)
        assertFalse(viewModel.uiState.value.livechat.isBusy)
    }

    @Test
    fun `a failed handover shows the livechat error rather than a send error`() = runTest {
        val repository = livechatRepository(livechatSync(status = LivechatState.Status.INACTIVE))
        coEvery { repository.requestLivechatHandover(any()) } returns Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()

        assertEquals(SnackbarType.LivechatActionFailed, viewModel.uiState.value.snackbarState?.type)
    }

    @Test
    fun `a failed end chat shows the livechat error rather than a send error`() = runTest {
        val repository = livechatRepository(livechatSync())
        coEvery { repository.closeLivechat(any()) } returns Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()
        viewModel.onConfirmLeaveLivechat()

        assertEquals(SnackbarType.LivechatActionFailed, viewModel.uiState.value.snackbarState?.type)
        assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
    }

    @Test
    fun `agent messages synced during an assistant reply wait for the reply to finish`() = runTest {
        val tick = Channel<Unit>()
        val events = MutableSharedFlow<StreamEvent>()
        every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns events
        val repository = livechatRepository(
            livechatSync(status = LivechatState.Status.WAITING),
            livechatSync(livechatMessage(id = "agent-1", role = MessageRole.AGENT))
        )
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }

        viewModel.onInputChanged("hi")
        viewModel.postMessage()
        tick.send(Unit)

        assertTrue(viewModel.uiState.value.conversation.none { it.id.startsWith("agent-1") })
        assertTrue(viewModel.noteKinds().isEmpty())

        events.emit(StreamDoneEvent(message = sampleMessage(id = "reply-1")))

        val conversation = viewModel.uiState.value.conversation
        assertTrue(conversation[0].id.startsWith("agent-1"))
        assertEquals(
            LivechatNoteKind.AGENT_JOINED,
            (conversation[1].part as? MessagePartUiModel.LivechatNote)?.kind
        )
    }

    @Test
    fun `the human agent card is not acceptable during a reset or once declined`() = runTest {
        val renewed = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase(any()) } coAnswers { renewed.await() }
        val repository = livechatRepository(livechatSync(status = LivechatState.Status.INACTIVE))
        coEvery { repository.requestLivechatHandover(any()) } returns Result.success(LivechatState.Status.WAITING)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }
        assertTrue(viewModel.uiState.value.canRequestHumanAgent)

        viewModel.onDeclineHumanAgent("marker-1")
        viewModel.onAcceptHumanAgent("marker-1")
        viewModel.resetConversation()

        assertFalse(viewModel.uiState.value.canRequestHumanAgent)
        coVerify(exactly = 0) { repository.requestLivechatHandover(any()) }
        renewed.complete(Result.success(Unit))
        assertTrue(viewModel.uiState.value.canRequestHumanAgent)
    }

    @Test
    fun `livechat actions wait while a reset is in progress`() = runTest {
        val renewed = CompletableDeferred<Result<Unit>>()
        coEvery { renewTokenUseCase(any()) } coAnswers { renewed.await() }
        val repository = livechatRepository(livechatSync(status = LivechatState.Status.INACTIVE))
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.resetConversation()
        viewModel.onLivechatControlClick()

        assertFalse(viewModel.uiState.value.canRequestHumanAgent)
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
    fun `a form submit keeps the latest message id when the last livechat session is closed`() = runTest {
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
        val closed = livechatSync(
            livechatMessage(id = "lc-1", role = MessageRole.AGENT),
            status = LivechatState.Status.CLOSED
        )
        val repository = livechatRepository(closed, closed)
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
        assertEquals("reply-1", viewModel.uiState.value.lastMessageId)

        viewModel.onFormValueChanged(partId = "form-1", key = "email", value = "a@b.c")
        viewModel.onFormSubmit("form-1")

        assertEquals("reply-1", viewModel.uiState.value.lastMessageId)
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
            lastSequenceNumber = null,
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
            lastSequenceNumber = null,
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
            lastSequenceNumber = null,
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
            lastSequenceNumber = null,
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
            uiStateHolder = uiStateHolder,
            currentPage = null,
            offersAttachments = false,
            encodeDispatcher = UnconfinedTestDispatcher(),
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
            submitLivechatFeedbackUseCase = mockk(relaxed = true),
            rateConversationUseCase = mockk(relaxed = true),
            ratingEnabled = true
        )

        assertFalse(viewModel.canAttach())
        assertFalse(viewModel.canAttachConsideringLivechat())
    }

    @Test
    fun `an image-upload prompt arms the picker`() {
        val viewModel = createViewModel()

        val armed = viewModel.onImageUploadPromptAddPhoto(maxSizeBytes = 2_000)

        assertTrue(armed)
        assertNull(viewModel.uiState.value.snackbarState)
    }

    @Test
    fun `an image-upload prompt is refused while attach is unavailable`() {
        val viewModel = createViewModel(offersAttachments = false)

        val armed = viewModel.onImageUploadPromptAddPhoto(maxSizeBytes = 2_000)

        assertFalse(armed)
        assertEquals(
            SnackbarType.PhotoAttachFailed,
            viewModel.uiState.value.snackbarState?.type
        )
    }

    @Nested
    @DisplayName("An image-upload prompt's size budget")
    inner class ImageUploadPromptBudget {

        @BeforeTest
        fun mockEncoder() {
            mockkObject(ImageAttachmentEncoder)
            every { ImageAttachmentEncoder.encodeForMessage(any(), any()) } returns null
        }

        @AfterTest
        fun unmockEncoder() {
            unmockkObject(ImageAttachmentEncoder)
        }

        @Test
        fun `applies to the next composer pick only`() {
            val viewModel = createViewModel()
            viewModel.onImageUploadPromptAddPhoto(maxSizeBytes = 1_000)

            viewModel.onComposerPhotoPicked { null }
            viewModel.onComposerPhotoPicked { null }

            verifyOrder {
                ImageAttachmentEncoder.encodeForMessage(any(), 1_000)
                ImageAttachmentEncoder.encodeForMessage(any(), null)
            }
        }

        @Test
        fun `is cleared when the picker is dismissed`() {
            val viewModel = createViewModel()
            viewModel.onImageUploadPromptAddPhoto(maxSizeBytes = 1_000)

            viewModel.onComposerPhotoPickDismissed()
            viewModel.onComposerPhotoPicked { null }

            verify(exactly = 1) { ImageAttachmentEncoder.encodeForMessage(any(), null) }
            verify(exactly = 0) { ImageAttachmentEncoder.encodeForMessage(any(), 1_000) }
        }

        @Test
        fun `is capped by a smaller livechat attachment limit`() {
            val viewModel = createViewModel()
            uiStateHolder.onConfigLoaded(
                sampleConfig(
                    livechat = LivechatConfig.serverDefaults.copy(
                        attachmentsEnabled = true,
                        maxAttachmentSizeBytes = 2_000
                    )
                )
            )
            uiStateHolder.onLivechatStatus(
                status = LivechatState.Status.ACTIVE,
                agentDisplayName = "Agent"
            )
            viewModel.onImageUploadPromptAddPhoto(maxSizeBytes = 5_000)

            viewModel.onComposerPhotoPicked { null }

            verify(exactly = 1) { ImageAttachmentEncoder.encodeForMessage(any(), 2_000) }
        }
    }

    @Nested
    @DisplayName("A photo request from the assistant")
    inner class PhotoRequest {

        private val request = RequestImageUpload(
            partId = "part_upload_1",
            acceptedTypes = listOf("image/png", "image/jpeg", "image/webp"),
            maxSizeBytes = 5_242_880
        )
        private val card = MessagePartUiModel.RequestImageUpload(
            partId = "part_upload_1",
            maxSizeBytes = 5_242_880
        )
        private val text = sampleRichTextUiModel("Hello")

        @Test
        fun `streamed in a reply shows its card when the host offers photos`() {
            val parts = streamedReplyParts(offersAttachments = true)

            assertEquals(listOf(card), parts.filterIsInstance<MessagePartUiModel.RequestImageUpload>())
        }

        @Test
        fun `streamed in a reply shows only the reply's text when the host disables attachments`() {
            val parts = streamedReplyParts(offersAttachments = false)

            assertTrue(parts.none { it is MessagePartUiModel.RequestImageUpload })
            assertTrue(text in parts)
        }

        @Test
        fun `in a history page shows its card when the host offers photos`() {
            val parts = historyPageParts(offersAttachments = true)

            assertEquals(listOf(card, text), parts)
        }

        @Test
        fun `in a history page shows only the message's text when the host disables attachments`() {
            val parts = historyPageParts(offersAttachments = false)

            assertEquals(listOf<MessagePartUiModel>(text), parts)
        }

        @Test
        fun `in a livechat message shows its card when the host offers photos`() = runTest {
            val parts = livechatMessageParts(offersAttachments = true)

            assertEquals(listOf(card), parts.filterIsInstance<MessagePartUiModel.RequestImageUpload>())
        }

        @Test
        fun `in a livechat message shows only the message's text when the host disables attachments`() = runTest {
            val parts = livechatMessageParts(offersAttachments = false)

            assertTrue(parts.none { it is MessagePartUiModel.RequestImageUpload })
            assertTrue(text in parts)
        }

        private fun streamedReplyParts(offersAttachments: Boolean): List<MessagePartUiModel> {
            val reply = listOf(sampleRichTextPart(), request)
            every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns flowOf(
                StreamPartEvent(part = reply[0]),
                StreamPartEvent(part = reply[1]),
                StreamDoneEvent(message = sampleMessage(id = "reply-1", parts = reply))
            )
            val viewModel = createViewModel(offersAttachments = offersAttachments)

            viewModel.send("hi")

            return viewModel.uiState.value.conversation.map { it.part }
        }

        // Presents the page the way the conversation list does, so the mapped bubbles can be read.
        private fun historyPageParts(offersAttachments: Boolean): List<MessagePartUiModel> {
            val page = listOf(sampleMessage(id = "message-1", parts = listOf(sampleRichTextPart(), request)))
            every { getMessageHistoryUseCase(pagingConfig = any()) } returns flowOf(PagingData.from(page))
            val viewModel = createViewModel(offersAttachments = offersAttachments)
            val presenter = object : PagingDataPresenter<ChatBubbleUiModel>() {
                override suspend fun presentPagingDataEvent(event: PagingDataEvent<ChatBubbleUiModel>) = Unit
            }

            collectorScope.launch { viewModel.historyMessages.collect { presenter.collectFrom(it) } }

            return presenter.snapshot().items.map { it.part }
        }

        private suspend fun livechatMessageParts(offersAttachments: Boolean): List<MessagePartUiModel> {
            val tick = Channel<Unit>()
            val message = LivechatMessage(
                messageId = "m-request",
                role = MessageRole.ASSISTANT,
                agent = null,
                parts = listOf(sampleRichTextPart(), request),
                createdAt = "2026-01-01T00:00:00Z",
                sequenceNumber = 1
            )
            val viewModel = createLivechatViewModel(
                repository = livechatRepository(livechatSync(), livechatSync(message)),
                sendLivechatMessageUseCase = mockk(relaxed = true),
                offersAttachments = offersAttachments
            ) { tick.receive() }

            tick.send(Unit)

            return viewModel.uiState.value.conversation.map { it.part }
        }
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
        seedPendingPhoto(image)
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
        seedPendingPhoto(image)
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
        seedPendingPhoto(image)
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
        seedPendingPhoto(image)

        viewModel.postMessage()

        coVerify {
            sendLivechat(message = "", images = listOf(image), currentPage = null, clientOperationId = any())
        }
        assertEquals(1, viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `a failed livechat send keeps the delivered message before it`() = runTest {
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(message = "Hi", images = any(), currentPage = null, clientOperationId = any())
        } returns Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
        coEvery {
            sendLivechat(message = "It arrived broken", images = any(), currentPage = null, clientOperationId = any())
        } returns Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }
        viewModel.onInputChanged("Hi")
        viewModel.postMessage()
        seedPendingPhoto()
        viewModel.onInputChanged("It arrived broken")

        viewModel.postMessage()

        val state = viewModel.uiState.value
        assertEquals(listOf("Hi".toUserMessageItem().part), state.conversation.map { it.part })
        assertEquals("It arrived broken", state.inputMessage)
        assertEquals(1, state.pendingAttachments.size)
    }

    @Test
    fun `a livechat send that fails while the assistant replies leaves that reply alone`() = runTest {
        val tick = Channel<Unit>()
        val agentSendAnswered = CompletableDeferred<Unit>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(message = "for the agent", images = any(), currentPage = null, clientOperationId = any())
        } coAnswers {
            agentSendAnswered.await()
            Result.failure(IOException("no active session"))
        }
        val assistantEvents = MutableSharedFlow<StreamEvent>()
        every {
            sendMessageUseCase(message = "for the assistant", images = any(), currentPage = null)
        } returns assistantEvents
        val repository = livechatRepository(
            livechatSync(),
            livechatSync(status = LivechatState.Status.CLOSED)
        )
        val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }
        // The message to the agent is still on its way when the agent closes the session.
        seedPendingPhoto()
        viewModel.onInputChanged("for the agent")
        viewModel.postMessage()
        tick.send(Unit)
        // The visitor asks the assistant instead, and its reply starts arriving.
        viewModel.onInputChanged("for the assistant")
        viewModel.postMessage()
        assistantEvents.emit(StreamPartEvent(part = sampleRichTextPart(id = "reply-part")))
        val replyBubble = viewModel.uiState.value.conversation.first()

        agentSendAnswered.complete(Unit)

        val state = viewModel.uiState.value
        assertEquals(
            listOf(replyBubble.part, "for the assistant".toUserMessageItem().part),
            state.conversation.map { it.part }
        )
        assertIs<PendingBotMessageState.Streaming>(state.pendingBotMessage)
        assertEquals("for the agent", state.inputMessage)
        assertEquals(1, state.pendingAttachments.size)
    }

    @Test
    fun `a livechat retry reuses the operation id only while the photos are unchanged`() = runTest {
        val operationIds = mutableListOf<String>()
        val sendLivechat = mockk<SendLivechatMessageUseCase>()
        coEvery {
            sendLivechat(
                message = "hi",
                images = any(),
                currentPage = null,
                clientOperationId = capture(operationIds)
            )
        } returns Result.failure(IOException("timeout"))
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            sendLivechat
        ) { awaitCancellation() }
        seedPendingPhoto()
        viewModel.onInputChanged("hi")

        viewModel.postMessage()
        viewModel.postMessage()
        viewModel.onRemovePendingAttachment(viewModel.uiState.value.pendingAttachments.single().id)
        viewModel.postMessage()

        assertEquals(3, operationIds.size)
        assertEquals(operationIds[0], operationIds[1])
        assertNotEquals(operationIds[1], operationIds[2])
    }

    @Test
    fun `an assistant send that fails restores its photos`() {
        every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns
            flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))
        val viewModel = createViewModel()
        val pending = seedPendingPhoto()
        viewModel.onInputChanged("hi")

        viewModel.postMessage()

        val state = viewModel.uiState.value
        assertEquals(listOf(pending), state.pendingAttachments)
        assertEquals("hi", state.inputMessage)
        assertTrue(state.conversation.isEmpty())
    }

    @Test
    fun `a photo picked while attach is unavailable shows the attach failure`() {
        val viewModel = createViewModel(offersAttachments = false)

        viewModel.onComposerPhotoPicked { error("the photo is not read") }

        assertEquals(SnackbarType.PhotoAttachFailed, viewModel.uiState.value.snackbarState?.type)
        assertFalse(viewModel.uiState.value.isEncodingAttachment)
    }

    @Test
    fun `an unreadable photo releases the encoding flag and shows the attach failure`() {
        val viewModel = createViewModel()

        viewModel.onComposerPhotoPicked { null }

        val state = viewModel.uiState.value
        assertFalse(state.isEncodingAttachment)
        assertTrue(state.pendingAttachments.isEmpty())
        assertEquals(SnackbarType.PhotoAttachFailed, state.snackbarState?.type)
    }

    @Test
    fun `a photo still encoding when the conversation resets is discarded`() = runTest {
        coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)
        val viewModel = createViewModel(encodeDispatcher = StandardTestDispatcher(testScheduler))
        viewModel.onComposerPhotoPicked { null }
        assertTrue(viewModel.uiState.value.isEncodingAttachment)

        viewModel.resetConversation()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEncodingAttachment)
        assertTrue(state.pendingAttachments.isEmpty())
        assertNull(state.snackbarState)
    }

    @Test
    fun `removing a pending photo drops only its chip`() {
        val viewModel = createViewModel()
        val first = seedPendingPhoto()
        val second = seedPendingPhoto()

        viewModel.onRemovePendingAttachment(first.id)

        assertEquals(listOf(second), viewModel.uiState.value.pendingAttachments)
    }

    @Test
    fun `attach for a live agent is disabled once eight photos are pending`() = runTest {
        val viewModel = createLivechatViewModel(
            livechatRepository(livechatSync()),
            mockk(relaxed = true)
        ) { awaitCancellation() }
        val chips = List(ChatbotUiStateHolder.MAX_PENDING_ATTACHMENTS) { seedPendingPhoto() }
        assertFalse(viewModel.canAttach())

        viewModel.onRemovePendingAttachment(chips.first().id)

        assertTrue(viewModel.canAttach())
    }

    @Test
    fun `attach for the assistant takes one photo`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.canAttach())

        seedPendingPhoto()

        assertFalse(viewModel.canAttach())
    }

    @Test
    fun `an assistant send carries one photo and keeps the others for the next message`() {
        every { sendMessageUseCase(message = "", images = any(), currentPage = null) } returns emptyFlow()
        val viewModel = createViewModel()
        val first = seedPendingPhoto()
        val second = seedPendingPhoto(
            requireNotNull(OutgoingImageInput.create(data = "Qg==", mime = "image/jpeg", filename = null))
        )

        viewModel.postMessage()

        verify(exactly = 1) {
            sendMessageUseCase(message = "", images = listOf(first.image), currentPage = null)
        }
        assertEquals(listOf(second), viewModel.uiState.value.pendingAttachments)
        assertEquals(1, viewModel.uiState.value.conversation.size)
    }

    @Test
    fun `entering active without attachments enabled drops pending chips`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val inactive = LivechatSync(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            lastSequenceNumber = null,
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
            uiStateHolder = uiStateHolder,
            currentPage = null,
            offersAttachments = true,
            encodeDispatcher = UnconfinedTestDispatcher(),
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = { tick.receive() })
            },
            submitActionUseCase = mockk(relaxed = true),
            fetchFormUseCase = mockk(relaxed = true),
            getChatSessionUseCase = mockk(relaxed = true),
            patchFormValuesUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase = mockk(relaxed = true),
            rateConversationUseCase = mockk(relaxed = true),
            ratingEnabled = true
        )
        seedPendingPhoto()
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
        coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
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

    @Test
    fun `a close whose follow-up reads fail does not invent a CSAT prompt`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(status = LivechatState.Status.WAITING))
        ) andThen Result.failure(IOException("offline"))
        coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }

        viewModel.onLivechatControlClick()
        viewModel.onConfirmLeaveLivechat()

        assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
        assertNull(viewModel.uiState.value.livechat.rating)
    }

    @Test
    fun `an expiry on an assistant send drops the CSAT prompt for good`() = runTest {
        val tick = Channel<Unit>(Channel.UNLIMITED)
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
            Result.success(livechatSync(status = LivechatState.Status.ACTIVE))
        ) andThen Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
        every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
            flow { throw ChatbotException.SessionExpired() }
        val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { tick.receive() }
        tick.send(Unit)
        assertNotNull(viewModel.uiState.value.livechat.rating)

        viewModel.onInputChanged("hello")
        viewModel.postMessage()
        viewModel.onChatHidden()
        viewModel.onChatVisible()

        assertEquals(ChatConnectionState.SessionExpired, viewModel.uiState.value.session.connectionState)
        assertNull(viewModel.uiState.value.livechat.rating)
    }

    private fun closedSync(
        feedback: LivechatState.Feedback.Status,
        vararg messages: LivechatMessage
    ) = LivechatSync(
        state = LivechatState.of(status = LivechatState.Status.CLOSED, feedback = feedback),
        messages = messages.toList(),
        lastSequenceNumber = messages.lastOrNull()?.sequenceNumber,
        hasMore = false,
        syncCursor = null
    )

    @Nested
    @DisplayName("Suggestion card send")
    inner class SuggestionCardSend {

        @Test
        fun `postPrompt sends the prompt and leaves the composer draft`() {
            every {
                sendMessageUseCase(message = "Show boots", images = any(), currentPage = null)
            } returns flowOf(StreamDoneEvent(message = sampleMessage(id = "reply-1")))

            val viewModel = createViewModel()
            viewModel.onInputChanged("draft stays")
            viewModel.postPrompt("Show boots")

            assertEquals("draft stays", viewModel.uiState.value.inputMessage)
            verify(exactly = 1) {
                sendMessageUseCase(message = "Show boots", images = emptyList(), currentPage = null)
            }
            assertTrue(
                viewModel.uiState.value.conversation.any { bubble ->
                    bubble.role == ai.askdiverge.ui.model.message.MessageRoleUiModel.USER
                }
            )
        }

        @Test
        fun `postPrompt is ignored while a reply is streaming`() {
            every { sendMessageUseCase(message = "first", images = any(), currentPage = null) } returns
                flow {
                    emit(
                        StreamDeltaEvent.PartStarted(
                            partId = "p1",
                            partType = ai.askdiverge.domain.model.event.delta.DeltaPartType.RICH_TEXT
                        )
                    )
                    awaitCancellation()
                }

            val viewModel = createViewModel()
            viewModel.onInputChanged("first")
            viewModel.postMessage()
            assertTrue(viewModel.uiState.value.pendingBotMessage != PendingBotMessageState.None)

            viewModel.postPrompt("Show boots")

            verify(exactly = 0) {
                sendMessageUseCase(message = "Show boots", images = any(), currentPage = null)
            }
        }

        @Test
        fun `a blank postPrompt is ignored`() {
            val viewModel = createViewModel()
            viewModel.postPrompt("   ")
            verify(exactly = 0) { sendMessageUseCase(any(), any(), any()) }
        }

        @Test
        fun `a failed postPrompt leaves the composer draft and photos as they are`() {
            every { sendMessageUseCase(message = "Show boots", images = any(), currentPage = null) } returns
                flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))
            val viewModel = createViewModel()
            val pending = seedPendingPhoto()
            viewModel.onInputChanged("my draft")

            viewModel.postPrompt("Show boots")

            val state = viewModel.uiState.value
            assertEquals("my draft", state.inputMessage)
            assertEquals(listOf(pending), state.pendingAttachments)
            assertTrue(state.conversation.isEmpty())
        }

        @Test
        fun `a postPrompt that fails keeps what the visitor typed during the send`() {
            val failure = CompletableDeferred<Unit>()
            every { sendMessageUseCase(message = "Show boots", images = any(), currentPage = null) } returns
                flow {
                    failure.await()
                    throw IOException("offline")
                }
            val viewModel = createViewModel()

            viewModel.postPrompt("Show boots")
            viewModel.onInputChanged("in size 42")
            failure.complete(Unit)

            assertEquals("in size 42", viewModel.uiState.value.inputMessage)
            assertEquals(SnackbarType.MessageSendFailed, viewModel.uiState.value.snackbarState?.type)
        }

        @Test
        fun `a failed postPrompt to a live agent leaves the composer as it is`() = runTest {
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } returns Result.failure(IOException("timeout"))
            val viewModel = createLivechatViewModel(
                livechatRepository(livechatSync()),
                sendLivechat
            ) { awaitCancellation() }
            val pending = seedPendingPhoto()
            viewModel.onInputChanged("my draft")

            viewModel.postPrompt("Show boots")

            val state = viewModel.uiState.value
            assertEquals("my draft", state.inputMessage)
            assertEquals(listOf(pending), state.pendingAttachments)
            coVerify(exactly = 1) {
                sendLivechat(message = "Show boots", images = emptyList(), currentPage = null, clientOperationId = any())
            }
        }

        @Test
        fun `a postPrompt to a live agent keeps the retry id of a draft whose send failed`() = runTest {
            val operationIds = mutableListOf<String>()
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = capture(operationIds))
            } returns Result.failure(IOException("timeout")) andThen
                Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } returns Result.success(livechatMessage(id = "server-2", role = MessageRole.USER))
            val viewModel = createLivechatViewModel(
                livechatRepository(livechatSync()),
                sendLivechat
            ) { awaitCancellation() }

            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            viewModel.postPrompt("Show boots")
            viewModel.postMessage()

            assertEquals(2, operationIds.size)
            assertEquals(operationIds[0], operationIds[1])
        }

        @Test
        fun `cards cannot send while the conversation is being reset`() {
            val viewModel = createViewModel()
            assertTrue(viewModel.canPostPrompt())

            uiStateHolder.onConversationResetStarted()

            assertFalse(viewModel.canPostPrompt())
        }

        @Test
        fun `cards cannot send while a livechat handover or close is in flight`() {
            val viewModel = createViewModel()
            assertTrue(viewModel.canPostPrompt())

            uiStateHolder.onLivechatBusy(true)

            assertFalse(viewModel.canPostPrompt())
        }

        @Test
        fun `a card tapped before the livechat bootstrap read answers is not sent`() = runTest {
            val bootstrapRead = CompletableDeferred<Unit>()
            every { sendMessageUseCase(message = any(), images = any(), currentPage = any()) } returns emptyFlow()
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } returns Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            val viewModel = createLivechatViewModel(
                repositoryAnsweringAfter(bootstrapRead),
                sendLivechat
            ) { awaitCancellation() }

            viewModel.postPrompt("Show boots")
            val bubblesWhileReading = viewModel.uiState.value.conversation.size
            bootstrapRead.complete(Unit)
            viewModel.postPrompt("Show boots")

            assertEquals(0, bubblesWhileReading)
            verify(exactly = 0) { sendMessageUseCase(message = any(), images = any(), currentPage = any()) }
            coVerify(exactly = 1) {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            }
        }

        @Test
        fun `cards cannot send until the livechat bootstrap read answers`() = runTest {
            val bootstrapRead = CompletableDeferred<Unit>()
            val viewModel = createLivechatViewModel(
                repositoryAnsweringAfter(bootstrapRead),
                mockk(relaxed = true)
            ) { awaitCancellation() }

            val canSendWhileReading = viewModel.canPostPrompt()
            bootstrapRead.complete(Unit)

            assertFalse(canSendWhileReading)
            assertTrue(viewModel.canPostPrompt())
        }

        @Test
        fun `a typed message waits for the livechat bootstrap read like a card`() = runTest {
            val bootstrapRead = CompletableDeferred<Unit>()
            every { sendMessageUseCase(message = any(), images = any(), currentPage = any()) } returns emptyFlow()
            val viewModel = createLivechatViewModel(
                repositoryAnsweringAfter(bootstrapRead),
                mockk(relaxed = true)
            ) { awaitCancellation() }
            viewModel.onInputChanged("Show boots")

            viewModel.postMessage()

            verify(exactly = 0) { sendMessageUseCase(message = any(), images = any(), currentPage = any()) }
            assertEquals("Show boots", viewModel.uiState.value.inputMessage)
        }

        @Test
        fun `cards can send again after a reset cancels a stalled livechat bootstrap read`() = runTest {
            coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }
            val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) { awaitCancellation() }
            val canSendWhileReading = viewModel.canPostPrompt()

            viewModel.resetConversation()

            assertFalse(canSendWhileReading)
            assertTrue(viewModel.canPostPrompt())
        }

        @Test
        fun `a second tap on a card while its send to the agent is in flight does not send it again`() = runTest {
            val firstSend = CompletableDeferred<Unit>()
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } coAnswers {
                firstSend.await()
                Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            }
            val viewModel = createLivechatViewModel(
                livechatRepository(livechatSync()),
                sendLivechat
            ) { awaitCancellation() }

            viewModel.postPrompt("Show boots")
            viewModel.postPrompt("Show boots")
            firstSend.complete(Unit)

            assertEquals(
                1,
                viewModel.uiState.value.conversation.count { bubble ->
                    bubble.role == ai.askdiverge.ui.model.message.MessageRoleUiModel.USER
                }
            )
            coVerify(exactly = 1) {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            }
        }

        @Test
        fun `cards can send again once a prompt has reached the agent`() = runTest {
            val send = CompletableDeferred<Unit>()
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } coAnswers {
                send.await()
                Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            }
            val viewModel = createLivechatViewModel(
                livechatRepository(livechatSync()),
                sendLivechat
            ) { awaitCancellation() }

            viewModel.postPrompt("Show boots")
            val canSendWhileSending = viewModel.canPostPrompt()
            send.complete(Unit)

            assertFalse(canSendWhileSending)
            assertTrue(viewModel.canPostPrompt())
        }

        @Test
        fun `cards can send again after a prompt to the agent fails`() = runTest {
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "Show boots", images = any(), currentPage = null, clientOperationId = any())
            } returns Result.failure(IOException("timeout"))
            val viewModel = createLivechatViewModel(
                livechatRepository(livechatSync()),
                sendLivechat
            ) { awaitCancellation() }

            viewModel.postPrompt("Show boots")

            assertTrue(viewModel.canPostPrompt())
        }

        /** A repository whose livechat bootstrap read finds an agent session once [read] completes. */
        private fun repositoryAnsweringAfter(read: CompletableDeferred<Unit>): ChatbotRepository {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers {
                read.await()
                Result.success(livechatSync())
            }
            return repository
        }
    }

    @Nested
    @DisplayName("Conversation rating on dismiss")
    inner class ConversationRatingDismiss {

        private fun sendAssistantMessage(viewModel: ChatbotViewModel) {
            every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns
                flowOf(StreamDoneEvent(message = sampleMessage(id = "reply-1")))
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
        }

        /** The visitor wrote to an agent, who then closed the session while the chat watched. */
        private suspend fun createChatAfterLiveAgentClose(): ChatbotViewModel {
            val tick = Channel<Unit>(Channel.UNLIMITED)
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
                Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            ) andThen Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            coEvery { repository.submitLivechatFeedback(any(), any()) } returns Result.success(Unit)
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(
                    message = any(),
                    images = any(),
                    currentPage = any(),
                    clientOperationId = any()
                )
            } returns Result.success(
                LivechatMessage(
                    messageId = "lc-1",
                    role = MessageRole.USER,
                    agent = null,
                    parts = emptyList(),
                    createdAt = "2026-01-01T00:00:00Z",
                    sequenceNumber = 1L
                )
            )
            val viewModel = createLivechatViewModel(
                repository = repository,
                sendLivechatMessageUseCase = sendLivechat,
                submitLivechatFeedbackUseCase =
                    ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase(repository)
            ) { tick.receive() }
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            tick.send(Unit)
            assertNotNull(viewModel.uiState.value.livechat.rating)
            return viewModel
        }

        @Test
        fun `dismiss without a user message closes immediately`() {
            val viewModel = createViewModel()
            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `dismiss after a user message offers the rating prompt`() {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `DISABLED skips the rating prompt`() {
            val viewModel = createViewModel(ratingEnabled = false)
            sendAssistantMessage(viewModel)
            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `Skip dismisses without posting and is not asked again`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()
            coVerify(exactly = 0) { rateConversationUseCase(any(), any()) }
            assertNull(viewModel.uiState.value.conversationRating)
            withTimeout(1_000) { viewModel.dismissEvents.first() }
            assertTrue(viewModel.onSheetDismissRequested())
        }

        @Test
        fun `a dismiss no sheet collected is dropped before a new sheet collects`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            viewModel.dropPendingDismiss()

            assertNull(withTimeoutOrNull(1_000) { viewModel.dismissEvents.first() })
        }

        @Test
        fun `cancelling keeps the picked score and comment for the next dismiss`() {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingScore(2)
            viewModel.onConversationRatingCommentChanged("Too slow")

            viewModel.onConversationRatingCancel()
            assertNull(viewModel.uiState.value.conversationRating)
            assertFalse(viewModel.onSheetDismissRequested())

            val draft = assertNotNull(viewModel.uiState.value.conversationRating)
            assertEquals(2, draft.score)
            assertEquals("Too slow", draft.comment)
        }

        @Test
        fun `skipping CSAT for a close seen live suppresses conversation rating`() = runTest {
            val viewModel = createChatAfterLiveAgentClose()

            viewModel.onLivechatRatingSkip()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `after rating the agent, writing to the assistant brings conversation rating back`() =
            runTest {
                val viewModel = createChatAfterLiveAgentClose()
                viewModel.onLivechatRatingScore(5)
                assertNull(viewModel.uiState.value.livechat.rating)
                assertTrue(viewModel.onSheetDismissRequested())

                sendAssistantMessage(viewModel)

                assertFalse(viewModel.onSheetDismissRequested())
                assertNotNull(viewModel.uiState.value.conversationRating)
            }

        @Test
        fun `cancelling the prompt keeps the chat open and asks again on the next dismiss`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingCancel()
            assertNull(viewModel.uiState.value.conversationRating)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a session expiry on rate still dismisses the sheet`() = runTest {
            coEvery { rateConversationUseCase(rating = 5, feedback = null) } returns
                Result.failure(ChatbotException.SessionExpired())
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingScore(5)
            withTimeout(1_000) { viewModel.dismissEvents.first() }
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a livechat found already closed does not suppress conversation rating`() = runTest {
            val repository = livechatRepository(
                closedSync(feedback = LivechatState.Feedback.Status.SUBMITTED)
            )
            val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
                awaitCancellation()
            }
            assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `Submit posts then dismisses and suppresses a second prompt`() {
            coEvery { rateConversationUseCase(rating = 5, feedback = null) } returns Result.success(Unit)
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingScore(5)
            coVerify(exactly = 1) { rateConversationUseCase(rating = 5, feedback = null) }
            assertNull(viewModel.uiState.value.conversationRating)
            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `livechat active allows dismiss without conversation rating`() = runTest {
            val repository = livechatRepository(livechatSync(status = LivechatState.Status.ACTIVE))
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(
                    message = any(),
                    images = any(),
                    currentPage = any(),
                    clientOperationId = any()
                )
            } returns Result.success(
                LivechatMessage(
                    messageId = "lc-1",
                    role = MessageRole.USER,
                    agent = null,
                    parts = emptyList(),
                    createdAt = "2026-01-01T00:00:00Z",
                    sequenceNumber = 1L
                )
            )
            val viewModel = createLivechatViewModel(repository, sendLivechat) { awaitCancellation() }
            assertEquals(LivechatState.Status.ACTIVE, viewModel.uiState.value.livechat.status)
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `CSAT pending for this close suppresses conversation rating`() = runTest {
            val tick = Channel<Unit>(Channel.UNLIMITED)
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
                Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            ) andThen Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            val viewModel = createLivechatViewModel(repository, mockk(relaxed = true)) {
                tick.receive()
            }
            every { sendMessageUseCase(message = "hi", images = any(), currentPage = null) } returns
                flowOf(StreamDoneEvent(message = sampleMessage(id = "reply-1")))
            tick.send(Unit)
            assertNotNull(viewModel.uiState.value.livechat.rating)
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a message the assistant refused does not offer the rating`() {
            every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
                flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))
            val viewModel = createViewModel()
            viewModel.onInputChanged("hello")
            viewModel.postMessage()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a message that never left the device does not offer the rating`() {
            every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
                flow { throw IOException("offline") }
            val viewModel = createViewModel()
            viewModel.onInputChanged("hello")
            viewModel.postMessage()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a message still on its way offers the rating`() {
            every { sendMessageUseCase(message = "hello", images = any(), currentPage = null) } returns
                MutableSharedFlow()
            val viewModel = createViewModel()
            viewModel.onInputChanged("hello")
            viewModel.postMessage()

            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a delivered message keeps the rating when a later send fails`() {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            every { sendMessageUseCase(message = "again", images = any(), currentPage = null) } returns
                flowOf(StreamErrorEvent(message = "Please try again.", retryable = true))
            viewModel.onInputChanged("again")
            viewModel.postMessage()

            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a delivered message still counts after the history is read again`() {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            viewModel.onSheetOpened()
            assertTrue(viewModel.uiState.value.conversation.isEmpty())

            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a message delivered to an agent still counts after the history is read again`() = runTest {
            val tick = Channel<Unit>(Channel.UNLIMITED)
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
                Result.success(closedSync(feedback = LivechatState.Feedback.Status.NOT_AVAILABLE))
            ) andThen Result.success(closedSync(feedback = LivechatState.Feedback.Status.NOT_AVAILABLE))
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any())
            } returns Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            tick.send(Unit)
            assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
            viewModel.onSheetOpened()
            assertTrue(viewModel.uiState.value.conversation.isEmpty())

            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a close held back behind a send leaves the dismiss to the livechat CSAT`() = runTest {
            val tick = Channel<Unit>(Channel.UNLIMITED)
            val stored = CompletableDeferred<Unit>()
            val sendLivechat = mockk<SendLivechatMessageUseCase>()
            coEvery {
                sendLivechat(message = "hi", images = any(), currentPage = null, clientOperationId = any())
            } coAnswers {
                stored.await()
                Result.success(livechatMessage(id = "server-1", role = MessageRole.USER))
            }
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(livechatSync(status = LivechatState.Status.ACTIVE)),
                Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            ) andThen Result.success(closedSync(feedback = LivechatState.Feedback.Status.PENDING))
            val viewModel = createLivechatViewModel(repository, sendLivechat) { tick.receive() }
            viewModel.onInputChanged("hi")
            viewModel.postMessage()
            tick.send(Unit)
            assertEquals(LivechatState.Status.CLOSED, viewModel.uiState.value.livechat.status)
            assertNull(viewModel.uiState.value.livechat.rating)

            assertTrue(viewModel.onSheetDismissRequested())
            stored.complete(Unit)

            assertNotNull(viewModel.uiState.value.livechat.rating)
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a reset that failed does not ask a visitor who skipped again`() {
            coEvery { renewTokenUseCase(any()) } returns Result.failure(IllegalStateException("boom"))
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            viewModel.resetConversation()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a delete that failed does not ask a visitor who rated again`() {
            coEvery { rateConversationUseCase(rating = 5, feedback = null) } returns Result.success(Unit)
            coEvery { deleteChatDataUseCase() } returns Result.failure(IllegalStateException("boom"))
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingScore(5)

            viewModel.deleteChatData()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a dismiss while a reset is in flight asks nothing`() {
            val resetResult = CompletableDeferred<Result<Unit>>()
            coEvery { renewTokenUseCase(any()) } coAnswers { resetResult.await() }
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)

            viewModel.resetConversation()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a dismiss while a delete is in flight asks nothing`() {
            val deleteResult = CompletableDeferred<Result<Unit>>()
            coEvery { deleteChatDataUseCase() } coAnswers { deleteResult.await() }
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)

            viewModel.deleteChatData()

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a reset starts the rating over`() {
            coEvery { renewTokenUseCase(any()) } returns Result.success(Unit)
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            viewModel.resetConversation()

            assertTrue(viewModel.onSheetDismissRequested())
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a data deletion starts the rating over`() {
            coEvery { deleteChatDataUseCase() } returns Result.success(Unit)
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            viewModel.deleteChatData()

            assertTrue(viewModel.onSheetDismissRequested())
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a session expiry starts the rating over`() {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            viewModel.onHistoryLoadFailed(ChatbotException.SessionExpired())
            viewModel.retry()

            assertTrue(viewModel.onSheetDismissRequested())
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `after the host invalidates the session a visitor who wrote nothing is not asked`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)

            sessionInvalidations.emit(Unit)

            assertTrue(viewModel.onSheetDismissRequested())
            assertNull(viewModel.uiState.value.conversationRating)
        }

        @Test
        fun `a comment backed out of is gone after the host invalidates the session`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingScore(2)
            viewModel.onConversationRatingCommentChanged("Too slow")
            viewModel.onConversationRatingCancel()

            sessionInvalidations.emit(Unit)
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())

            val draft = assertNotNull(viewModel.uiState.value.conversationRating)
            assertNull(draft.score)
            assertEquals("", draft.comment)
        }

        @Test
        fun `a Skip before the host invalidates the session does not silence the next one`() = runTest {
            val viewModel = createViewModel()
            sendAssistantMessage(viewModel)
            assertFalse(viewModel.onSheetDismissRequested())
            viewModel.onConversationRatingSkip()

            sessionInvalidations.emit(Unit)
            sendAssistantMessage(viewModel)

            assertFalse(viewModel.onSheetDismissRequested())
            assertNotNull(viewModel.uiState.value.conversationRating)
        }
    }

    private fun createLivechatViewModel(
        repository: ChatbotRepository,
        sendLivechatMessageUseCase: SendLivechatMessageUseCase,
        submitActionUseCase: SubmitActionUseCase = mockk(relaxed = true),
        submitLivechatFeedbackUseCase: ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase =
            mockk(relaxed = true),
        offersAttachments: Boolean = true,
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
            uiStateHolder = uiStateHolder,
            currentPage = null,
            offersAttachments = offersAttachments,
            encodeDispatcher = UnconfinedTestDispatcher(),
            createLivechatSession = { scope ->
                LivechatSession(repository = repository, scope = scope, delayMs = delayMs)
            },
            submitActionUseCase = submitActionUseCase,
            fetchFormUseCase = mockk(relaxed = true),
            getChatSessionUseCase = mockk(relaxed = true),
            patchFormValuesUseCase = mockk(relaxed = true),
            submitLivechatFeedbackUseCase = submitLivechatFeedbackUseCase,
            rateConversationUseCase = mockk(relaxed = true),
            ratingEnabled = true
        )
    }

    /** Puts a photo in the composer, as a finished pick does. Call it after creating the ViewModel. */
    private fun seedPendingPhoto(image: OutgoingImageInput = samplePendingImage()): PendingComposerAttachment {
        val pending = PendingComposerAttachment(
            image = image,
            previewUrl = "data:${image.mimeType};base64,${image.dataBase64}"
        )
        uiStateHolder.addPendingAttachment(pending)
        return pending
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
        coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
        return repository
    }

    private fun livechatSync(
        vararg messages: LivechatMessage,
        status: LivechatState.Status = LivechatState.Status.ACTIVE,
        agentName: String? = null
    ) = LivechatSync(
        state = LivechatState.of(status).copy(
            activeAgent = agentName?.let { LivechatAgent(agentId = "agent", displayName = it, avatarUrl = null) }
        ),
        messages = messages.toList(),
        lastSequenceNumber = messages.lastOrNull()?.sequenceNumber,
        hasMore = false,
        syncCursor = null
    )

    private fun collectOpenedUrls(viewModel: ChatbotViewModel): List<String> {
        val opened = mutableListOf<String>()
        collectorScope.launch { viewModel.openUrlEvents.collect { opened += it } }
        return opened
    }

    private fun attachmentMessage(url: String) = LivechatMessage(
        messageId = "m-file",
        role = MessageRole.AGENT,
        agent = null,
        parts = listOf(
            MessageFile(
                filename = "a.pdf",
                url = url,
                attachmentId = "att-1",
                mimeType = "application/pdf",
                sizeBytes = 10,
                urlExpiresAt = "2099-01-01T00:00:00Z"
            )
        ),
        createdAt = "2026-01-01T00:00:00Z",
        sequenceNumber = 2
    )

    private fun livechatMessage(
        id: String,
        role: MessageRole,
        sequence: Long = 1
    ) = LivechatMessage(
        messageId = id,
        role = role,
        agent = null,
        parts = listOf(sampleRichTextPart(id = "$id-part")),
        createdAt = "2026-01-01T00:00:00Z",
        sequenceNumber = sequence
    )

    private fun ChatbotViewModel.noteKinds() = uiState.value.conversation.mapNotNull { bubble ->
        (bubble.part as? MessagePartUiModel.LivechatNote)?.kind
    }

    private fun verifyHistoryLoads(times: Int) {
        verify(exactly = times) { getMessageHistoryUseCase(pagingConfig = any()) }
    }

    private fun ChatbotViewModel.submitFilledContactForm() {
        val form = ConversationForm.contact(
            ShowContactForm(
                partId = "contact",
                fields = listOf(
                    FormField(
                        key = "name",
                        label = "Name",
                        placeholder = null,
                        type = FormField.FieldType.TEXT,
                        required = true,
                        options = emptyList(),
                        visibleWhen = emptyList()
                    )
                )
            )
        )
        onFormEnsure(form)
        onFormValueChanged(partId = "contact", key = "name", value = "Ada")
        onFormSubmit("contact")
    }
}
