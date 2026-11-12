package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire row from `GET /session` / `PATCH …/values` → `values[]`.
 *
 * The waiting form reads [key] and [value] only; other wire fields are ignored.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatsessionvalue)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatSessionValueRemote(
    val key: String,
    val value: String
)
