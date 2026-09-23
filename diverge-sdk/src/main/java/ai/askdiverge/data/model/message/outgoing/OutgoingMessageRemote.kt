package ai.askdiverge.data.model.message.outgoing

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.OutgoingMessageContextRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class OutgoingMessageRemote(
    val parts: List<OutgoingMessagePartRemote>,
    val context: OutgoingMessageContextRemote? = null
)
