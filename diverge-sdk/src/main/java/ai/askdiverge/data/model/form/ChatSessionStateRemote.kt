package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire body for `GET /api/v1/chat/session` and `PATCH /forms/{form_id}/values`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatsessionstate)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatSessionStateRemote(
    val values: List<ChatSessionValueRemote>
)
