package ai.askdiverge.ui.compose.components.conversation

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos

/**
 * Brings a newly arrived newest bubble into view, animated, when [shouldFollowNewestBubble] says
 * the visitor was following the newest. Otherwise the list stays where it is and the jump-to-newest
 * control offers the way down.
 *
 * The list keeps the item it showed in place by key, so a bubble that arrives below it is laid out
 * just past the bottom edge. The position is therefore read in the composition that brings the
 * bubble, before the list lays it out.
 *
 * @param listState the conversation list, laid out with `reverseLayout = true`.
 * @param newestBubbleId the newest live bubble, `null` when the conversation is empty.
 * @param isReplyInFlight a reply is awaited or streaming.
 * @param isScrollRequested a send or a restored livechat session is already scrolling to the newest.
 * @param isListShown the list is on screen; while it is not, it does not move.
 */
@Composable
internal fun FollowNewestBubbleEffect(
    listState: LazyListState,
    newestBubbleId: String?,
    isReplyInFlight: Boolean,
    isScrollRequested: Boolean,
    isListShown: Boolean
) {
    val follow = remember(listState) { NewestBubbleFollowState() }
    val followsNewest = remember(newestBubbleId) {
        // Read without observing, so scrolling does not recompose the caller.
        Snapshot.withoutReadObservation {
            shouldFollowNewestBubble(
                previousNewestId = follow.newestId,
                newestId = newestBubbleId,
                isReplyInFlight = isReplyInFlight,
                isScrollRequested = isScrollRequested,
                isFollowing = follow.runningFollows > 0,
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffsetPx = listState.firstVisibleItemScrollOffset,
                viewportHeightPx = if (isListShown) listState.layoutInfo.viewportSize.height else 0
            )
        }.also { follow.newestId = newestBubbleId }
    }

    LaunchedEffect(newestBubbleId) {
        if (!followsNewest) {
            return@LaunchedEffect
        }
        follow.runningFollows += 1
        try {
            // Waits for the frame that lays the new bubble out, so the scroll starts from there.
            withFrameNanos { }
            listState.animateScrollToItem(index = 0)
        } finally {
            follow.runningFollows -= 1
        }
    }
}

/** What [FollowNewestBubbleEffect] remembers between arrivals; read and written on the main thread. */
private class NewestBubbleFollowState {

    /** The newest live bubble the last decision was made for. */
    var newestId: String? = null

    /** Follows started and not yet finished or cancelled; a newer arrival cancels the older one. */
    var runningFollows = 0
}
