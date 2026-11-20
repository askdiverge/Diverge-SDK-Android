package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire body for `POST /api/v1/chat/actions`.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-actions/POST/api/v1/chat/actions)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class SubmitActionRequestRemote(
    val part_id: String,
    val current_page_url: String?,
    val action: SubmitActionRemote
)
