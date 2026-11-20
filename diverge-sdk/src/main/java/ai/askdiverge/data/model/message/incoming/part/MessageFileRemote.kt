package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire `file` message part.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/filecontent)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class MessageFileRemote(
    val filename: String,
    val url: String,
    val attachment_id: String? = null,
    val mime_type: String? = null,
    val size_bytes: Int? = null,
    val url_expires_at: String? = null
) : MessagePartRemote
