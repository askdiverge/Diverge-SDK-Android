package ai.askdiverge.data.interceptor

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

internal class BearerTokenInterceptorTest {

    private val request = Request.Builder().url("https://api.example.com/v1/chat/config").build()
    private val tokenLocalDataSource = mockk<TokenLocalDataSource>()
    private val sentRequest = slot<Request>()
    private val chain = mockk<Interceptor.Chain> {
        every { request() } returns this@BearerTokenInterceptorTest.request
        every { proceed(capture(sentRequest)) } returns mockk<Response>(relaxed = true)
    }

    private val interceptor = BearerTokenInterceptor(tokenLocalDataSource)

    @Test
    fun `when there is a session token expect the call authenticated with it`() {
        every { tokenLocalDataSource.getToken() } returns "token"

        interceptor.intercept(chain)

        assertEquals(
            expected = "Bearer token",
            actual = sentRequest.captured.header("Authorization")
        )
    }

    @Test
    fun `when the token is added expect the rest of the request left alone`() {
        every { tokenLocalDataSource.getToken() } returns "token"

        interceptor.intercept(chain)

        assertEquals(
            expected = request.url,
            actual = sentRequest.captured.url
        )
    }

    @Test
    fun `when there is no session token expect the call to fail instead of going out`() {
        every { tokenLocalDataSource.getToken() } returns null

        assertFailsWith<ChatbotException.NoTokenAvailable> { interceptor.intercept(chain) }

        verify(exactly = 0) { chain.proceed(any()) }
    }
}
