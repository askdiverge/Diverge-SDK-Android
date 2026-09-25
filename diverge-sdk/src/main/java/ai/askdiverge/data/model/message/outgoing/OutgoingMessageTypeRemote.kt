package ai.askdiverge.data.model.message.outgoing

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class OutgoingMessageTypeRemote(val type: String) {
    @Json(name = "text")
    TEXT("text")
}
