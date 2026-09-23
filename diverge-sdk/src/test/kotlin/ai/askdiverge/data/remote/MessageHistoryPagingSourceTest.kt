package ai.askdiverge.data.remote

import androidx.paging.PagingSource
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.model.message.incoming.Message
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class MessageHistoryPagingSourceTest {

    private val remoteDataSource = mockk<ChatbotRemoteDataSource>()

    private val pagingSource = MessageHistoryPagingSource(
        remoteDataSource = remoteDataSource,
        dispatcher = UnconfinedTestDispatcher()
    )

    @Test
    fun `when a page loads expect its messages and no key for newer ones`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(samplePage())

        val result = pagingSource.load(sampleRefreshParams())

        val loadedPage = assertIs<PagingSource.LoadResult.Page<String, Message>>(result)
        assertEquals(
            expected = listOf("message-1"),
            actual = loadedPage.data.map { it.id }
        )
        // The conversation only pages backwards: newer messages arrive on the stream.
        assertNull(loadedPage.prevKey)
    }

    @Test
    fun `when a page has more expect its cursor as the key of the next one`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(
            samplePage(
                hasMore = true,
                nextCursor = "cursor-2"
            )
        )

        val result = pagingSource.load(sampleRefreshParams())

        val loadedPage = assertIs<PagingSource.LoadResult.Page<String, Message>>(result)
        assertEquals(
            expected = "cursor-2",
            actual = loadedPage.nextKey
        )
    }

    @Test
    fun `when a page is the last one expect the pagination to end`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(
            samplePage(
                hasMore = false,
                nextCursor = "cursor-2"
            )
        )

        val result = pagingSource.load(sampleRefreshParams())

        val loadedPage = assertIs<PagingSource.LoadResult.Page<String, Message>>(result)
        assertNull(loadedPage.nextKey)
    }

    @Test
    fun `when a page promises more without a cursor expect the pagination to end`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(
            samplePage(
                hasMore = true,
                nextCursor = null
            )
        )

        val result = pagingSource.load(sampleRefreshParams())

        val loadedPage = assertIs<PagingSource.LoadResult.Page<String, Message>>(result)
        assertNull(loadedPage.nextKey)
    }

    @Test
    fun `when a page has a blank cursor expect the pagination to end`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(
            samplePage(
                hasMore = true,
                nextCursor = "   "
            )
        )

        val result = pagingSource.load(sampleRefreshParams())

        val loadedPage = assertIs<PagingSource.LoadResult.Page<String, Message>>(result)
        assertNull(loadedPage.nextKey)
    }

    @Test
    fun `when the first page is loaded expect it requested without a cursor`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(samplePage())

        pagingSource.load(
            sampleRefreshParams(
                key = null,
                loadSize = 10
            )
        )

        coVerify {
            remoteDataSource.getMessageHistory(
                cursor = null,
                pageSize = 10
            )
        }
    }

    @Test
    fun `when older messages are loaded expect them requested from the kept cursor`() = runTest {
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.success(samplePage())

        pagingSource.load(
            PagingSource.LoadParams.Append(
                key = "cursor-2",
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        coVerify {
            remoteDataSource.getMessageHistory(
                cursor = "cursor-2",
                pageSize = 20
            )
        }
    }

    @Test
    fun `when a load fails expect the failure reported to Paging`() = runTest {
        val failure = IllegalStateException("boom")
        coEvery {
            remoteDataSource.getMessageHistory(
                cursor = any(),
                pageSize = any()
            )
        } returns Result.failure(failure)

        val result = pagingSource.load(sampleRefreshParams())

        val error = assertIs<PagingSource.LoadResult.Error<String, Message>>(result)
        assertEquals(
            expected = failure,
            actual = error.throwable
        )
    }

    @Test
    fun `when the history is refreshed expect it to restart from the newest page`() {
        assertNull(pagingSource.getRefreshKey(mockk(relaxed = true)))
    }

    private fun samplePage(
        hasMore: Boolean = true,
        nextCursor: String? = "cursor-2",
        messages: List<MessageRemote> = listOf(
            MessageRemote(
                message_id = "message-1",
                role = MessageRoleRemote.ASSISTANT,
                parts = listOf(ImagePartRemote(url = "https://example.com/image.png"))
            )
        )
    ) = MessagePageRemote(
        has_more = hasMore,
        messages = messages,
        next_cursor = nextCursor
    )

    private fun sampleRefreshParams(
        key: String? = null,
        loadSize: Int = 10
    ) = PagingSource.LoadParams.Refresh<String>(
        key = key,
        loadSize = loadSize,
        placeholdersEnabled = false
    )
}
