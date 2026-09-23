package ai.askdiverge.data.model.event.delta

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class DeltaBlockTypeRemote {
    @Json(name = "paragraph")
    PARAGRAPH,

    @Json(name = "bullet_list")
    BULLET_LIST,

    UNKNOWN
}
