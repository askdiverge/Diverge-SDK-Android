package ai.askdiverge.data.model.config.display.subtitle

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class SubtitleRemote(
    val text: String? = null,
    val link: SubtitleLinkValueRemote? = null
)
