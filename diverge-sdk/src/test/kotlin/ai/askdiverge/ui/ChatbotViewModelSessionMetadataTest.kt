package ai.askdiverge.ui

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.SessionMetadataRequestException
import ai.askdiverge.domain.livechat.LivechatSession
import ai.askdiverge.domain.metadata.ScriptedPatcher
import ai.askdiverge.domain.metadata.SessionMetadataQueue
import ai.askdiverge.domain.metadata.screen
import ai.askdiverge.domain.metadata.signed
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.usecase.DeleteChatDataUseCase
import ai.askdiverge.domain.usecase.GetLatestMessageUseCase
import ai.askdiverge.domain.usecase.GetMessageHistoryUseCase
import ai.askdiverge.domain.usecase.GetSessionInvalidationsUseCase
import ai.askdiverge.domain.usecase.InitializeUseCase
import ai.askdiverge.domain.usecase.InvalidateSessionUseCase
import ai.askdiverge.domain.usecase.PatchSessionMetadataUseCase
import ai.askdiverge.domain.usecase.RenewTokenUseCase
import ai.askdiverge.domain.usecase.SendMessageUseCase
import ai.askdiverge.domain.usecase.StartSessionUseCase
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.streaming.DefaultPartBuilderFactory
import ai.askdiverge.ui.streaming.StreamingMessageBuilder
import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatbotViewModelSessionMetadataTest {

    private val initializeUseCase: InitializeUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(sampleConfig())
    }
    private val sendMessageUseCase: SendMessageUseCase = mockk {
        every { this@mockk.invoke(message = any(), images = any(), currentPage = any()) } returns
            flowOf(StreamDoneEvent(message = sampleMessage(id = "reply-1")))
    }
    private val startSessionUseCase: StartSessionUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(Unit)
    }
    private val invalidateSessionUseCase = mockk<InvalidateSessionUseCase>(relaxed = true)
    private val sessionInvalidations = MutableSharedFlow<Unit>()
    private val getSessionInvalidationsUseCase: GetSessionInvalidationsUseCase = mockk {
        every { this@mockk.invoke() } returns sessionInvalidations
    }
    private val renewTokenUseCase: RenewTokenUseCase = mockk {
        coEvery { this@mockk.invoke(any()) } returns Result.success(Unit)
    }
    private val deleteChatDataUseCase: DeleteChatDataUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(Unit)
    }
    private val getMessageHistoryUseCase: GetMessageHistoryUseCase = mockk {
        every { this@mockk.invoke(pagingConfig = any(), onRefreshLoaded = any()) } returns
            flowOf(PagingData.empty())
    }
    private val getLatestMessageUseCase: GetLatestMessageUseCase = mockk {
        coEvery { this@mockk.invoke() } returns Result.success(sampleMessage(id = "message-1"))
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Showing the chat")
    inner class Showing {

        @Test
        fun `opening the chat sends the metadata set before it`() {
            val (queue, patcher) = makeQueue()
            queue.enqueue(screen("home"))
            queue.enqueue(signed("token"))

            val viewModel = makeSUT(queue, patcher)
            assertEquals(0, patcher.attempts)
            viewModel.onChatVisible()

            assertEquals(listOf(screen("home"), signed("token")), patcher.sent)
        }

        @Test
        fun `metadata waits while the chat is off screen and goes when it is back`() {
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("home"))
            assertEquals(1, patcher.attempts)

            viewModel.onChatHidden()
            queue.enqueue(screen("checkout"))
            assertEquals(1, patcher.attempts)

            viewModel.onChatVisible()
            assertEquals(listOf(screen("home"), screen("checkout")), patcher.sent)
        }

        @Test
        fun `metadata waits while the chat connects and goes once it is ready`() {
            val connected = CompletableDeferred<Unit>()
            coEvery { initializeUseCase() } coAnswers {
                connected.await()
                Result.success(sampleConfig())
            }
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("home"))
            assertEquals(0, patcher.attempts)

            connected.complete(Unit)

            assertEquals(listOf(screen("home")), patcher.sent)
        }

        @Test
        fun `every sheet given one ChatbotSessionMetadata uses its queue`() {
            val (queue, first) = makeQueue()
            val second = ScriptedPatcher()
            val firstChat = makeSUT(queue, first)
            val secondChat = makeSUT(queue, second)
            firstChat.onChatVisible()
            firstChat.onChatHidden()

            secondChat.onChatVisible()
            queue.enqueue(screen("checkout"))

            assertEquals(0, first.attempts)
            assertEquals(listOf(screen("checkout")), second.sent)
        }

        @Test
        fun `a chat whose ViewModel is cleared sends nothing more`() {
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            val store = ViewModelStore().apply { put("chat", viewModel) }

            store.clear()
            queue.enqueue(screen("checkout"))

            assertEquals(0, patcher.attempts)
        }
    }

    @Nested
    @DisplayName("Sending")
    inner class Sending {

        @Test
        fun `a send goes at once while metadata is on its way`() {
            val (queue, patcher) = makeQueue(gate = CompletableDeferred())
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("checkout"))

            viewModel.onInputChanged("Where is my order?")
            viewModel.postMessage()

            verify(exactly = 1) {
                sendMessageUseCase(message = "Where is my order?", images = any(), currentPage = any())
            }
            assertTrue(patcher.sent.isEmpty())
        }

        @Test
        fun `a send sends again the metadata a failure left queued`() {
            val (queue, patcher) = makeQueue(failures = mapOf(0 to SessionMetadataRequestException(503)))
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("checkout"))
            assertEquals(1, patcher.attempts)

            viewModel.onInputChanged("Hello")
            viewModel.postMessage()

            assertEquals(2, patcher.attempts)
            assertEquals(listOf(screen("checkout")), patcher.sent)
        }

        @Test
        fun `a 401 on this call leaves the connection Ready`() {
            val (queue, patcher) = makeQueue(failures = mapOf(0 to SessionMetadataRequestException(401)))
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()

            queue.enqueue(signed("token"))

            assertEquals(1, patcher.attempts)
            assertEquals(ChatConnectionState.Ready, viewModel.uiState.value.session.connectionState)
            verify(exactly = 0) { invalidateSessionUseCase() }
            coVerify(exactly = 0) { renewTokenUseCase(any()) }
        }
    }

    @Nested
    @DisplayName("Losing the session")
    inner class LosingTheSession {

        @Test
        fun `metadata set while the chat reconnects waits for the new connection`() = runTest {
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            val reconnected = CompletableDeferred<Unit>()
            coEvery { initializeUseCase() } coAnswers {
                reconnected.await()
                Result.success(sampleConfig())
            }

            sessionInvalidations.emit(Unit)
            queue.enqueue(screen("signed in"))
            assertEquals(0, patcher.attempts)

            reconnected.complete(Unit)
            assertEquals(listOf(screen("signed in")), patcher.sent)
        }

        @Test
        fun `a reset keeps the chat attached and sends nothing already applied again`() {
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("home"))

            viewModel.resetConversation()
            queue.enqueue(screen("after reset"))

            assertEquals(listOf(screen("home"), screen("after reset")), patcher.sent)
        }
    }

    @Nested
    @DisplayName("Deleting the visitor's data")
    inner class Deleting {

        @Test
        fun `deleting the visitor's data drops metadata still queued`() {
            val (queue, patcher) = makeQueue(failures = mapOf(0 to SessionMetadataRequestException(503)))
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()
            queue.enqueue(screen("checkout"))
            assertEquals(1, patcher.attempts)

            viewModel.deleteChatData()

            assertEquals(1, patcher.attempts)
            assertTrue(patcher.sent.isEmpty())
        }

        @Test
        fun `metadata set while the visitor's data is being deleted is dropped with it, unsent`() {
            val deleted = CompletableDeferred<Unit>()
            coEvery { deleteChatDataUseCase() } coAnswers {
                deleted.await()
                Result.success(Unit)
            }
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()

            viewModel.deleteChatData()
            queue.enqueue(screen("checkout"))
            deleted.complete(Unit)

            assertEquals(0, patcher.attempts)
        }

        @Test
        fun `a delete that fails sends the metadata set during it`() {
            val deleted = CompletableDeferred<Result<Unit>>()
            coEvery { deleteChatDataUseCase() } coAnswers { deleted.await() }
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()

            viewModel.deleteChatData()
            queue.enqueue(screen("checkout"))
            assertEquals(0, patcher.attempts)
            deleted.complete(Result.failure(IllegalStateException("host failed")))

            assertEquals(listOf(screen("checkout")), patcher.sent)
        }

        @Test
        fun `metadata set after a delete goes to the new session once it has started`() {
            val started = CompletableDeferred<Result<Unit>>()
            coEvery { startSessionUseCase() } coAnswers { started.await() }
            val (queue, patcher) = makeQueue()
            val viewModel = makeSUT(queue, patcher)
            viewModel.onChatVisible()

            viewModel.deleteChatData()
            queue.enqueue(screen("new session"))
            assertEquals(0, patcher.attempts)
            started.complete(Result.success(Unit))

            assertEquals(listOf(screen("new session")), patcher.sent)
        }
    }

    private fun makeQueue(
        failures: Map<Int, Throwable> = emptyMap(),
        gate: CompletableDeferred<Unit>? = null
    ): Pair<SessionMetadataQueue, ScriptedPatcher> =
        SessionMetadataQueue() to ScriptedPatcher(failures = failures, gate = gate)

    private fun makeSUT(
        sessionMetadata: SessionMetadataQueue,
        patcher: ScriptedPatcher
    ): ChatbotViewModel = ChatbotViewModel(
        initializeUseCase = initializeUseCase,
        sendMessageUseCase = sendMessageUseCase,
        sendLivechatMessageUseCase = mockk(relaxed = true),
        startSessionUseCase = startSessionUseCase,
        invalidateSessionUseCase = invalidateSessionUseCase,
        getSessionInvalidationsUseCase = getSessionInvalidationsUseCase,
        renewTokenUseCase = renewTokenUseCase,
        deleteChatDataUseCase = deleteChatDataUseCase,
        exportMyDataUseCase = mockk(relaxed = true),
        fetchBannersUseCase = mockk {
            coEvery { this@mockk.invoke(pageUrl = any()) } returns Result.success(emptyList())
        },
        getMessageHistoryUseCase = getMessageHistoryUseCase,
        getLatestMessageUseCase = getLatestMessageUseCase,
        patchSessionMetadataUseCase = mockk<PatchSessionMetadataUseCase> {
            coEvery { this@mockk.invoke(any()) } coAnswers { patcher.patch(firstArg()) }
        },
        sessionMetadata = sessionMetadata,
        streamingMessageBuilder = StreamingMessageBuilder(DefaultPartBuilderFactory()),
        uiStateHolder = ChatbotUiStateHolder(),
        currentPage = null,
        offersAttachments = false,
        encodeDispatcher = UnconfinedTestDispatcher(),
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
        submitLivechatFeedbackUseCase = mockk(relaxed = true),
        rateConversationUseCase = mockk(relaxed = true),
        ratingEnabled = false
    )
}
