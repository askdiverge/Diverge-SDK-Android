package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.domain.exception.ChatbotException

/**
 * Chooses what a history load Retry should do. A 401 must renew the session rather than hit the
 * same dead token again through Paging; other failures retry the failed page.
 */
internal fun onHistoryLoadRetry(
    error: Throwable?,
    onSessionExpired: () -> Unit,
    onRetryPaging: () -> Unit
) {
    when (error) {
        is ChatbotException.SessionExpired,
        is ChatbotException.NoTokenAvailable -> onSessionExpired()
        else -> onRetryPaging()
    }
}

/** True when a history [androidx.paging.LoadState.Error] means the visitor session is dead. */
internal fun Throwable.isHistorySessionExpiry(): Boolean =
    this is ChatbotException.SessionExpired || this is ChatbotException.NoTokenAvailable
