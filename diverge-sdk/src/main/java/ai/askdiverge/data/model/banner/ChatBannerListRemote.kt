package ai.askdiverge.data.model.banner

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire envelope for `GET /api/v1/chat/banners`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatbannerlist)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatBannerListRemote(
    val banners: List<ChatBannerRemote> = emptyList()
)
