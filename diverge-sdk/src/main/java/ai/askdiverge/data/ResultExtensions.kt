package ai.askdiverge.data

import ai.askdiverge.data.mapper.mapToResult
import ai.askdiverge.domain.logging.ChatbotLogger
import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** [runCatching] that lets a cancelled coroutine's [CancellationException] keep travelling. */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = runCatching(block)
    .onFailure { error -> if (error is CancellationException) throw error }

/**
 * Folds a Retrofit call into a [Result]: it throws on transport failures instead of returning a [Response].
 * A failed response becomes what [mapError] returns for it, or an exception carrying [errorMessage].
 */
internal suspend fun <T> safeApiCall(
    errorMessage: String,
    mapError: (Response<*>) -> Throwable? = { null },
    call: suspend () -> Response<T>
): Result<T> = runCatchingCancellable { call() }
    .mapCatching { response -> response.mapToResult(errorMessage, mapError).getOrThrow() }
    .onFailure { error -> ChatbotLogger.logError(error) }
