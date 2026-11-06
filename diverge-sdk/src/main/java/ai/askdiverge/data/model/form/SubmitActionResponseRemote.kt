package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire success body for `POST /api/v1/chat/actions`.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-actions/POST/api/v1/chat/actions)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class SubmitActionResponseRemote(
    val submission_id: String,
    val confirmation_text: String? = null
)
