package ai.askdiverge.domain.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.turbine.test
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamStatusEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

internal class ChatbotUseCasesTest {

    private val repository = mockk<ChatbotRepository>()

    @Test
    fun `when InitializeUseCase succeeds expect the config the repository resolved`() = runTest {
        val config = mockk<Config>()
        coEvery { repository.initialize() } returns Result.success(config)

        val result = InitializeUseCase(repository).invoke()

        assertEquals(
            expected = config,
            actual = result.getOrNull()
        )

        coVerify(exactly = 1) { repository.initialize() }
    }

    @Test
    fun `when InitializeUseCase fails expect the failure passed on unchanged`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery { repository.initialize() } returns Result.failure(failure)

        val result = InitializeUseCase(repository).invoke()

        assertEquals(
            expected = failure,
            actual = result.exceptionOrNull()
        )
    }

    @Test
    fun `when StartSessionUseCase is invoked expect the repository to start a session`() = runTest {
        coEvery { repository.startSession() } returns Result.success(Unit)

        assertTrue(StartSessionUseCase(repository).invoke().isSuccess)
        coVerify(exactly = 1) { repository.startSession() }
    }

    @Test
    fun `when StartSessionUseCase fails expect the failure passed on unchanged`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery { repository.startSession() } returns Result.failure(failure)

        assertEquals(
            expected = failure,
            actual = StartSessionUseCase(repository).invoke().exceptionOrNull()
        )
    }

    @Test
    fun `when InvalidateSessionUseCase is invoked expect the repository to invalidate the session`() {
        every { repository.invalidateSession() } just Runs

        InvalidateSessionUseCase(repository).invoke()

        verify(exactly = 1) { repository.invalidateSession() }
    }

    @Test
    fun `when RenewTokenUseCase is invoked expect the repository to renew the token`() = runTest {
        coEvery { repository.renewToken() } returns Result.success(Unit)

        assertTrue(RenewTokenUseCase(repository).invoke().isSuccess)
        coVerify(exactly = 1) { repository.renewToken() }
    }

    @Test
    fun `when RenewTokenUseCase fails expect the failure passed on unchanged`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery { repository.renewToken() } returns Result.failure(failure)

        assertEquals(
            expected = failure,
            actual = RenewTokenUseCase(repository).invoke().exceptionOrNull()
        )
    }

    @Test
    fun `when DeleteChatDataUseCase is invoked expect the repository to delete the chat data`() = runTest {
        coEvery { repository.deleteChatData() } returns Result.success(Unit)

        assertTrue(DeleteChatDataUseCase(repository).invoke().isSuccess)
        coVerify(exactly = 1) { repository.deleteChatData() }
    }

    @Test
    fun `when DeleteChatDataUseCase fails expect the failure passed on unchanged`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery { repository.deleteChatData() } returns Result.failure(failure)

        assertEquals(
            expected = failure,
            actual = DeleteChatDataUseCase(repository).invoke().exceptionOrNull()
        )
    }

    @Test
    fun `when SendMessageUseCase is invoked with a page expect the message sent with it`() = runTest {
        val event = StreamStatusEvent(status = "thinking")
        every {
            repository.sendMessage(
                message = "hello",
                currentPage = "product_page"
            )
        } returns flowOf(event)

        SendMessageUseCase(repository).invoke(
            message = "hello",
            currentPage = "product_page"
        ).test {
            assertEquals(
                expected = event,
                actual = awaitItem()
            )
            awaitComplete()
        }

        verify(exactly = 1) {
            repository.sendMessage(
                message = "hello",
                currentPage = "product_page"
            )
        }
    }

    @Test
    fun `when SendMessageUseCase is invoked without a page expect the message sent without one`() = runTest {
        every {
            repository.sendMessage(
                message = "hello",
                currentPage = null
            )
        } returns flowOf()

        SendMessageUseCase(repository).invoke(
            message = "hello",
            currentPage = null
        )

        verify(exactly = 1) {
            repository.sendMessage(
                message = "hello",
                currentPage = null
            )
        }
    }

    @Test
    fun `when GetMessageHistoryUseCase is invoked expect the history paged with the caller's configuration`() = runTest {
        val pagingConfig = PagingConfig(pageSize = 10)
        every { repository.getMessageHistory(pagingConfig = pagingConfig) } returns flowOf(PagingData.empty())

        GetMessageHistoryUseCase(repository).invoke(pagingConfig = pagingConfig).test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) { repository.getMessageHistory(pagingConfig = pagingConfig) }
    }
}
