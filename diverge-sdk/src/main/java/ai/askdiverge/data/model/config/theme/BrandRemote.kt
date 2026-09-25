package ai.askdiverge.data.model.config.theme

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class BrandRemote(val primary_color: String)
