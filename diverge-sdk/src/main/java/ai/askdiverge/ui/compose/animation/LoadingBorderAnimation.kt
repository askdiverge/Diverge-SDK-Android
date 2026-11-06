package ai.askdiverge.ui.compose.animation

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.theme.LocalThinkingBorderGradientColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme

/**
 * A self-contained animated gradient border, drawn as a rotating sweep gradient.
 * Draws nothing when `colors` is empty.
 */
internal fun Modifier.loadingBorder(
    colors: List<Color> = emptyList(),
    width: Dp = 1.dp,
    shape: Shape = RectangleShape,
    durationMillis: Int = 2500
) = composed {
    val resolvedColors = colors.ifEmpty { LocalThinkingBorderGradientColors.current }
    if (resolvedColors.isEmpty()) {
        this
    } else {
        val closedColors = resolvedColors + resolvedColors.first()

        val transition = rememberInfiniteTransition(label = "loadingBorder")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = LinearEasing)
            ),
            label = "loadingBorderAngle"
        )

        drawWithCache {
            val strokeWidthPx = width.toPx()
            val inset = strokeWidthPx / 2f
            val insetSize = Size(
                width = size.width - strokeWidthPx,
                height = size.height - strokeWidthPx
            )
            val outline = shape.createOutline(insetSize, layoutDirection, this)
            val center = Offset(x = insetSize.width / 2f, y = insetSize.height / 2f)
            val argbColors = closedColors.map { it.toArgb() }.toIntArray()

            onDrawWithContent {
                drawContent()
                // Rotating only the gradient's local matrix keeps the border shape static while the
                // colours travel along its perimeter, instead of the whole outline visibly spinning.
                val brush = object : ShaderBrush() {
                    override fun createShader(size: Size): Shader = SweepGradient(center.x, center.y, argbColors, null).apply {
                        setLocalMatrix(Matrix().apply { setRotate(angle, center.x, center.y) })
                    }
                }
                translate(left = inset, top = inset) {
                    drawOutline(outline = outline, brush = brush, style = Stroke(width = strokeWidthPx))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingBorderPreview() {
    Row(
        modifier = Modifier.padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size = 96.dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .loadingBorder(colors = listOf(Color.Red, Color.Green, Color.Blue))
        )
        Box(
            modifier = Modifier
                .size(size = 96.dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .loadingBorder()
        )
    }
}
