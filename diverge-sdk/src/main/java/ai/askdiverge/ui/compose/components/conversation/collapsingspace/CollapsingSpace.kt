package ai.askdiverge.ui.compose.components.conversation.collapsingspace

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.IntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * Shrinks this by however much [spaceState] has collapsed so far, during layout so the list moves in
 * the same frame as the scroll.
 *
 * Chain before `LazyItemScope.fillParentMaxHeight()`, which supplies the height to shrink from.
 *
 * @param spaceState how much is collapsed already, and where the measured viewport height is
 * recorded.
 * @param keyboardInsets read for its invalidation only, never its value, so that a keyboard change
 * re-measures in the same pass.
 * @param inputBarHeightPx read for its invalidation only, never its value, so that an input bar
 * resize re-measures in the same pass.
 */
internal fun Modifier.collapsingSpace(
    spaceState: CollapsingSpaceState,
    keyboardInsets: WindowInsets,
    inputBarHeightPx: IntState
): Modifier = layout { measurable, constraints ->
    keyboardInsets.getBottom(this)
    inputBarHeightPx.intValue
    val placeable = measurable.measure(constraints)
    spaceState.updateViewportHeight(placeable.height.toFloat())
    val heightPx = (placeable.height - spaceState.collapsedPx)
        .roundToInt()
        .coerceAtLeast(0)
    layout(placeable.width, heightPx) {
        placeable.place(
            x = 0,
            y = 0
        )
    }
}
