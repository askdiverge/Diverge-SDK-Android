package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * How a condition compares a field's answer with its value, chosen by the operator who designs
 * the form. The client evaluates it to decide whether a field is shown. An entry is added when the
 * backend supports a new comparison. Unrecognised wire values decode as [UNKNOWN].
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class FormFieldConditionOperatorRemote {
    @Json(name = "equals")
    EQUALS,

    UNKNOWN
}
