package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/livechat/typing`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechattypingrequest)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatTypingRequestRemote(
    /** Whether the visitor is currently typing. Wire name [LivechatWire.IS_TYPING]. */
    val is_typing: Boolean
)
