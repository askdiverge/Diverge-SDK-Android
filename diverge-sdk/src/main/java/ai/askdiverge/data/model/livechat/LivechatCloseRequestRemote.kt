package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/livechat/close`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatcloserequest)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatCloseRequestRemote(
    val reason: String? = null
) {
    companion object {
        /** `reason` when the visitor ends the session from the header control. */
        const val ENDED_BY_VISITOR_REASON = "Switched back to AI chatbot mode"

        /** `reason` when the visitor resets the chat while a session is open. */
        const val RESET_REASON = "Chat reset"
    }
}
