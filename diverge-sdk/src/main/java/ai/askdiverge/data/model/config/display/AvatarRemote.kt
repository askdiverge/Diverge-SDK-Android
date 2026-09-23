package ai.askdiverge.data.model.config.display

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class AvatarRemote(val url: String?)
