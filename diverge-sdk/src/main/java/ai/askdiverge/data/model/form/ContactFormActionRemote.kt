package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/** Wire `contact_form` action: the filled fields keyed by `FormField.key`. */
@Keep
@JsonClass(generateAdapter = true)
internal data class ContactFormActionRemote(
    val fields: Map<String, String>
) : SubmitActionRemote
