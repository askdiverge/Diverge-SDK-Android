package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Error: ImageVector
    get() {
        if (_Error != null) {
            return _Error!!
        }
        _Error = ImageVector.Builder(
            name = "Error",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Outer circle
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 21f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 3f)
                // Exclamation stem
                moveTo(12f, 7.5f)
                verticalLineTo(13f)
                // Exclamation dot
                moveTo(12f, 16.5f)
                verticalLineTo(16.51f)
            }
        }.build()

        return _Error!!
    }

@Suppress("ObjectPropertyName")
private var _Error: ImageVector? = null
