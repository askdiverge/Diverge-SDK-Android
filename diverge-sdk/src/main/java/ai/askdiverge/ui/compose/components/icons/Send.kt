package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Send: ImageVector
    get() {
        if (_Send != null) {
            return _Send!!
        }
        _Send = ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.3f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(12f, 20f)
                verticalLineTo(3.837f)
                moveTo(12f, 3.837f)
                lineTo(19f, 10.837f)
                moveTo(12f, 3.837f)
                lineTo(5f, 10.837f)
            }
        }.build()

        return _Send!!
    }

@Suppress("ObjectPropertyName")
private var _Send: ImageVector? = null
