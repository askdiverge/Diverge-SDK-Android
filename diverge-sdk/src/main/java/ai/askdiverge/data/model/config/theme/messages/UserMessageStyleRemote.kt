package ai.askdiverge.data.model.config.theme.messages

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class UserMessageStyleRemote(
    val background_color: String,
    val text_color: String,
    val border_color: String?
)
