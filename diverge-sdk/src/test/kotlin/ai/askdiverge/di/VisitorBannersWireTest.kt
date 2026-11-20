package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.model.banner.ChatBannerRemote
import ai.askdiverge.data.model.banner.VisitorBannersWire
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

internal class VisitorBannersWireTest {

    @Nested
    @DisplayName("GET /chat/banners")
    inner class BannerCalls {

        @Test
        fun `banners use the wire path, url query and bearer`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"banners":[{"id":"1","message":"Sale","dismissible":true}]}"""
                )
            )

            val result = sut.listBanners(url = "/products/boots")

            assertTrue(result.isSuccess)
            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/${VisitorBannersWire.PATH}"))
            assertEquals("/products/boots", request.url.queryParameter(VisitorBannersWire.URL))
            assertEquals("Bearer token-1", request.header("Authorization"))
            assertEquals(VisitorBannersWire.PATH, "v1/chat/banners")
            assertEquals(VisitorBannersWire.URL, "url")
        }

        @Test
        fun `a null page omits the url query`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(status = 200, body = """{"banners":[]}""")
            )

            val result = sut.listBanners(url = null)

            assertTrue(result.isSuccess)
            assertNull(recorder.requests.single().url.queryParameter(VisitorBannersWire.URL))
        }

        @Test
        fun `a banner row that fails to decode is skipped and the rest are kept`() = runTest {
            val (sut, _) = makeSUT(
                ScriptedResponse(
                    status = 200,
                    body = """{"banners":[{"id":"1"},{"id":"2","message":"ok"}]}"""
                )
            )

            val result = sut.listBanners(url = null)

            assertEquals(
                listOf(ChatBannerRemote(id = "2", message = "ok")),
                result.getOrThrow().banners
            )
        }

        @Test
        fun `a 401 on banners surfaces as session expired`() = runTest {
            val (sut, _) = makeSUT(ScriptedResponse(status = 401, body = ""))

            val result = sut.listBanners(url = "/")

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
        }
    }

    private fun makeSUT(vararg responses: ScriptedResponse): Pair<ChatbotRemoteDataSource, ScriptedInterceptor> {
        val recorder = ScriptedInterceptor(responses.toList())
        val okHttp = buildOkHttpClient(
            tokenLocalDataSource = TokenLocalDataSourceImpl().apply { setToken("token-1") },
            sdkVersion = "9.8.7",
            clientProfiles = listOf(ClientProfile.CUSTOMER_SERVICE)
        ).newBuilder()
            .addInterceptor(recorder)
            .build()

        return createRemoteDataSource(
            environment = DivergeEnvironment.DEVELOPMENT,
            okHttp = okHttp
        ) to recorder
    }

    private data class ScriptedResponse(
        val status: Int,
        val body: String
    )

    private class ScriptedInterceptor(private val responses: List<ScriptedResponse>) : Interceptor {

        val requests = CopyOnWriteArrayList<Request>()
        private val index = AtomicInteger(0)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            val scripted = responses.getOrElse(index.getAndIncrement()) { responses.last() }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(scripted.status)
                .message(if (scripted.status in 200..299) "OK" else "Error")
                .body(scripted.body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
