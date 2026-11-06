package ai.askdiverge.data.model.error

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * The body the Chatbot API returns with a non-2xx status — `{ error: { message, params? } }`.
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ApiErrorEnvelopeRemote(
    val error: ApiErrorBodyRemote
)
