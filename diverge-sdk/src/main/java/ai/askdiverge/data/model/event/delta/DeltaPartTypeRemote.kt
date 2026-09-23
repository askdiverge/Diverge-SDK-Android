package ai.askdiverge.data.model.event.delta

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class DeltaPartTypeRemote {
    @Json(name = "rich_text")
    RICH_TEXT,

    @Json(name = "products")
    PRODUCTS,

    @Json(name = "table")
    TABLE,

    UNKNOWN
}
