package ai.askdiverge.data.mapper

import retrofit2.Response

/**
 * Folds a Retrofit [Response] into a [Result]. A failed response becomes what [mapError] returns
 * for it, or an exception carrying [errorMessage] when that is `null`.
 */
internal fun <T> Response<T>.mapToResult(
    errorMessage: String,
    mapError: (Response<*>) -> Throwable? = { null }
): Result<T> = if (isSuccessful) {
    body()?.let { Result.success(it) } ?: Result.failure(Exception(errorMessage))
} else {
    Result.failure(mapError(this) ?: Exception(errorMessage))
}
