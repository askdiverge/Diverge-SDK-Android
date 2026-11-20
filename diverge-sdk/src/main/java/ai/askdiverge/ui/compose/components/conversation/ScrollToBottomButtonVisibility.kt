package ai.askdiverge.ui.compose.components.conversation

/**
 * Whether the jump-to-newest control shows: the list is scrolled away from the newest message and
 * has settled, the chat has more than one bubble, and the keyboard is down.
 *
 * @param canScrollBackward the list is scrolled away from the newest message.
 * @param bubbleCount the chat's bubbles, a reply in flight included.
 */
internal fun isScrollToBottomButtonVisible(
    canScrollBackward: Boolean,
    isScrollInProgress: Boolean,
    bubbleCount: Int,
    isImeVisible: Boolean
): Boolean = canScrollBackward && !isScrollInProgress && bubbleCount > 1 && !isImeVisible
