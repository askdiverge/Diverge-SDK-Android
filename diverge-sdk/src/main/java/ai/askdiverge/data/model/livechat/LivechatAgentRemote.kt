package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire agent identity on a `role: agent` message. Every field is nullable so a history page
 * still decodes when the backend omits or nulls one.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatagentidentity)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatAgentRemote(
    val agent_id: String? = null,
    val display_name: String? = null,
    val avatar_url: String? = null
)
