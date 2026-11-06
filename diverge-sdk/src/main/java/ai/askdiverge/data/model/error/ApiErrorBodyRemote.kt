package ai.askdiverge.data.model.error

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/** The `error` object of [ApiErrorEnvelopeRemote]; only its field-level `params` are read. */
@Keep
@JsonClass(generateAdapter = true)
internal data class ApiErrorBodyRemote(
    val params: List<ValidationErrorRemote>? = null
)
