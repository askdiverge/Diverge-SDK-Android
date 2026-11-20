package ai.askdiverge.data.model.banner

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire row for `GET /api/v1/chat/banners`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatbanner)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatBannerRemote(
    val id: String,
    val message: String,
    val background_color: String? = null,
    val text_color: String? = null,
    val cta_url: String? = null,
    val cta_label: String? = null,
    /** Wire literal `"link"` / `"button"`; unknown values stay as the raw string for the mapper. */
    val cta_style: String? = null,
    val dismissible: Boolean = false
)
