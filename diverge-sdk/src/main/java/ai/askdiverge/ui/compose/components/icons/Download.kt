package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Download: ImageVector
    get() {
        if (_Download != null) {
            return _Download!!
        }
        _Download = ImageVector.Builder(
            name = "Download",
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
                moveTo(12f, 3.75f)
                verticalLineTo(14.25f)
                moveTo(12f, 14.25f)
                lineTo(16.5f, 9.75f)
                moveTo(12f, 14.25f)
                lineTo(7.5f, 9.75f)
                moveTo(4.5f, 17.25f)
                verticalLineTo(19.5f)
                horizontalLineTo(19.5f)
                verticalLineTo(17.25f)
            }
        }.build()

        return _Download!!
    }

@Suppress("ObjectPropertyName")
private var _Download: ImageVector? = null
