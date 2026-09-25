package ai.askdiverge.data.model.config.theme.header

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeLogoRemote(val url: String?)
