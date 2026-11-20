package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class LivechatClosedByRemote {
    @Json(name = "customer")
    CUSTOMER,

    @Json(name = "agent")
    AGENT,

    UNKNOWN
}
