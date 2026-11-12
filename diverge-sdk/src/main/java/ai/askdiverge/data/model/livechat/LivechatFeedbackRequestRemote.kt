package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/livechat/feedback`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatfeedbackrequest)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatFeedbackRequestRemote(
    /** Rating from 1 (worst) to 5 (best). Wire name [LivechatWire.RATING]. */
    val rating: Int,
    /**
     * Optional free-text feedback. Wire name [LivechatWire.FEEDBACK]. Null when blank after trim;
     * Moshi omits null properties from the JSON body.
     */
    val feedback: String? = null
)
