package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.OutgoingMessageContextRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessagePartRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatVisitorMessagePayloadRemote(
    val parts: List<OutgoingMessagePartRemote>
)

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatVisitorMessageRequestRemote(
    val message: LivechatVisitorMessagePayloadRemote,
    val context: OutgoingMessageContextRemote? = null,
    val client_operation_id: String
)
