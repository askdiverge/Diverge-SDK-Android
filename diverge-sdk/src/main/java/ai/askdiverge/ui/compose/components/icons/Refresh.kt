package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val Refresh: ImageVector
    get() {
        if (_Refresh != null) {
            return _Refresh!!
        }
        _Refresh = ImageVector.Builder(
            name = "Refresh",
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
                moveTo(7.481f, 9.347f)
                horizontalLineTo(2.981f)
                moveTo(2.981f, 9.347f)
                verticalLineTo(4.847f)
                moveTo(2.981f, 9.347f)
                lineTo(6.169f, 6.169f)
                curveTo(6.934f, 5.402f, 7.843f, 4.794f, 8.844f, 4.379f)
                curveTo(9.844f, 3.964f, 10.917f, 3.751f, 12f, 3.751f)
                curveTo(13.083f, 3.751f, 14.156f, 3.964f, 15.156f, 4.379f)
                curveTo(16.157f, 4.794f, 17.066f, 5.402f, 17.831f, 6.169f)
                moveTo(16.519f, 14.653f)
                horizontalLineTo(21.019f)
                moveTo(21.019f, 14.653f)
                verticalLineTo(19.153f)
                moveTo(21.019f, 14.653f)
                lineTo(17.831f, 17.831f)
                curveTo(17.066f, 18.598f, 16.157f, 19.206f, 15.156f, 19.621f)
                curveTo(14.156f, 20.035f, 13.083f, 20.249f, 12f, 20.249f)
                curveTo(10.917f, 20.249f, 9.844f, 20.035f, 8.844f, 19.621f)
                curveTo(7.843f, 19.206f, 6.934f, 18.598f, 6.169f, 17.831f)
            }
        }.build()

        return _Refresh!!
    }

@Suppress("ObjectPropertyName")
private var _Refresh: ImageVector? = null
