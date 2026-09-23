package ai.askdiverge.data.model.config.theme.header

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class HeaderAlignmentRemote {
    @Json(name = "left")
    LEFT,

    @Json(name = "center")
    CENTER,

    @Json(name = "right")
    RIGHT,

    UNKNOWN
}
