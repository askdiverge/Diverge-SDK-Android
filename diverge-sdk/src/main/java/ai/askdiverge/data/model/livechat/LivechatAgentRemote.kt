package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire agent identity on a `role: agent` message.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatagentidentity)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatAgentRemote(
    val agent_id: String,
    val display_name: String,
    val avatar_url: String? = null
)
