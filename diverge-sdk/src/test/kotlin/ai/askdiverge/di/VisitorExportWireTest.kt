package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.model.export.VisitorExportWire
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.export.ExportDestination
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
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
 * What leaves the SDK on `GET /chat/export`. No server is involved: a terminal interceptor
 * records each request and answers it in-process.
 */
internal class VisitorExportWireTest {

    @Nested
    @DisplayName("GET /chat/export")
    inner class ExportCalls {

        @Test
        fun `export uses the wire path and sends the bearer`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val body = """{"exported_at":"2026-01-01T00:00:00Z","conversations":[]}"""
            val destination = RecordingDestination()
            val (sut, recorder) = makeSUT(
                tokens = tokens,
                ScriptedResponse(status = 200, body = body)
            )

            val result = sut.exportMyData(destination)

            assertTrue(result.isSuccess)
            assertEquals(body, destination.written.toString(Charsets.UTF_8))
            assertFalse(destination.deleted)
            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/${VisitorExportWire.PATH}"))
            assertEquals("Bearer token-1", request.header("Authorization"))
            assertEquals(VisitorExportWire.PATH, "v1/chat/export")
            assertEquals("token-1", tokens.getToken())
        }

        @Test
        fun `a JSON array body is rejected and the token stays`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(
                tokens = tokens,
                ScriptedResponse(status = 200, body = """[{"id":1}]""")
            )

            val result = sut.exportMyData(destination)

            assertTrue(result.isFailure)
            assertEquals(0, destination.written.size())
            assertTrue(destination.deleted)
            assertEquals("token-1", tokens.getToken())
        }

        @Test
        fun `a 401 on export surfaces as session expired and keeps the token`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(
                tokens = tokens,
                ScriptedResponse(status = 401, body = "")
            )

            val result = sut.exportMyData(destination)

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
            assertTrue(destination.deleted)
            assertEquals("token-1", tokens.getToken())
        }

        @Test
        fun `a server error on export is a plain failure and not a session expiry`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(
                tokens = tokens,
                ScriptedResponse(status = 404, body = """{"error":"Chatbot not found"}""")
            )

            val result = sut.exportMyData(destination)

            val error = assertNotNull(result.exceptionOrNull())
            assertFalse(error is ChatbotException)
            assertTrue(destination.deleted)
            assertEquals("token-1", tokens.getToken())
        }

        @Test
        fun `a cancelled export deletes the destination`() = runTest {
            val tokens = TokenLocalDataSourceImpl().apply { setToken("token-1") }
            val destination = RecordingDestination(openFailure = CancellationException("cancelled"))
            val (sut, _) = makeSUT(
                tokens = tokens,
                ScriptedResponse(status = 200, body = """{"conversations":[]}""")
            )

            assertFailsWith<CancellationException> { sut.exportMyData(destination) }

            assertTrue(destination.deleted)
        }
    }

    /** Records what the export writes, and whether it asked for the document to be removed. */
    private class RecordingDestination(private val openFailure: Exception? = null) : ExportDestination {

        val written = ByteArrayOutputStream()

        @Volatile
        var deleted = false
            private set

        override fun openOutputStream(): OutputStream {
            openFailure?.let { throw it }
            return written
        }

        override fun delete() {
            deleted = true
        }
    }

    private fun makeSUT(
        tokens: TokenLocalDataSourceImpl,
        vararg responses: ScriptedResponse
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
