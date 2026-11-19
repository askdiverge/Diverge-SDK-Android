package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.model.export.VisitorExportWire
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.export.ExportDestination
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
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
            val body = """{"exported_at":"2026-01-01T00:00:00Z","conversations":[]}"""
            val destination = RecordingDestination()
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = body))

            val result = sut.exportMyData(destination)

            assertTrue(result.isSuccess)
            assertEquals(body, destination.written.toString(Charsets.UTF_8))
            assertFalse(destination.deleted)
            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/${VisitorExportWire.PATH}"))
            assertEquals("Bearer token-1", request.header("Authorization"))
            assertEquals(VisitorExportWire.PATH, "v1/chat/export")
        }

        @Test
        fun `a JSON array body is rejected`() = runTest {
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(ScriptedResponse(status = 200, body = """[{"id":1}]"""))

            val result = sut.exportMyData(destination)

            assertTrue(result.isFailure)
            assertEquals(0, destination.written.size())
            assertTrue(destination.deleted)
        }

        @Test
        fun `a 401 on export surfaces as session expired`() = runTest {
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(ScriptedResponse(status = 401, body = ""))

            val result = sut.exportMyData(destination)

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
            assertTrue(destination.deleted)
        }

        @Test
        fun `a server error on export is a plain failure and not a session expiry`() = runTest {
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(
                ScriptedResponse(status = 404, body = """{"error":"Chatbot not found"}""")
            )

            val result = sut.exportMyData(destination)

            val error = assertNotNull(result.exceptionOrNull())
            assertFalse(error is ChatbotException)
            assertTrue(destination.deleted)
        }

        @Test
        fun `a cancelled export stops copying and deletes the destination`() = runTest {
            val body = """{"padding":"${"x".repeat(64 * 1024)}"}"""
            lateinit var export: Job
            val destination = RecordingDestination(onOpen = { export.cancel() })
            val (sut, _) = makeSUT(ScriptedResponse(status = 200, body = body))

            export = launch { sut.exportMyData(destination) }
            export.join()

            assertTrue(export.isCancelled)
            assertTrue(destination.written.size() < body.length)
            assertTrue(destination.deleted)
        }

        @Test
        fun `a cancelled export that then fails still deletes the destination`() = runTest {
            lateinit var export: Job
            val destination = RecordingDestination(onOpen = {
                export.cancel()
                throw IOException("No space left on device")
            })
            val (sut, _) = makeSUT(
                ScriptedResponse(status = 200, body = """{"conversations":[]}""")
            )

            export = launch { sut.exportMyData(destination) }
            export.join()

            assertTrue(destination.deleted)
        }

        @Test
        fun `cancelling an export ends a read that is waiting on the network`() = runTest {
            val stalled = CompletableDeferred<Unit>()
            val destination = RecordingDestination()
            val (sut, _) = makeSUT(
                ScriptedResponse(status = 200, body = """{"conversations":[""", stalled = stalled)
            )
            val export = launch { sut.exportMyData(destination) }
            stalled.await()

            export.cancel()

            assertNotNull(
                joinWithin(export, millis = 2_000),
                "the cancelled export was still waiting on the network"
            )
            assertTrue(destination.deleted)
        }

        @Test
        fun `a retry after a cancelled export keeps the document it saves into`() = runTest {
            val stalled = CompletableDeferred<Unit>()
            val document = SharedDocument()
            val (sut, _) = makeSUT(
                ScriptedResponse(status = 200, body = """{"conversations":[""", stalled = stalled),
                ScriptedResponse(status = 200, body = """{"conversations":[]}""")
            )
            val cancelled = launch { sut.exportMyData(document.destination()) }
            stalled.await()
            cancelled.cancel()
            assertNotNull(
                joinWithin(cancelled, millis = 2_000),
                "the cancelled export was still waiting on the network"
            )

            val retry = sut.exportMyData(document.destination())

            assertTrue(retry.isSuccess)
            assertEquals(listOf("opened", "deleted", "opened"), document.events)
        }
    }

    // Waits in real time, as the export copies on the IO dispatcher; null when it took longer.
    private suspend fun joinWithin(job: Job, millis: Long): Unit? =
        withContext(Dispatchers.Default) { withTimeoutOrNull(millis) { job.join() } }

    /** Records what the export writes, and whether it asked for the document to be removed. */
    private class RecordingDestination(private val onOpen: () -> Unit = {}) : ExportDestination {

        val written = ByteArrayOutputStream()

        @Volatile
        var deleted = false
            private set

        override fun openOutputStream(): OutputStream {
            onOpen()
            return written
        }

        override fun delete() {
            deleted = true
        }
    }

    /** One document that exports save into in turn; records the order it is opened and deleted in. */
    private class SharedDocument {

        val events = CopyOnWriteArrayList<String>()

        fun destination(): ExportDestination = object : ExportDestination {

            override fun openOutputStream(): OutputStream {
                events += "opened"
                return ByteArrayOutputStream()
            }

            override fun delete() {
                events += "deleted"
            }
        }
    }

    private fun makeSUT(
        vararg responses: ScriptedResponse
    ): Pair<ChatbotRemoteDataSource, ScriptedInterceptor> {
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

    /** With [stalled], the body stops arriving after [body], and [stalled] completes at that point. */
    private data class ScriptedResponse(
        val status: Int,
        val body: String,
        val stalled: CompletableDeferred<Unit>? = null
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
                .body(
                    scripted.stalled
                        ?.let { stalled -> StalledBody(scripted.body, stalled, chain.call()) }
                        ?: scripted.body.toResponseBody("application/json".toMediaType())
                )
                .build()
        }
    }

    /**
     * A body whose bytes stop arriving after [start]. The waiting read fails once [call] is
     * cancelled, as a socket read does, or with a read timeout when it never is.
     */
    private class StalledBody(
        start: String,
        stalled: CompletableDeferred<Unit>,
        call: Call
    ) : ResponseBody() {

        private val source = object : Source {

            private var started = false

            override fun read(sink: Buffer, byteCount: Long): Long {
                if (!started) {
                    started = true
                    val bytes = start.encodeToByteArray()
                    sink.write(bytes)
                    return bytes.size.toLong()
                }
                stalled.complete(Unit)
                val deadline = System.nanoTime() + READ_TIMEOUT_NANOS
                while (System.nanoTime() < deadline) {
                    if (call.isCanceled()) throw IOException("Canceled")
                    Thread.sleep(10)
                }
                throw SocketTimeoutException("timeout")
            }

            override fun timeout(): Timeout = Timeout.NONE

            override fun close() = Unit
        }.buffer()

        override fun contentType(): MediaType = "application/json".toMediaType()

        override fun contentLength(): Long = -1L

        override fun source(): BufferedSource = source

        private companion object {
            const val READ_TIMEOUT_NANOS = 5_000_000_000L
        }
    }
}
