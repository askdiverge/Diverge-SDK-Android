package ai.askdiverge.ui.compose.components.conversation.section

import androidx.paging.LoadState

/**
 * What the conversation header shows from the history pager's load states.
 *
 * The welcome shows when history is exhausted, or when a history load failed and no history
 * message is on screen, so a blank chat never opens without the greeting. A failed older page
 * above loaded messages shows only the retry row. Mid-pagination [LoadState.NotLoading] without
 * end-of-pagination keeps the welcome out so it does not flash above messages that are still
 * loading.
 */
internal data class HistoryHeaderVisibility(
    val showLoading: Boolean,
    val showError: Boolean,
    val showWelcome: Boolean
)

internal fun historyHeaderVisibility(
    refresh: LoadState,
    append: LoadState,
    historyItemCount: Int
): HistoryHeaderVisibility {
    val showLoading = refresh is LoadState.Loading || append is LoadState.Loading
    if (showLoading) {
        return HistoryHeaderVisibility(
            showLoading = true,
            showError = false,
            showWelcome = false
        )
    }

    val showError = refresh is LoadState.Error || append is LoadState.Error
    val showWelcome = append.endOfPaginationReached || (showError && historyItemCount == 0)
    return HistoryHeaderVisibility(
        showLoading = false,
        showError = showError,
        showWelcome = showWelcome
    )
}
