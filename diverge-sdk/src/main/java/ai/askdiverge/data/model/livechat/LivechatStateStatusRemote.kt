package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class LivechatStateStatusRemote {
    @Json(name = "inactive")
    INACTIVE,

    @Json(name = "waiting")
    WAITING,

    @Json(name = "active")
    ACTIVE,

    @Json(name = "closed")
    CLOSED,

    UNKNOWN
}
