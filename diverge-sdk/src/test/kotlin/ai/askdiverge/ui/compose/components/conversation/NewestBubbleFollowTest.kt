package ai.askdiverge.ui.compose.components.conversation

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NewestBubbleFollowTest {

    @Nested
    @DisplayName("Where the visitor was when the bubble arrived")
    inner class Position {

        @Test
        fun `at the newest the list follows the new bubble`() {
            assertTrue(shouldFollow())
        }

        @Test
        fun `a little way up the newest item the list follows`() {
            assertTrue(shouldFollow(scrollOffsetPx = 300))
        }

        @Test
        fun `exactly 70 percent of the viewport below the edge still follows`() {
            assertTrue(shouldFollow(scrollOffsetPx = 700, viewportHeightPx = 1_000))
        }

        @Test
        fun `more than 70 percent of the viewport below the edge stays`() {
            assertFalse(shouldFollow(scrollOffsetPx = 701, viewportHeightPx = 1_000))
        }

        @Test
        fun `with the newest item wholly below the edge the list stays`() {
            assertFalse(shouldFollow(firstVisibleItemIndex = 1, scrollOffsetPx = 0))
        }

        @Test
        fun `far up the history the list stays`() {
            assertFalse(shouldFollow(firstVisibleItemIndex = 12, scrollOffsetPx = 40))
        }

        @Test
        fun `while still moving to the newest for an earlier arrival the list keeps following`() {
            assertTrue(shouldFollow(isFollowing = true, firstVisibleItemIndex = 1, scrollOffsetPx = 80))
        }
    }

    @Nested
    @DisplayName("What changed")
    inner class Change {

        @Test
        fun `the first bubble in a chat that showed only history is followed`() {
            assertTrue(shouldFollow(previousNewestId = null))
        }

        @Test
        fun `an older history page leaves the newest bubble and the list alone`() {
            assertFalse(shouldFollow(previousNewestId = NEWEST_ID))
        }

        @Test
        fun `an emptied conversation leaves the list alone`() {
            assertFalse(shouldFollow(newestId = null))
        }

        @Test
        fun `before the list's first layout it stays, opening at the newest`() {
            assertFalse(shouldFollow(previousNewestId = null, viewportHeightPx = 0))
        }

        @Test
        fun `a part of a reply in flight leaves the list to the reply space`() {
            assertFalse(shouldFollow(isReplyInFlight = true))
        }

        @Test
        fun `a send or restore that already scrolls to the newest is left to do so`() {
            assertFalse(shouldFollow(isScrollRequested = true))
        }
    }

    // Defaults describe a visitor at the newest when a new bubble arrives, so each test changes the
    // inputs it is about.
    private fun shouldFollow(
        previousNewestId: String? = "previous",
        newestId: String? = NEWEST_ID,
        isReplyInFlight: Boolean = false,
        isScrollRequested: Boolean = false,
        isFollowing: Boolean = false,
        firstVisibleItemIndex: Int = 0,
        scrollOffsetPx: Int = 0,
        viewportHeightPx: Int = 1_000
    ): Boolean = shouldFollowNewestBubble(
        previousNewestId = previousNewestId,
        newestId = newestId,
        isReplyInFlight = isReplyInFlight,
        isScrollRequested = isScrollRequested,
        isFollowing = isFollowing,
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffsetPx = scrollOffsetPx,
        viewportHeightPx = viewportHeightPx
    )

    private companion object {
        const val NEWEST_ID = "newest"
    }
}
