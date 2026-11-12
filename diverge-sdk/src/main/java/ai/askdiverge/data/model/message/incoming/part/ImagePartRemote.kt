package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire `image` message part.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/imagecontent)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ImagePartRemote(
    val url: String,
    val attachment_id: String? = null,
    val mime_type: String? = null,
    val size_bytes: Int? = null,
    val url_expires_at: String? = null,
    val thumbnail_url: String? = null,
    val caption: String? = null
) : MessagePartRemote
