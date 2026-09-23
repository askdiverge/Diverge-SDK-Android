package ai.askdiverge.data.model.config.display

import androidx.annotation.Keep
import ai.askdiverge.data.model.config.display.subtitle.SubtitleRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class DisplayRemote(
    val name: String,
    val avatar: AvatarRemote,
    val welcome_message: String?,
    val subtitle: SubtitleRemote?,
    val privacy_policy_url: String
)
