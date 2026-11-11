package ai.askdiverge.data.model.livechat

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire `livechat` block from `GET /api/v1/chat/config`. Absent or partial fields map to
 * [ai.askdiverge.domain.model.livechat.LivechatConfig.serverDefaults].
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatconfig)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class LivechatConfigRemote(
    val enabled: Boolean? = null,
    val configured: Boolean? = null,
    val availability_status: LivechatAvailabilityStatusRemote? = null,
    val availability_reason: LivechatAvailabilityReasonRemote? = null,
    val show_livechat_logo: Boolean? = null,
    val attachments_enabled: Boolean? = null,
    val max_attachment_size_bytes: Int? = null
)
