package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The input a form field asks for, chosen per field by the operator who designs the form. The
 * backend passes it to the client as an input hint. An entry is added when the backend introduces
 * a new input type. Unrecognised wire values decode as [UNKNOWN].
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class FormFieldTypeRemote {
    @Json(name = "text")
    TEXT,

    @Json(name = "email")
    EMAIL,

    @Json(name = "tel")
    TEL,

    /** Deprecated alias of [TEL]. */
    @Json(name = "phone")
    PHONE,

    @Json(name = "textarea")
    TEXTAREA,

    @Json(name = "file")
    FILE,

    @Json(name = "dropdown")
    DROPDOWN,

    UNKNOWN
}
