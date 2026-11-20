package ai.askdiverge.ui.rating

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.usecase.RateConversationUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.livechat.LivechatRatingModel
import ai.askdiverge.ui.sampleConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationRatingCoordinatorTest {

    @Test
    fun `a dismiss after a visitor message opens the prompt and holds the dismiss`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)

        assertFalse(sut.onDismissRequested())

        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `a 4 or 5 is posted at once and dismisses once it is stored`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } returns Result.success(Unit)
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()

        sut.selectScore(5)
        runCurrent()

        coVerify(exactly = 1) { repository.rateConversation(rating = 5, feedback = null) }
        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
        assertTrue(sut.onDismissRequested())
    }

    @Test
    fun `a 1 to 3 waits for Send and is posted with its comment`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } returns Result.success(Unit)
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()

        sut.selectScore(2)
        sut.setComment("Too slow")
        runCurrent()
        coVerify(exactly = 0) { repository.rateConversation(any(), any()) }

        sut.send()
        runCurrent()

        coVerify(exactly = 1) { repository.rateConversation(rating = 2, feedback = "Too slow") }
        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
    }

    @Test
    fun `a failure other than an expired session keeps the prompt open for another try`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } returns
            Result.failure(RuntimeException("boom")) andThen Result.success(Unit)
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(2)
        sut.setComment("Too slow")

        sut.send()
        runCurrent()

        val failed = assertNotNull(holder.uiState.value.conversationRating)
        assertEquals(LivechatRatingModel.Phase.Failed(score = 2, feedback = "Too slow"), failed.phase)
        assertEquals("Too slow", failed.comment)
        assertEquals(0, dismissals)

        sut.send()
        runCurrent()

        coVerify(exactly = 2) { repository.rateConversation(rating = 2, feedback = "Too slow") }
        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
    }

    @Test
    fun `an expired session closes the prompt, dismisses and expires the chat`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } returns
            Result.failure(ChatbotException.SessionExpired())
        var dismissals = 0
        var expiries = 0
        val holder = readyHolder()
        val sut = makeSUT(
            holder,
            repository,
            onSessionExpired = { expiries++ },
            onReadyToDismiss = { dismissals++ }
        )
        sut.onDismissRequested()

        sut.selectScore(4)
        runCurrent()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
        assertEquals(1, expiries)
    }

    @Test
    fun `Skip while the rating is on its way dismisses at once, and only once`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()
        assertEquals(true, holder.uiState.value.conversationRating?.isSubmitting)

        sut.skip()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)

        answer.complete(Result.success(Unit))
        runCurrent()

        assertEquals(1, dismissals)
        assertTrue(sut.onDismissRequested())
    }

    @Test
    fun `back while the rating is on its way closes the chat instead of returning to it`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()

        sut.cancel()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)

        answer.complete(Result.success(Unit))
        runCurrent()

        assertEquals(1, dismissals)
        assertTrue(sut.onDismissRequested())
    }

    @Test
    fun `a rating refused after the visitor left the prompt shows nothing`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()
        sut.skip()

        answer.complete(Result.failure(RuntimeException("boom")))
        runCurrent()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
    }

    @Test
    fun `an expired session found after the visitor left still expires the chat`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        var expiries = 0
        val holder = readyHolder()
        val sut = makeSUT(
            holder,
            repository,
            onSessionExpired = { expiries++ },
            onReadyToDismiss = { dismissals++ }
        )
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()
        sut.skip()

        answer.complete(Result.failure(ChatbotException.SessionExpired()))
        runCurrent()

        assertEquals(1, dismissals)
        assertEquals(1, expiries)
    }

    @Test
    fun `Skip after a failed send posts nothing more, dismisses once and leaves the rating open`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } returns
            Result.failure(RuntimeException("boom"))
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()
        assertEquals(
            LivechatRatingModel.Phase.Failed(score = 5, feedback = null),
            holder.uiState.value.conversationRating?.phase
        )

        sut.skip()
        runCurrent()

        coVerify(exactly = 1) { repository.rateConversation(any(), any()) }
        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
        assertFalse(sut.onDismissRequested())
        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `Skip on the comment step posts nothing, dismisses once and leaves the rating open`() = runTest {
        val repository = mockk<ChatbotRepository>()
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(2)
        sut.setComment("Too slow")

        sut.skip()
        runCurrent()

        coVerify(exactly = 0) { repository.rateConversation(any(), any()) }
        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
        assertFalse(sut.onDismissRequested())
        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `a failed message with none delivered closes the prompt and keeps the chat open`() = runTest {
        var hasVisitorMessage = true
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(
            holder,
            hasVisitorMessage = { hasVisitorMessage },
            onReadyToDismiss = { dismissals++ }
        )
        sut.onDismissRequested()
        sut.selectScore(2)
        hasVisitorMessage = false

        sut.onVisitorMessageFailed()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(0, dismissals)
        hasVisitorMessage = true
        assertFalse(sut.onDismissRequested())
        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `a failed message leaves the prompt when another was delivered`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)
        sut.onDismissRequested()

        sut.onVisitorMessageFailed()

        assertNotNull(holder.uiState.value.conversationRating)
    }

    @Test
    fun `a failed message leaves a rating on its way alone`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var hasVisitorMessage = true
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(
            holder,
            repository,
            hasVisitorMessage = { hasVisitorMessage },
            onReadyToDismiss = { dismissals++ }
        )
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()
        hasVisitorMessage = false

        sut.onVisitorMessageFailed()

        assertEquals(true, holder.uiState.value.conversationRating?.isSubmitting)

        answer.complete(Result.success(Unit))
        runCurrent()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(1, dismissals)
    }

    @Test
    fun `a new presentation closes an unsent prompt without dismissing and a later dismiss asks again`() = runTest {
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(2)
        sut.setComment("Too slow")

        sut.onNewPresentation()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(0, dismissals)
        assertFalse(sut.onDismissRequested())
        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `a rating on its way at a new presentation is stored without dismissing`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()

        sut.onNewPresentation()

        assertNull(holder.uiState.value.conversationRating)

        answer.complete(Result.success(Unit))
        runCurrent()

        assertEquals(0, dismissals)
        assertTrue(sut.onDismissRequested())
        assertNull(holder.uiState.value.conversationRating)
    }

    @Test
    fun `back before a rating is sent keeps the chat open and the draft`() = runTest {
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(2)
        sut.setComment("Too slow")

        sut.cancel()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(0, dismissals)
        assertFalse(sut.onDismissRequested())
        val draft = assertNotNull(holder.uiState.value.conversationRating)
        assertIs<LivechatRatingModel.Phase.Feedback>(draft.phase)
        assertEquals("Too slow", draft.comment)
    }

    @Test
    fun `nothing is offered while the conversation is being reset`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)
        holder.onConversationResetStarted()

        assertTrue(sut.onDismissRequested())

        assertNull(holder.uiState.value.conversationRating)
    }

    @Test
    fun `nothing is offered while the visitor's data is being deleted`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)
        holder.onDeleteChatDataStarted()

        assertTrue(sut.onDismissRequested())

        assertNull(holder.uiState.value.conversationRating)
    }

    @Test
    fun `clear forgets a Skip`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)
        sut.onDismissRequested()
        sut.skip()
        assertTrue(sut.onDismissRequested())

        sut.clear()

        assertFalse(sut.onDismissRequested())
        assertNotNull(holder.uiState.value.conversationRating)
    }

    @Test
    fun `clear forgets a draft the visitor backed out of`() = runTest {
        val holder = readyHolder()
        val sut = makeSUT(holder)
        sut.onDismissRequested()
        sut.selectScore(2)
        sut.setComment("Too slow")
        sut.cancel()

        sut.clear()

        assertFalse(sut.onDismissRequested())
        assertEquals(LivechatRatingModel(), holder.uiState.value.conversationRating)
    }

    @Test
    fun `clear drops a rating on its way without dismissing`() = runTest {
        val answer = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.rateConversation(any(), any()) } coAnswers { answer.await() }
        var dismissals = 0
        val holder = readyHolder()
        val sut = makeSUT(holder, repository, onReadyToDismiss = { dismissals++ })
        sut.onDismissRequested()
        sut.selectScore(5)
        runCurrent()

        sut.clear()
        answer.complete(Result.success(Unit))
        runCurrent()

        assertNull(holder.uiState.value.conversationRating)
        assertEquals(0, dismissals)
        assertFalse(sut.onDismissRequested())
    }

    private fun readyHolder() = ChatbotUiStateHolder().apply { onConfigLoaded(sampleConfig()) }

    private fun TestScope.makeSUT(
        holder: ChatbotUiStateHolder,
        repository: ChatbotRepository = mockk(relaxed = true),
        hasVisitorMessage: () -> Boolean = { true },
        onSessionExpired: () -> Unit = {},
        onReadyToDismiss: () -> Unit = {}
    ) = ConversationRatingCoordinator(
        uiStateHolder = holder,
        rateConversationUseCase = RateConversationUseCase(repository),
        scope = this,
        ratingEnabled = true,
        hasVisitorMessage = hasVisitorMessage,
        isLivechatBlocking = { false },
        livechatOwnsClose = { false },
        onSessionExpired = onSessionExpired,
        onReadyToDismiss = onReadyToDismiss
    )
}
