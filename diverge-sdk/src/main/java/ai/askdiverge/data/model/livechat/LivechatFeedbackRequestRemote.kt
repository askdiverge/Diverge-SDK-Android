package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/livechat/feedback` and `POST /api/v1/chat/rate`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatfeedbackrequest)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatFeedbackRequestRemote(
    /** Rating from 1 (worst) to 5 (best). */
    @Json(name = LivechatWire.RATING)
    val rating: Int,
    /**
     * Optional free-text feedback. Null when blank after trim; Moshi omits null properties from
     * the JSON body.
     */
    @Json(name = LivechatWire.FEEDBACK)
    val feedback: String? = null
)
