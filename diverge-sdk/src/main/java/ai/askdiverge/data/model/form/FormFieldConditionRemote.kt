package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire shape of a visibility condition on a [FormFieldRemote].
 *
 * [API ref](https://docs.askdiverge.ai/api#model/formfieldcondition)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class FormFieldConditionRemote(
    val field: String,
    val operator: FormFieldConditionOperatorRemote? = null,
    val value: String
)
