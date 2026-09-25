package ai.askdiverge.data.interceptor

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import okhttp3.Interceptor
import okhttp3.Response

internal class BearerTokenInterceptor(private val tokenLocalDataSource: TokenLocalDataSource) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenLocalDataSource.getToken() ?: throw ChatbotException.NoTokenAvailable()
        return chain.proceed(
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        )
    }
}
