package ai.askdiverge.ui.compose.extension

import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Measures the input bar and writes its height to [inputBarHeightPx] there and then, rather than
 * after layout the way [androidx.compose.ui.layout.onSizeChanged] would.
 *
 * The scaffold measures its bottom bar before its content, so content laid out in the same pass
 * already sees the height the input bar just took, rather than the height it had a frame ago.
 */
internal fun Modifier.measureInputBarHeight(inputBarHeightPx: MutableIntState): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    inputBarHeightPx.intValue = placeable.height
    layout(placeable.width, placeable.height) {
        placeable.place(
            x = 0,
            y = 0
        )
    }
}
