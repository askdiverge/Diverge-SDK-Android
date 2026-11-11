package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatSyncRemote(
    val state: LivechatStateRemote,
    val messages: List<LivechatMessageRemote> = emptyList(),
    val has_more: Boolean = false,
    val sync_cursor: String? = null
)
