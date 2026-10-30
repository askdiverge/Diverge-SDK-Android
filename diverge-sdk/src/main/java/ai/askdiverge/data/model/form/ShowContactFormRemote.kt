package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire shape of a `show_contact_form` message part. [part_id] and [fields] are required.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showcontactformmarker)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ShowContactFormRemote(
    val part_id: String,
    val fields: List<FormFieldRemote>
)
