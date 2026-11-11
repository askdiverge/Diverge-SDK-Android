package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire `availability_status` on [LivechatConfigRemote].
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatavailabilitystatus)
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class LivechatAvailabilityStatusRemote {
    @Json(name = "live")
    LIVE,

    @Json(name = "offline")
    OFFLINE,

    UNKNOWN
}
