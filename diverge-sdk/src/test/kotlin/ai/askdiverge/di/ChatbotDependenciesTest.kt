package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.interceptor.SdkHeadersInterceptor
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.sdk.BuildConfig
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Drives the Chatbot API calls through the client the SDK builds. No server is involved: a
 * terminal [RecordingInterceptor] records each request after every SDK interceptor has run and
 * answers it in-process, so these tests assert what the SDK would put on the wire.
 */
internal class ChatbotDependenciesTest {

    private val historyBody = """{"has_more":false,"messages":[],"next_cursor":null}"""
    private val message = SendMessageRemote(
        message = OutgoingMessageRemote(parts = listOf(OutgoingTextPartRemote(text = "hello")))
    )

    @Nested
    @DisplayName("Every authenticated call identifies the SDK and lists the profiles it requests")
    inner class SdkHeaders {

        @Test
        fun `the config fetch carries the token, platform, version and profile`() = runTest {
            // Only the request matters here, so the stubbed body need not be a valid config.
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.PRODUCTION,
                body = "{}",
                contentType = "application/json"
            )

            sut.getConfig()

            recorder.requests.single().assertIdentifiesSdk()
        }

        @Test
        fun `a history page carries them too`() = runTest {
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.PRODUCTION,
                body = historyBody,
                contentType = "application/json"
            )

            sut.getMessageHistory(cursor = null, pageSize = 10)

            recorder.requests.single().assertIdentifiesSdk()
        }

        @Test
        fun `the streamed send carries them without losing its own Accept header`() = runTest {
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.PRODUCTION,
                body = "",
                contentType = "text/event-stream"
            )

            sut.sendMessage(message).toList()

            val request = recorder.requests.single()
            request.assertIdentifiesSdk()
            assertEquals(
                expected = "text/event-stream",
                actual = request.header("Accept")
            )
        }

        @Test
        fun `the version sent is SemVer from VERSION`() {
            assertTrue(
                actual = Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?$""").matches(BuildConfig.SDK_VERSION),
                message = "SDK_VERSION '${BuildConfig.SDK_VERSION}' is not SemVer"
            )
        }
    }

    @Nested
    @DisplayName("API environment")
    inner class Environment {

        @Test
        fun `production calls the host the SDK has always called`() = runTest {
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.PRODUCTION,
                body = historyBody,
                contentType = "application/json"
            )

            sut.getMessageHistory(cursor = null, pageSize = 10)

            val url = recorder.requests.single().url.toString()
            assertTrue(
                actual = url.startsWith("https://api.dialogintelligens.dk/api/v1/chat/messages"),
                message = url
            )
        }

        @Test
        fun `development changes the host and keeps the endpoint path`() = runTest {
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.DEVELOPMENT,
                body = historyBody,
                contentType = "application/json"
            )

            sut.getMessageHistory(cursor = null, pageSize = 10)

            val url = recorder.requests.single().url.toString()
            assertTrue(
                actual = url.startsWith("https://dev.api.dialogintelligens.dk/api/v1/chat/messages"),
                message = url
            )
        }

        @Test
        fun `development streams the send to the development host`() = runTest {
            val (sut, recorder) = makeSUT(
                environment = DivergeEnvironment.DEVELOPMENT,
                body = "",
                contentType = "text/event-stream"
            )

            sut.sendMessage(message).toList()

            assertEquals(
                expected = "https://dev.api.dialogintelligens.dk/api/v1/chat/messages",
                actual = recorder.requests.single().url.toString()
            )
        }

        @Test
        fun `each environment maps to a Diverge host over https`() {
            assertEquals(
                expected = "https://api.dialogintelligens.dk/api/",
                actual = DivergeEnvironment.PRODUCTION.baseUrl
            )
            assertEquals(
                expected = "https://dev.api.dialogintelligens.dk/api/",
                actual = DivergeEnvironment.DEVELOPMENT.baseUrl
            )
        }
    }

    private fun Request.assertIdentifiesSdk() {
        assertEquals(expected = "Bearer token-1", actual = header("Authorization"))
        assertEquals(expected = "android", actual = header(SdkHeadersInterceptor.SDK_PLATFORM_HEADER))
        assertEquals(expected = "9.8.7", actual = header(SdkHeadersInterceptor.SDK_VERSION_HEADER))
        assertEquals(
            expected = "product-recommendation",
            actual = header(SdkHeadersInterceptor.CLIENT_PROFILE_HEADER)
        )
    }

    private fun makeSUT(
        environment: DivergeEnvironment,
        body: String,
        contentType: String
    ): Pair<ChatbotRemoteDataSource, RecordingInterceptor> {
        val recorder = RecordingInterceptor(
            body = body,
            contentType = contentType
        )
        val okHttp = buildOkHttpClient(
            tokenLocalDataSource = TokenLocalDataSourceImpl().apply { setToken("token-1") },
            sdkVersion = "9.8.7",
            clientProfiles = listOf(ClientProfile.PRODUCT_RECOMMENDATION)
        ).newBuilder()
            .addInterceptor(recorder)
            .build()

        return createRemoteDataSource(
            environment = environment,
            okHttp = okHttp
        ) to recorder
    }

    /**
     * Terminal interceptor: records each request and answers it with [body] instead of calling
     * the network. Added last, it sees the request exactly as the SDK's interceptors left it.
     * Requests are recorded from OkHttp's dispatcher thread for streamed calls, hence the
     * thread-safe list.
     */
    private class RecordingInterceptor(
        private val body: String,
        private val contentType: String
    ) : Interceptor {

        val requests = CopyOnWriteArrayList<Request>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody(contentType.toMediaType()))
                .build()
        }
    }
}
