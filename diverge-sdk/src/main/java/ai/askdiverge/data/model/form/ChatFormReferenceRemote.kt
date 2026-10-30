package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire row from `GET /api/v1/chat/config` → `forms[]`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatformreference)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatFormReferenceRemote(
    val form_id: String,
    val name: String? = null,
    val trigger: ChatFormTriggerRemote
)
