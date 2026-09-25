package ai.askdiverge.data.model.message.incoming

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class MessageRemote(
    val message_id: String,
    val role: MessageRoleRemote,
    val parts: List<MessagePartRemote>
)
