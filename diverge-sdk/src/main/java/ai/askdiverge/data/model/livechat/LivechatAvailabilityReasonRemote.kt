package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire `availability_reason` on [LivechatConfigRemote].
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatavailabilityreason)
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class LivechatAvailabilityReasonRemote {
    @Json(name = "disabled")
    DISABLED,

    @Json(name = "manual_live")
    MANUAL_LIVE,

    @Json(name = "manual_offline")
    MANUAL_OFFLINE,

    @Json(name = "always_on")
    ALWAYS_ON,

    @Json(name = "within_working_hours")
    WITHIN_WORKING_HOURS,

    @Json(name = "outside_working_hours")
    OUTSIDE_WORKING_HOURS,

    UNKNOWN
}
