package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatStateRemote(
    val status: LivechatStateStatusRemote = LivechatStateStatusRemote.UNKNOWN,
    val active_agent: LivechatAgentRemote? = null,
    val is_agent_typing: Boolean = false,
    val agent_joined_at: String? = null,
    val closed_by: LivechatClosedByRemote? = null,
    val feedback: LivechatFeedbackRemote = LivechatFeedbackRemote(),
    val state_version: Int? = null
)
