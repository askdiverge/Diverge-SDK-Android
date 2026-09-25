package ai.askdiverge.data.model.message.outgoing

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class SendMessageRemote(val message: OutgoingMessageRemote)
