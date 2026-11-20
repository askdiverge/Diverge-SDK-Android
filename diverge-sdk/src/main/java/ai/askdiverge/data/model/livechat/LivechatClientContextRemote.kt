package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatClientContextRemote(
    val browser: String? = null,
    val browser_language: String? = null,
    val browser_version: String? = null,
    val current_page_url: String? = null,
    val language: String? = null,
    val os: String? = null
)
