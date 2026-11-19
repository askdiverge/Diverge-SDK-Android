package ai.askdiverge.ui.compose.components.conversation

import androidx.paging.LoadState
import ai.askdiverge.domain.exception.ChatbotException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class HistoryLoadFailureTest {

    @Test
    fun `history that is still loading has no failure`() {
        val failure = historyLoadFailure(
            refresh = LoadState.Loading,
            append = LoadState.NotLoading(endOfPaginationReached = false)
        )

        assertNull(failure)
    }

    @Test
    fun `history that loaded has no failure`() {
        val failure = historyLoadFailure(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true)
        )

        assertNull(failure)
    }

    @Test
    fun `a failed first page is the failure`() {
        val sessionExpired = ChatbotException.SessionExpired()

        val failure = historyLoadFailure(
            refresh = LoadState.Error(sessionExpired),
            append = LoadState.NotLoading(endOfPaginationReached = false)
        )

        assertSame(expected = sessionExpired, actual = failure)
    }

    @Test
    fun `a failed older page is the failure`() {
        val sessionExpired = ChatbotException.SessionExpired()

        val failure = historyLoadFailure(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Error(sessionExpired)
        )

        assertSame(expected = sessionExpired, actual = failure)
    }

    @Test
    fun `a failed first page comes before a failed older page`() {
        val firstPageError = ChatbotException.SessionExpired()

        val failure = historyLoadFailure(
            refresh = LoadState.Error(firstPageError),
            append = LoadState.Error(IOException("timeout"))
        )

        assertSame(expected = firstPageError, actual = failure)
    }
}
