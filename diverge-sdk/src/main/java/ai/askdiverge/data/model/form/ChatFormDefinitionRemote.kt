package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire body of `GET /api/v1/chat/forms/{form_id}`, used to hydrate a `show_form` whose fields
 * arrive empty.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatformdefinition)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatFormDefinitionRemote(
    val form_id: String,
    val name: String?,
    val confirmation_text: String? = null,
    val fields: List<FormFieldRemote>,
    val min_filled_fields: Int
)
