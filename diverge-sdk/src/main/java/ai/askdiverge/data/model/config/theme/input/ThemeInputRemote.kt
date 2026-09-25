package ai.askdiverge.data.model.config.theme.input

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeInputRemote(
    val text_color: String?,
    val placeholder_color: String?,
    val background_color: String?,
    val border_color: String?,
    val send_button: ThemeSendButtonRemote
)
