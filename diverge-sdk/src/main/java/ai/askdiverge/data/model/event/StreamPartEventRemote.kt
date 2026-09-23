package ai.askdiverge.data.model.event

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class StreamPartEventRemote(val part: MessagePartRemote?) : StreamEventRemote
