package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val ArrowUpRight: ImageVector
    get() {
        if (_ArrowUpRight != null) {
            return _ArrowUpRight!!
        }
        _ArrowUpRight = ImageVector.Builder(
            name = "ArrowUpRight",
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
                moveTo(6f, 18f)
                lineTo(18f, 6f)
                moveTo(18f, 6f)
                horizontalLineTo(8.25f)
                moveTo(18f, 6f)
                verticalLineTo(15.75f)
            }
        }.build()

        return _ArrowUpRight!!
    }

@Suppress("ObjectPropertyName")
private var _ArrowUpRight: ImageVector? = null
