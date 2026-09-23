package ai.askdiverge.ui.compose.components.conversation.collapsingspace

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * How much of a lazy list item's trailing space has been collapsed, and the scroll handling that
 * collapses it, in the way a collapsing toolbar does. It only ever collapses, never grows back.
 *
 * The height fields are written during layout and read during scrolling, never during composition.
 *
 * Assumes a list with `reverseLayout = true`, where a downward drag reaches the older items.
 */
internal class CollapsingSpaceState(private val listState: LazyListState) {

    // Written while the space is laid out, and read only when a scroll is spent against it.
    private var viewportHeightPx = 0f
    private var contentHeightPx = 0f

    /** How much of the space has been collapsed so far. */
    internal var collapsedPx by mutableFloatStateOf(0f)
        private set

    private val remainingPx: Float
        get() = (viewportHeightPx - contentHeightPx - collapsedPx).coerceAtLeast(0f)

    /** Records the height a whole viewport gives the space, measured as the space is laid out. */
    internal fun updateViewportHeight(heightPx: Float) {
        viewportHeightPx = heightPx
    }

    /** Records the height of the content beside the space, the point past which the space is used up. */
    internal fun updateContentHeight(heightPx: Float) {
        contentHeightPx = heightPx
    }

    /**
     * Spends as much of [availablePx] on collapsing the space as it has left to give, and answers
     * with how much that was, so the rest can go on to scroll the list.
     */
    private fun collapse(availablePx: Float): Float {
        val consumedPx = availablePx.coerceAtMost(remainingPx)
        collapsedPx += consumedPx
        return consumedPx
    }

    /**
     * Consumes a scroll towards the older items into [collapse] before the list sees it,
     * and only while the list rests at its newest end.
     */
    internal val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val towardsOlderItemsPx = available.y
            return if (towardsOlderItemsPx <= 0f || listState.canScrollBackward) {
                Offset.Zero
            } else {
                Offset(
                    x = 0f,
                    y = collapse(towardsOlderItemsPx)
                )
            }
        }
    }
}
