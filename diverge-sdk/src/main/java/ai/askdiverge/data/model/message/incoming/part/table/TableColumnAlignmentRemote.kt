package ai.askdiverge.data.model.message.incoming.part.table

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class TableColumnAlignmentRemote {
    @Json(name = "left")
    LEFT,

    @Json(name = "center")
    CENTER,

    @Json(name = "right")
    RIGHT,

    UNKNOWN
}
