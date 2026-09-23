package ai.askdiverge.data.model.config.theme.header

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeHeaderButtonRemote(
    val background_color: String?,
    val icon_color: String?
)
