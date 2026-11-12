package ai.askdiverge.ui.livechat

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class LivechatRatingCoordinatorTest {

    @Test
    fun `a live close with pending feedback offers the prompt`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder)

        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        assertNotNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `a restored close does not offer the prompt`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder)

        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = true
        )

        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `skip sends nothing and a later close can ask again`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns Result.success(Unit)
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        assertNotNull(holder.uiState.value.livechat.rating)

        sut.skip()
        assertNull(holder.uiState.value.livechat.rating)
        coVerify(exactly = 0) { repository.submitLivechatFeedback(any(), any()) }

        sut.update(
            previousStatus = LivechatState.Status.CLOSED,
            state = LivechatState.of(LivechatState.Status.ACTIVE),
            isRestored = false
        )
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        assertNotNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `a 409 dismisses the prompt without an error`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery {
            repository.submitLivechatFeedback(any(), any())
        } returns Result.failure(LivechatRequestException.Conflict())
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(5)
        runCurrent()

        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `other failures stay up until a submitted snapshot clears them`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery {
            repository.submitLivechatFeedback(any(), any())
        } returns Result.failure(RuntimeException("boom"))
        val refreshes = mutableListOf<Unit>()
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository, refresh = { refreshes += Unit })
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(5)
        runCurrent()

        assertIs<LivechatRatingModel.Phase.Failed>(holder.uiState.value.livechat.rating?.phase)
        assertEquals(1, refreshes.size)

        sut.update(
            previousStatus = LivechatState.Status.CLOSED,
            state = closed(pending = false),
            isRestored = false
        )
        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `a 401 expires the session`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery {
            repository.submitLivechatFeedback(any(), any())
        } returns Result.failure(ChatbotException.SessionExpired())
        var expired = 0
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository, onSessionExpired = { expired += 1 })
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(4)
        runCurrent()

        assertEquals(1, expired)
        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `late success after a new session does not block the next close`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(4)
        runCurrent()
        sut.update(
            previousStatus = LivechatState.Status.CLOSED,
            state = LivechatState.of(LivechatState.Status.ACTIVE),
            isRestored = false
        )
        gate.complete(Unit)
        runCurrent()

        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        assertNotNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `an off-screen close waits until the chat is visible`() = runTest {
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder)
        sut.setVisible(false)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        assertNull(holder.uiState.value.livechat.rating)

        sut.setVisible(true)

        assertNotNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `scores 4 and 5 post immediately and 1 to 3 ask for a comment`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns Result.success(Unit)
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(2)
        val draft = assertNotNull(holder.uiState.value.livechat.rating)
        assertEquals(true, draft.needsComment)
        coVerify(exactly = 0) { repository.submitLivechatFeedback(any(), any()) }

        sut.setComment("  Too slow ")
        sut.send()
        runCurrent()

        coVerify { repository.submitLivechatFeedback(rating = 2, feedback = "  Too slow ") }
        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `a 4 or 5 sends that score with no comment`() = runTest {
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } returns Result.success(Unit)
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository)
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )

        sut.selectScore(4)
        runCurrent()

        coVerify(exactly = 1) { repository.submitLivechatFeedback(rating = 4, feedback = null) }
        assertNull(holder.uiState.value.livechat.rating)
    }

    @Test
    fun `a failure after the feedback stopped pending closes the prompt`() = runTest {
        val sent = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } coAnswers { sent.await() }
        val refreshes = mutableListOf<Unit>()
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository, refresh = { refreshes += Unit })
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        sut.selectScore(5)
        runCurrent()
        sut.update(
            previousStatus = LivechatState.Status.CLOSED,
            state = closed(pending = false),
            isRestored = false
        )

        sent.complete(Result.failure(RuntimeException("timeout")))
        runCurrent()
        sut.setVisible(true)

        assertNull(holder.uiState.value.livechat.rating)
        assertEquals(0, refreshes.size)
    }

    @Test
    fun `a failure for a prompt that has gone shows nothing`() = runTest {
        val sent = CompletableDeferred<Result<Unit>>()
        val repository = mockk<ChatbotRepository>()
        coEvery { repository.submitLivechatFeedback(any(), any()) } coAnswers { sent.await() }
        val refreshes = mutableListOf<Unit>()
        val holder = ChatbotUiStateHolder()
        val sut = makeSUT(holder, repository, refresh = { refreshes += Unit })
        sut.update(
            previousStatus = LivechatState.Status.ACTIVE,
            state = closed(pending = true),
            isRestored = false
        )
        sut.selectScore(5)
        runCurrent()
        holder.onSessionExpired()

        sent.complete(Result.failure(RuntimeException("timeout")))
        runCurrent()

        assertNull(holder.uiState.value.livechat.rating)
        assertEquals(0, refreshes.size)
    }

    private fun TestScope.makeSUT(
        holder: ChatbotUiStateHolder,
        repository: ChatbotRepository = mockk(relaxed = true),
        onSessionExpired: () -> Unit = {},
        refresh: suspend () -> Unit = {}
    ) = LivechatRatingCoordinator(
        uiStateHolder = holder,
        submitLivechatFeedbackUseCase = SubmitLivechatFeedbackUseCase(repository),
        scope = this,
        onSessionExpired = onSessionExpired,
        refresh = refresh
    )

    private fun closed(pending: Boolean) = LivechatState.of(
        status = LivechatState.Status.CLOSED,
        feedback = if (pending) {
            LivechatState.Feedback.Status.PENDING
        } else {
            LivechatState.Feedback.Status.SUBMITTED
        }
    )
}
