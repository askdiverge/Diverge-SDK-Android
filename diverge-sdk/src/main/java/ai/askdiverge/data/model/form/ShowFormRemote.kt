package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire shape of a `show_form` message part. Only [part_id] and [form_id] are guaranteed: the
 * backend sends a thin marker when it cannot resolve the form. An empty [fields] list is hydrated
 * from `GET …/forms/{form_id}`. [name] is trimmed when mapped to the domain.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showformmarker)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ShowFormRemote(
    val part_id: String,
    val form_id: String,
    val name: String? = null,
    val confirmation_text: String? = null,
    val fields: List<FormFieldRemote> = emptyList(),
    val min_filled_fields: Int = 0
)
