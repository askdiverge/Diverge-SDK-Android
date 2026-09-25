package ai.askdiverge.ui.compose.previewprovider

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import ai.askdiverge.ui.model.ChatBubbleUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private val endOfPagination = LoadState.NotLoading(endOfPaginationReached = true)

/**
 * Empty paged history that reports end of pagination, so previews render the welcome message
 * branch of the conversation header.
 */
internal fun emptyEndOfPaginationHistory(): Flow<PagingData<ChatBubbleUiModel>> = flowOf(
    PagingData.empty(
        sourceLoadStates = LoadStates(
            refresh = endOfPagination,
            prepend = endOfPagination,
            append = endOfPagination
        )
    )
)
