package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val ShieldCheck: ImageVector
    get() {
        if (_ShieldCheck != null) {
            return _ShieldCheck!!
        }
        _ShieldCheck = ImageVector.Builder(
            name = "ShieldCheck",
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
                moveTo(16.125f, 9.75f)
                lineTo(10.622f, 15f)
                lineTo(7.875f, 12.375f)
                moveTo(3.75f, 10.753f)
                verticalLineTo(4.5f)
                lineTo(20.25f, 4.5f)
                verticalLineTo(10.753f)
                curveTo(20.25f, 18.628f, 13.563f, 21.242f, 12f, 21.675f)
                curveTo(10.438f, 21.25f, 3.75f, 18.628f, 3.75f, 10.753f)
                close()
            }
        }.build()

        return _ShieldCheck!!
    }

@Suppress("ObjectPropertyName")
private var _ShieldCheck: ImageVector? = null
