package ai.askdiverge.data.model.message.incoming

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class MessagePageRemote(
    val has_more: Boolean,
    val messages: List<MessageRemote>,
    val next_cursor: String?
)
