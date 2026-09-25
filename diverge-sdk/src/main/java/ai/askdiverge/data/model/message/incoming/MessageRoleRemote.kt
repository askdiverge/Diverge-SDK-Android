package ai.askdiverge.data.model.message.incoming

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class MessageRoleRemote {
    @Json(name = "user")
    USER,

    @Json(name = "assistant")
    ASSISTANT,

    @Json(name = "agent")
    AGENT,

    UNKNOWN
}
