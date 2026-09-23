package ai.askdiverge.data.model.config.theme.messages

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeMessagesRemote(
    val assistant: AssistantMessageStyleRemote,
    val user: UserMessageStyleRemote
)
