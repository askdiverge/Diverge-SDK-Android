package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class LivechatFeedbackStatusRemote {
    @Json(name = "not_available")
    NOT_AVAILABLE,

    @Json(name = "pending")
    PENDING,

    @Json(name = "submitted")
    SUBMITTED,

    UNKNOWN
}
