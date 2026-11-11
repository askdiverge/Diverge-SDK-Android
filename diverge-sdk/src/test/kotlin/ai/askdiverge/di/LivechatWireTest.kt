package ai.askdiverge.di

import ai.askdiverge.DivergeEnvironment
import ai.askdiverge.data.interceptor.ClientProfile
import ai.askdiverge.data.local.datasource.TokenLocalDataSourceImpl
import ai.askdiverge.data.mapper.mapToRemote
import ai.askdiverge.data.model.livechat.LivechatWire
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * What leaves the SDK on the visitor livechat endpoints. No server is involved: a terminal
 * interceptor records each request and answers it in-process.
 */
internal class LivechatWireTest {

    private val syncBody = """
        {
          "state": {
            "status": "waiting",
            "is_agent_typing": false,
            "feedback": { "status": "not_available" },
            "state_version": 1
          },
          "messages": [],
          "has_more": false,
          "sync_cursor": "cursor-1"
        }
    """.trimIndent()

    private val messageBody = """
        {
          "message": {
            "message_id": "lc_1",
            "role": "user",
            "parts": [],
            "created_at": "2026-01-01T00:00:00Z",
            "sequence_number": 1
          }
        }
    """.trimIndent()

    @Nested
    @DisplayName("GET /livechat/sync")
    inner class SyncCalls {

        @Test
        fun `sync query items use the wire literals`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = syncBody))

            val result = sut.syncLivechat(
                afterSequenceNumber = 7,
                waitMs = LivechatSessionMaxWait,
                syncCursor = "abc"
            )

            assertTrue(result.isSuccess)
            val request = recorder.requests.single()
            assertEquals("GET", request.method)
            assertTrue(request.url.encodedPath.endsWith("/v1/chat/livechat/sync"))
            assertEquals("7", request.url.queryParameter(LivechatWire.AFTER_SEQUENCE_NUMBER))
            assertEquals(LivechatSessionMaxWait.toString(), request.url.queryParameter(LivechatWire.WAIT_MS))
            assertEquals("abc", request.url.queryParameter(LivechatWire.SYNC_CURSOR))
            assertEquals(LivechatWire.AFTER_SEQUENCE_NUMBER, "after_sequence_number")
            assertEquals(LivechatWire.WAIT_MS, "wait_ms")
            assertEquals(LivechatWire.SYNC_CURSOR, "sync_cursor")
        }

        @Test
        fun `a 401 on sync surfaces as session expired`() = runTest {
            val (sut, _) = makeSUT(ScriptedResponse(status = 401, body = ""))

            val result = sut.syncLivechat(
                afterSequenceNumber = null,
                waitMs = null,
                syncCursor = null
            )

            assertIs<ChatbotException.SessionExpired>(result.exceptionOrNull())
        }
    }

    @Nested
    @DisplayName("POST /livechat/messages")
    inner class MessageCalls {

        @Test
        fun `send body carries client_operation_id`() = runTest {
            val (sut, recorder) = makeSUT(ScriptedResponse(status = 200, body = messageBody))

            val result = sut.sendLivechatMessage(
                LivechatVisitorMessageRequest(
                    text = "hello agent",
                    currentPage = "https://shop.example/cart",
                    clientOperationId = "op-42"
                ).mapToRemote()
            )

            assertTrue(result.isSuccess)
            val request = recorder.requests.single()
            assertEquals("POST", request.method)
            assertTrue(request.url.encodedPath.endsWith("/v1/chat/livechat/messages"))
            val body = recorder.bodies.single()
            assertTrue(body.contains("\"client_operation_id\":\"op-42\""))
            assertTrue(body.contains("\"text\":\"hello agent\""))
            assertTrue(body.contains("\"page\":\"https://shop.example/cart\""))
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
        val bodies = CopyOnWriteArrayList<String>()
        private val index = AtomicInteger(0)

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                bodies += buffer.readUtf8()
            }
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

    private companion object {
        const val LivechatSessionMaxWait = 20_000
    }
}
