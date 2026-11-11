package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire `availability_reason` on [LivechatConfigRemote]: why the backend set
 * `availability_status`. The backend decides it from the chatbot's livechat settings, working
 * hours and manual overrides; the client only reads it to choose its "agents unavailable" copy.
 * An entry is added when the Chatbot API TypeSpec adds a reason; until then a new value decodes
 * to [UNKNOWN].
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
