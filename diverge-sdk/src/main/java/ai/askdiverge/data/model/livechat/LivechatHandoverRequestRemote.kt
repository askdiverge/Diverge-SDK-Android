package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatHandoverRequestRemote(
    val platform: String,
    val source: String,
    val part_id: String? = null,
    val client_context: LivechatClientContextRemote? = null
)
