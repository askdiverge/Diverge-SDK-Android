package ai.askdiverge.data.model.config.theme.input

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeSendButtonRemote(val icon_color: String?)
