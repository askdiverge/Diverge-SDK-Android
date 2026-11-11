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
            append = LoadState.NotLoading(endOfPaginationReached = true)
        )

        assertTrue(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `loading append hides welcome even when pagination is exhausted`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Loading
        )

        assertTrue(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `end of pagination shows the welcome`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = true)
        )

        assertFalse(visibility.showLoading)
        assertTrue(visibility.showWelcome)
        assertFalse(visibility.showError)
    }

    @Test
    fun `refresh error shows welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.Error(IllegalStateException("network")),
            append = LoadState.NotLoading(endOfPaginationReached = false)
        )

        assertFalse(visibility.showLoading)
        assertTrue(visibility.showWelcome)
        assertTrue(visibility.showError)
    }

    @Test
    fun `append error shows welcome and error`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.Error(IllegalStateException("network"))
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
    fun `mid pagination without end of page hides welcome`() {
        val visibility = historyHeaderVisibility(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            append = LoadState.NotLoading(endOfPaginationReached = false)
        )

        assertFalse(visibility.showLoading)
        assertFalse(visibility.showWelcome)
        assertFalse(visibility.showError)
    }
}
