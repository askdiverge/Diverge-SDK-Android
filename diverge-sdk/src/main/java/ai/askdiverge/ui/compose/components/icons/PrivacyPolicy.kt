package ai.askdiverge.ui.compose.components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val PrivacyPolicy: ImageVector
    get() {
        if (_PrivacyPolicy != null) {
            return _PrivacyPolicy!!
        }
        _PrivacyPolicy = ImageVector.Builder(
            name = "PrivacyPolicy",
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
                moveTo(3.75f, 4.5f)
                verticalLineTo(10.753f)
                curveTo(3.75f, 18.628f, 10.438f, 21.25f, 12f, 21.675f)
                curveTo(13.563f, 21.242f, 20.25f, 18.628f, 20.25f, 10.753f)
                verticalLineTo(4.5f)
                lineTo(3.75f, 4.5f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.3f,
                strokeLineCap = StrokeCap.Square
            ) {
                moveTo(10.143f, 10.727f)
                verticalLineTo(8.7f)
                curveTo(10.143f, 8.249f, 10.322f, 7.817f, 10.641f, 7.498f)
                curveTo(10.959f, 7.179f, 11.392f, 7f, 11.843f, 7f)
                curveTo(12.293f, 7f, 12.726f, 7.179f, 13.045f, 7.498f)
                curveTo(13.363f, 7.817f, 13.543f, 8.249f, 13.543f, 8.7f)
                verticalLineTo(10.727f)
                moveTo(8f, 16.117f)
                verticalLineTo(10.727f)
                horizontalLineTo(15.671f)
                verticalLineTo(16.117f)
                horizontalLineTo(8f)
                close()
            }
        }.build()

        return _PrivacyPolicy!!
    }

@Suppress("ObjectPropertyName")
private var _PrivacyPolicy: ImageVector? = null
