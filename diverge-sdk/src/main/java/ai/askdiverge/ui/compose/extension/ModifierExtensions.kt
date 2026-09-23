package ai.askdiverge.ui.compose.extension

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * A self-contained animated shimmer background for loading placeholders
 */
internal fun Modifier.shimmer() = composed {
    val base = MaterialTheme.colorScheme.onSurface
    val gradient = listOf(
        base.copy(alpha = 0.08f),
        base.copy(alpha = 0.16f),
        base.copy(alpha = 0.08f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                delayMillis = 100
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerTranslate"
    )

    background(
        Brush.linearGradient(
            colors = gradient,
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = translate, y = translate)
        )
    )
}
