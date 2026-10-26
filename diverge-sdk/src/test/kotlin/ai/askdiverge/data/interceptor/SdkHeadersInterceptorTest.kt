package ai.askdiverge.data.interceptor

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class SdkHeadersInterceptorTest {

    private val request = Request.Builder()
        .url("https://api.example.com/v1/chat/config")
        .header("Authorization", "Bearer token")
        .build()
    private val sentRequest = slot<Request>()
    private val chain = mockk<Interceptor.Chain> {
        every { request() } returns this@SdkHeadersInterceptorTest.request
        every { proceed(capture(sentRequest)) } returns mockk<Response>(relaxed = true)
    }

    private val interceptor = SdkHeadersInterceptor(
        sdkVersion = "9.8.7",
        clientProfiles = listOf(ClientProfile.PRODUCT_RECOMMENDATION)
    )

    @Test
    fun `when a call goes out expect it to name the Android SDK and its release`() {
        interceptor.intercept(chain)

        assertEquals(
            expected = "android",
            actual = sentRequest.captured.header(SdkHeadersInterceptor.SDK_PLATFORM_HEADER)
        )
        assertEquals(
            expected = "9.8.7",
            actual = sentRequest.captured.header(SdkHeadersInterceptor.SDK_VERSION_HEADER)
        )
    }

    @Test
    fun `when a call goes out expect it to list the profiles it requests`() {
        interceptor.intercept(chain)

        assertEquals(
            expected = "product-recommendation",
            actual = sentRequest.captured.header(SdkHeadersInterceptor.CLIENT_PROFILE_HEADER)
        )
    }

    @Test
    fun `when the headers are added expect the rest of the request left alone`() {
        interceptor.intercept(chain)

        assertEquals(
            expected = request.url,
            actual = sentRequest.captured.url
        )
        assertEquals(
            expected = "Bearer token",
            actual = sentRequest.captured.header("Authorization")
        )
    }

    @Test
    fun `header names and values are the strings the backend reads`() {
        assertEquals(expected = "X-Diverge-SDK-Platform", actual = SdkHeadersInterceptor.SDK_PLATFORM_HEADER)
        assertEquals(expected = "android", actual = SdkHeadersInterceptor.SDK_PLATFORM)
        assertEquals(expected = "X-Diverge-SDK-Version", actual = SdkHeadersInterceptor.SDK_VERSION_HEADER)
        assertEquals(expected = "X-Diverge-Client-Profile", actual = SdkHeadersInterceptor.CLIENT_PROFILE_HEADER)
        assertEquals(expected = "product-recommendation", actual = ClientProfile.PRODUCT_RECOMMENDATION.wireValue)
    }

    @Test
    fun `the profile list sends each value once, in declaration order, comma-separated`() {
        val canonical = ClientProfile.entries.joinToString(separator = ", ") { it.wireValue }

        assertEquals(
            expected = canonical,
            actual = ClientProfile.headerValue(ClientProfile.entries.reversed())
        )
        assertEquals(
            expected = canonical,
            actual = ClientProfile.headerValue(ClientProfile.entries + ClientProfile.entries)
        )
        assertEquals(
            expected = "product-recommendation",
            actual = ClientProfile.headerValue(listOf(ClientProfile.PRODUCT_RECOMMENDATION))
        )
    }
}
