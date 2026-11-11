package ai.askdiverge.data.model.message.incoming

import androidx.annotation.Keep
import ai.askdiverge.data.model.livechat.LivechatAgentRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class MessageRemote(
    val message_id: String,
    val role: MessageRoleRemote,
    /** Livechat agent identity for `role: agent` messages. Absent for other roles. */
    val agent: LivechatAgentRemote? = null,
    val parts: List<MessagePartRemote>
)
