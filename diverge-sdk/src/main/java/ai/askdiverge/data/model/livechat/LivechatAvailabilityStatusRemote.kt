package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire `availability_status` on [LivechatConfigRemote]: whether a visitor can hand over to a
 * live agent now. The backend decides it from the chatbot's livechat settings, working hours and
 * manual overrides; the client offers the handover control only while it is [LIVE]. An entry is
 * added when the Chatbot API TypeSpec adds a status; until then a new value decodes to
 * [UNKNOWN].
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
