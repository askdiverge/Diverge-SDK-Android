package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatSessionResponseRemote(
    val status: LivechatStateStatusRemote = LivechatStateStatusRemote.UNKNOWN
)
