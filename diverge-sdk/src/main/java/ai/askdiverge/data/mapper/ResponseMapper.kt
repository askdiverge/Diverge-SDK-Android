package ai.askdiverge.data.mapper

import retrofit2.Response

internal fun <T> Response<T>.mapToResult(errorMessage: String): Result<T> = if (isSuccessful) {
    body()?.let { Result.success(it) } ?: Result.failure(Exception(errorMessage))
} else {
    Result.failure(Exception(errorMessage))
}
