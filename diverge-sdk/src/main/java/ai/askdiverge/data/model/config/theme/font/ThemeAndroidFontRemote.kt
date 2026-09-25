package ai.askdiverge.data.model.config.theme.font

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ThemeAndroidFontRemote(val asset_url: String)
