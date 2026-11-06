package ai.askdiverge.data.model.error

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/** One entry of `error.params`: the invalid value's path in the request body and what is wrong. */
@Keep
@JsonClass(generateAdapter = true)
internal data class ValidationErrorRemote(
    val field: String,
    val message: String
)
