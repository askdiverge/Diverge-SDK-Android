package ai.askdiverge.ui.compose.components.conversation

/** How much of the viewport height, in percent, may lie below it for the visitor to follow the newest. */
private const val FOLLOW_DISTANCE_PERCENT = 70L

/**
 * Whether the list brings a new newest bubble into view. It does when the newest bubble changed,
 * nothing else scrolls to it already, and the visitor was following the newest, judged from where
 * the list stood before the bubble arrived.
 *
 * The visitor follows the newest while the newest item is in view with no more than 70% of the
 * viewport height of it below the bottom edge, and while the list is still moving to the newest for
 * an earlier arrival. A list not laid out yet does not move: it opens at the newest.
 *
 * @param previousNewestId the newest live bubble before the change, `null` when there was none.
 * @param newestId the newest live bubble now, `null` when the conversation is empty.
 * @param isReplyInFlight a reply is awaited or streaming; the send scrolled to it, and it fills the
 * space reserved under the visitor's message.
 * @param isScrollRequested a send or a restored livechat session is already scrolling to the newest.
 * @param isFollowing the list is still moving to the newest for an earlier arrival.
 * @param firstVisibleItemIndex the lowest index in view, where `0` is the newest item.
 * @param firstVisibleItemScrollOffsetPx how much of that item lies below the bottom edge.
 * @param viewportHeightPx the height of the list's viewport, `0` before its first layout.
 */
internal fun shouldFollowNewestBubble(
    previousNewestId: String?,
    newestId: String?,
    isReplyInFlight: Boolean,
    isScrollRequested: Boolean,
    isFollowing: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    viewportHeightPx: Int
): Boolean {
    if (newestId == null || newestId == previousNewestId || isReplyInFlight || isScrollRequested) {
        return false
    }
    if (isFollowing) {
        return true
    }
    if (viewportHeightPx <= 0 || firstVisibleItemIndex > 0) {
        return false
    }
    return firstVisibleItemScrollOffsetPx * 100L <= viewportHeightPx * FOLLOW_DISTANCE_PERCENT
}
