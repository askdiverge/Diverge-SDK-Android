package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatMessageRemote(
    val message_id: String,
    val role: MessageRoleRemote,
    val agent: LivechatAgentRemote? = null,
    val parts: List<MessagePartRemote> = emptyList(),
    val created_at: String,
    val sequence_number: Long
)
