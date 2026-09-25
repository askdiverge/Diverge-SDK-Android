package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class MessagePartTypeRemote(val type: String) {
    @Json(name = "rich_text")
    RICH_TEXT(type = "rich_text"),

    @Json(name = "image")
    IMAGE(type = "image"),

    @Json(name = "products")
    PRODUCTS(type = "products"),

    @Json(name = "table")
    TABLE(type = "table")
}
