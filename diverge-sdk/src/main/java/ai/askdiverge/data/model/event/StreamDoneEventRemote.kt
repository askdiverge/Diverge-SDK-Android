package ai.askdiverge.data.model.event

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.MessageRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class StreamDoneEventRemote(val message: MessageRemote) : StreamEventRemote
