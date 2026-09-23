package ai.askdiverge.data.remote

import app.cash.turbine.test
import ai.askdiverge.data.model.event.StreamDoneEventRemote
import ai.askdiverge.data.model.event.StreamErrorEventRemote
import ai.askdiverge.data.model.event.StreamPartEventRemote
import ai.askdiverge.data.model.event.StreamStatusEventRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.adapter.StreamEventAdapter
import ai.askdiverge.data.remote.adapter.buildConversationModels
import ai.askdiverge.data.remote.service.ChatbotStreamServiceImpl
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class ChatbotStreamServiceImplTest {

    private val moshi = Moshi.Builder().buildConversationModels().build()
    private val baseRequest = Request.Builder().url("https://example.com/messages").build()
    private val listenerSlot = slot<EventSourceListener>()
    private val mockEventSource = mockk<EventSource>(relaxed = true)
    private val eventSourceFactory = mockk<EventSource.Factory> {
        every { newEventSource(any(), capture(listenerSlot)) } returns mockEventSource
    }

    private val sseService = ChatbotStreamServiceImpl(
        request = baseRequest,
        eventSourceFactory = eventSourceFactory,
        sendMessageAdapter = moshi.adapter(SendMessageRemote::class.java),
        sseAdapter = StreamEventAdapter(moshi)
    )

    private val testMessage = SendMessageRemote(
        message = OutgoingMessageRemote(parts = listOf(OutgoingTextPartRemote(text = "hello")))
    )

    @Nested
    @DisplayName("When SSE event is received")
    inner class EventTest {

        @Test
        fun `emits status event`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onEvent(mockEventSource, null, "status", """{"status":"thinking"}""")

                val item = awaitItem()
                assertIs<StreamStatusEventRemote>(item)
                assertEquals(expected = "thinking", actual = item.status)

                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `skips events with unknown type`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onEvent(mockEventSource, null, "unknown_type", """{"status":"x"}""")
                listenerSlot.captured.onEvent(mockEventSource, null, "status", """{"status":"done"}""")

                val item = awaitItem()
                assertIs<StreamStatusEventRemote>(item)
                assertEquals("done", item.status)

                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `emits multiple events in sequence`() = runTest {
            val partJson = """{"part":{"type":"rich_text","part_id":"p1",
                |"blocks":[{"type":"paragraph","spans":[{"text":"hello","type":"text"}]}]}}
            """.trimMargin()
            val doneJson = """{"message":{"message_id":"m1","role":"assistant","parts":[{"type":"rich_text","part_id":"p1",
                |"blocks":[{"type":"paragraph","spans":[{"text":"hello","type":"text"}]}]}]}}
            """.trimMargin()

            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onEvent(mockEventSource, null, "status", """{"status":"thinking"}""")
                listenerSlot.captured.onEvent(mockEventSource, null, "part", partJson)
                listenerSlot.captured.onEvent(mockEventSource, null, "part", partJson)
                listenerSlot.captured.onEvent(mockEventSource, null, "done", doneJson)

                assertIs<StreamStatusEventRemote>(awaitItem())
                assertIs<StreamPartEventRemote>(awaitItem())
                assertIs<StreamPartEventRemote>(awaitItem())

                val doneItem = awaitItem()
                assertIs<StreamDoneEventRemote>(doneItem)
                assertEquals(expected = "m1", actual = doneItem.message.message_id)

                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `emits error event`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onEvent(
                    mockEventSource,
                    null,
                    "error",
                    """{"code":"generation_failed","message":"Please try again.","retryable":true}"""
                )

                val item = awaitItem()
                assertIs<StreamErrorEventRemote>(item)
                assertEquals(expected = "generation_failed", actual = item.code)
                assertEquals(expected = "Please try again.", actual = item.message)
                assertEquals(expected = true, actual = item.retryable)

                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `skips events with malformed JSON`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onEvent(mockEventSource, null, "status", "{invalid json}")
                listenerSlot.captured.onEvent(mockEventSource, null, "status", """{"status":"ok"}""")

                assertEquals(expected = "ok", actual = (awaitItem() as StreamStatusEventRemote).status)

                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("When connection lifecycle changes")
    inner class LifecycleTest {

        @Test
        fun `completes flow when onClosed is called`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `closes flow with error when onFailure is called`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onFailure(mockEventSource, IOException("timeout"), null)
                assertEquals(expected = "timeout", actual = awaitError().message)
            }
        }

        @Test
        fun `closes flow with default error when onFailure has no throwable`() = runTest {
            sseService.sendMessage(testMessage).test {
                listenerSlot.captured.onFailure(mockEventSource, null, null)
                assertEquals(expected = "SSE connection failed", actual = awaitError().message)
            }
        }

        @Test
        fun `cancels event source when flow is cancelled`() = runTest {
            sseService.sendMessage(testMessage).test { cancel() }
            verify { mockEventSource.cancel() }
        }
    }

    @Nested
    @DisplayName("When building the request")
    inner class RequestTest {

        @Test
        fun `does not include Authorization header (interceptor responsibility)`() = runTest {
            val requestSlot = slot<Request>()
            every { eventSourceFactory.newEventSource(capture(requestSlot), capture(listenerSlot)) } returns mockEventSource

            sseService.sendMessage(testMessage).test {
                assertNull(requestSlot.captured.header("Authorization"))
                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }

        @Test
        fun `sets POST method with JSON body`() = runTest {
            val requestSlot = slot<Request>()
            every { eventSourceFactory.newEventSource(capture(requestSlot), capture(listenerSlot)) } returns mockEventSource

            sseService.sendMessage(testMessage).test {
                assertEquals(expected = "POST", actual = requestSlot.captured.method)
                assertEquals(expected = "application/json; charset=utf-8", actual = requestSlot.captured.body?.contentType().toString())
                listenerSlot.captured.onClosed(mockEventSource)
                awaitComplete()
            }
        }
    }
}
