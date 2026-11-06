package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.error.ApiErrorEnvelopeRemote
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import com.squareup.moshi.JsonAdapter
import retrofit2.Response

/**
 * The typed failure for a failed form call, or `null` when the status has none and the caller's
 * generic error applies. Field-level errors are read from the body's error envelope.
 */
internal fun Response<*>.mapFormError(envelopeAdapter: JsonAdapter<ApiErrorEnvelopeRemote>): Throwable? =
    when (code()) {
        401 -> ChatbotException.SessionExpired()
        404 -> FormRequestException.NotFound()
        409 -> FormRequestException.Conflict()
        else -> {
            val envelope = runCatching { errorBody()?.string()?.let(envelopeAdapter::fromJson) }.getOrNull()
            val params = envelope?.error?.params.orEmpty().map { param ->
                FormRequestException.Validation.Param(
                    field = param.field,
                    message = param.message
                )
            }
            if (code() == 422 || params.isNotEmpty()) FormRequestException.Validation(params) else null
        }
    }
