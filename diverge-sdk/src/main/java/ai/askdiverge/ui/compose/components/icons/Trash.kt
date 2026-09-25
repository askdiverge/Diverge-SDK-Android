package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Trash: ImageVector
    get() {
        if (_Trash != null) {
            return _Trash!!
        }
        _Trash = ImageVector.Builder(
            name = "Trash",
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
                moveTo(20.25f, 5.25f)
                horizontalLineTo(3.75f)
                moveTo(9.75f, 9.75f)
                verticalLineTo(15.75f)
                moveTo(14.25f, 9.75f)
                verticalLineTo(15.75f)
                moveTo(18.75f, 5.25f)
                verticalLineTo(20.25f)
                horizontalLineTo(5.25f)
                verticalLineTo(5.25f)
                moveTo(15.75f, 5.25f)
                verticalLineTo(2.25f)
                horizontalLineTo(8.25f)
                verticalLineTo(5.25f)
            }
        }.build()

        return _Trash!!
    }

@Suppress("ObjectPropertyName")
private var _Trash: ImageVector? = null
