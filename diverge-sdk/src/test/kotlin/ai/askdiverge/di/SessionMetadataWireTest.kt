package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.interceptor.SdkHeadersInterceptor
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.mapper.mapToRemote
import ai.askdiverge.data.model.session.SessionMetadataWire
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.SessionMetadataRequestException
import ai.askdiverge.domain.model.metadata.JsonValue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

internal class SessionMetadataWireTest {

    @Nested
    @DisplayName("PATCH /chat/session")
    inner class Patch {

        @Test
        fun `metadata goes to PATCH session with the bearer and the three SDK headers, keys verbatim`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = "{}"))

            val result = sut.patchSession(
                SessionMetadataPatch.Metadata(
                    mapOf(
                        "cseyeStoreSlug" to JsonValue.Str("boozt-se"),
                        "loggedIn" to JsonValue.Bool(false),
                        "cart" to JsonValue.Object(
                            mapOf(
                                "itemCount" to JsonValue.Integer(2),
                                "total" to JsonValue.Decimal(49.5),
                                "tags" to JsonValue.Array(listOf(JsonValue.Str("sale")))
                            )
                        )
                    )
                ).mapToRemote()
            )

            assertTrue(result.isSuccess)
            val request = recorder.requests.single()
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/chat/session", request.url.encodedPath)
            assertEquals("Bearer token-1", request.header("Authorization"))
            assertEquals("android", request.header(SdkHeadersInterceptor.SDK_PLATFORM_HEADER))
            assertEquals("9.8.7", request.header(SdkHeadersInterceptor.SDK_VERSION_HEADER))
            assertEquals("customer-service", request.header(SdkHeadersInterceptor.CLIENT_PROFILE_HEADER))
            assertEquals(
                """{"metadata":{"cseyeStoreSlug":"boozt-se","loggedIn":false,""" +
                    """"cart":{"itemCount":2,"total":49.5,"tags":["sale"]}}}""",
                request.bodyText()
            )
        }

        @Test
        fun `a null inside metadata is sent as null, and the unset top-level field is left out`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = "{}"))

            sut.patchSession(SessionMetadataPatch.Metadata(mapOf("nickname" to JsonValue.Null)).mapToRemote())

            assertEquals("""{"metadata":{"nickname":null}}""", recorder.requests.single().bodyText())
        }

        @Test
        fun `a signed token goes as signed_metadata, alone`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = "{}"))

            sut.patchSession(SessionMetadataPatch.Signed("header.claims.signature").mapToRemote())

            assertEquals(
                """{"signed_metadata":"header.claims.signature"}""",
                recorder.requests.single().bodyText()
            )
        }

        @Test
        fun `the path and both field names are pinned literals`() {
            assertEquals("v1/chat/session", SessionMetadataWire.PATH)
            assertEquals("metadata", SessionMetadataWire.METADATA)
            assertEquals("signed_metadata", SessionMetadataWire.SIGNED_METADATA)
        }
    }

    @Nested
    @DisplayName("Response")
    inner class Answer {

        @Test
        fun `any 2xx counts as applied, whatever its body`() = runTest {
            val (sut, recorder) = makeSUT(
                ScriptedResponse(status = 200, body = """{"session":{}}"""),
                ScriptedResponse(status = 202, body = "accepted"),
                ScriptedResponse(status = 204, body = "")
            )

            repeat(3) {
                assertTrue(sut.patchSession(SessionMetadataPatch.Signed("token").mapToRemote()).isSuccess)
            }
            assertEquals(3, recorder.requests.size)
        }

        @Test
        fun `a 401 keeps the token - the host is not asked for a new session`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val (sut, recorder) = makeSUT(
                ScriptedResponse(status = 401, body = """{"error":{"message":"Unauthorized"}}"""),
                ScriptedResponse(status = 200, body = "{}"),
                tokens = tokens
            )

            val failed = sut.patchSession(SessionMetadataPatch.Signed("token").mapToRemote())
            sut.patchSession(SessionMetadataPatch.Signed("token").mapToRemote())

            assertEquals(401, assertIs<SessionMetadataRequestException>(failed.exceptionOrNull()).status)
            assertEquals("token-1", tokens.getToken())
            assertEquals("Bearer token-1", recorder.requests.last().header("Authorization"))
        }

        @Test
        fun `a 422, a 409 and a 401 reach the caller as the status that came`() = runTest {
            listOf(422, 409, 401, 503, 429).forEach { status ->
                val (sut, _) = makeSUT(ScriptedResponse(status = status, body = """{"error":{"message":"x"}}"""))

                val result = sut.patchSession(SessionMetadataPatch.Signed("token").mapToRemote())

                assertEquals(status, assertIs<SessionMetadataRequestException>(result.exceptionOrNull()).status)
            }
        }
    }

    private fun makeSUT(
        vararg responses: ScriptedResponse,
        tokens: TokenLocalDataSourceImpl = TokenLocalDataSourceImpl().apply { setToken("token-1") }
    ): Pair<ChatbotRemoteDataSource, ScriptedInterceptor> {
        val recorder = ScriptedInterceptor(responses.toList())
        val okHttp = buildOkHttpClient(
            tokenLocalDataSource = tokens,
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

    private fun Request.bodyText(): String = Buffer().also { checkNotNull(body).writeTo(it) }.readUtf8()

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
