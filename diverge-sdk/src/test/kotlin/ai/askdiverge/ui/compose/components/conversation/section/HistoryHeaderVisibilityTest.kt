package ai.askdiverge.ui.compose.components.conversation.section

import androidx.paging.LoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HistoryHeaderVisibilityTest {

    @Test
    fun `loading refresh hides welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.Loading,
            append = LoadState.NotLoading(endOfPaginationReached = true),
            historyItemCount = 0
        )

        assertTrue(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `loading append hides welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Loading,
            historyItemCount = 0
        )

        assertTrue(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `end of pagination shows the welcome above loaded messages`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true),
            historyItemCount = 5
        )

        assertFalse(visibility.showLoading)
        assertTrue(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `refresh error with no history shows welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.Error(IllegalStateException("network")),
            append = LoadState.NotLoading(endOfPaginationReached = false),
            historyItemCount = 0
        )

        assertFalse(visibility.showLoading)
        assertTrue(visibility.showWelcome)
        assertTrue(visibility.showError)
    }

    @Test
    fun `refresh error with history on screen shows error without welcome`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.Error(IllegalStateException("network")),
            append = LoadState.NotLoading(endOfPaginationReached = false),
            historyItemCount = 5
        )

        assertFalse(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertTrue(visibility.showError)
    }

    @Test
    fun `append error with no history shows welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Error(IllegalStateException("network")),
            historyItemCount = 0
        )

        assertEquals(
            expected = HistoryHeaderVisibility(
                showLoading = false,
                showError = true,
                showWelcome = true
            ),
            actual = visibility
        )
    }

    @Test
    fun `append error above loaded messages shows retry without welcome`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Error(IllegalStateException("network")),
            historyItemCount = 5
        )

        assertEquals(
            expected = HistoryHeaderVisibility(
                showLoading = false,
                showError = true,
                showWelcome = false
            ),
            actual = visibility
        )
    }

    @Test
    fun `mid pagination without end of page hides welcome`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false),
            historyItemCount = 5
        )

        assertFalse(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }
}
