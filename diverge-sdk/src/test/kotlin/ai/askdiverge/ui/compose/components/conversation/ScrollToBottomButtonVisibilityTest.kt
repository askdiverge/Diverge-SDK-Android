package ai.askdiverge.ui.compose.components.conversation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ScrollToBottomButtonVisibilityTest {

    @Test
    fun `scrolled away from the newest of several bubbles shows the button`() {
        assertTrue(isVisible())
    }

    @Test
    fun `at the newest message the button is hidden`() {
        assertFalse(isVisible(canScrollBackward = false))
    }

    @Test
    fun `while the list is still scrolling the button is hidden`() {
        assertFalse(isVisible(isScrollInProgress = true))
    }

    @Test
    fun `a chat with a single bubble hides the button`() {
        assertFalse(isVisible(bubbleCount = 1))
    }

    @Test
    fun `a chat with two bubbles shows the button`() {
        assertTrue(isVisible(bubbleCount = 2))
    }

    @Test
    fun `while the keyboard is up the button is hidden`() {
        assertFalse(isVisible(isImeVisible = true))
    }

    // Defaults describe the one state that shows the button, so each test changes a single input.
    private fun isVisible(
        canScrollBackward: Boolean = true,
        isScrollInProgress: Boolean = false,
        bubbleCount: Int = 5,
        isImeVisible: Boolean = false
    ): Boolean = isScrollToBottomButtonVisible(
        canScrollBackward = canScrollBackward,
        isScrollInProgress = isScrollInProgress,
        bubbleCount = bubbleCount,
        isImeVisible = isImeVisible
    )
}
