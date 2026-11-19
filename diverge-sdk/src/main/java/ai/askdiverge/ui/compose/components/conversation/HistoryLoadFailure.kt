package ai.askdiverge.ui.compose.components.conversation

import androidx.paging.LoadState

/**
 * The error the history pager failed with, from its load states: the failed first page, else the
 * failed older page. `null` while neither load has failed.
 */
internal fun historyLoadFailure(
    refresh: LoadState,
    append: LoadState
): Throwable? = (refresh as? LoadState.Error)?.error ?: (append as? LoadState.Error)?.error
