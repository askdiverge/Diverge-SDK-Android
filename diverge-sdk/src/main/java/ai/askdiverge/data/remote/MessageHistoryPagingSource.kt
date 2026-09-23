package ai.askdiverge.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.data.runCatchingCancellable
import ai.askdiverge.domain.model.message.incoming.Message
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MessageHistoryPagingSource(
    private val remoteDataSource: ChatbotRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : PagingSource<String, Message>() {

    override suspend fun load(params: LoadParams<String>): LoadResult<String, Message> = runCatchingCancellable {
        val pageRemote = remoteDataSource.getMessageHistory(
            cursor = params.key,
            pageSize = params.loadSize
        ).getOrThrow()

        withContext(dispatcher) {
            val page = pageRemote.mapToDomain()
            LoadResult.Page(
                data = page.messages,
                prevKey = null,
                nextKey = page.nextCursor?.takeIf { page.hasMore && it.isNotBlank() }
            )
        }
    }.getOrElse { error -> LoadResult.Error(error) }

    // History only pages backwards (older) and refresh restarts from the newest page, so there is
    // no anchor-based refresh key to compute.
    override fun getRefreshKey(state: PagingState<String, Message>): String? = null
}
