package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire shape of a dynamic form field carried inline on a form marker or form definition.
 *
 * A missing or unrecognised [type] maps to text; the deprecated `phone` alias maps to tel. Contract
 * defaults for [required], [options] and [visible_when] are applied in the domain mapper.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/formfield)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class FormFieldRemote(
    val key: String,
    val label: String,
    val placeholder: String? = null,
    val type: FormFieldTypeRemote? = null,
    val required: Boolean? = null,
    val options: List<String>? = null,
    val visible_when: List<FormFieldConditionRemote>? = null
)
