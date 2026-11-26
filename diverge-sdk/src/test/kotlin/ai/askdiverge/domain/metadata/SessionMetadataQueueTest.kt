package ai.askdiverge.domain.metadata

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.SessionMetadataRequestException
import ai.askdiverge.domain.model.metadata.JsonValue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
internal class SessionMetadataQueueTest {

    @Nested
    @DisplayName("Order and merging")
    inner class OrderAndMerging {

        @Test
        fun `nothing is sent before a chat is attached, then what waits goes in call order`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            sut.enqueue(screen("checkout"))
            sut.enqueue(signed("token"))
            assertEquals(0, patcher.attempts)

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(listOf(screen("checkout"), signed("token")), patcher.sent)
        }

        @Test
        fun `the newest waiting token replaces the one before it, and unsigned calls merge around it`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            sut.enqueue(signed("signed at login"))
            sut.enqueue(screen("home"))
            sut.enqueue(signed("signed as the chat opens"))
            sut.enqueue(screen("checkout"))

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(2, patcher.attempts)
            assertEquals(listOf(screen("checkout"), signed("signed as the chat opens")), patcher.sent)
        }

        @Test
        fun `a token on its way is sent, and the newest of those set meanwhile follows it`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val (sut, patcher, chat) = makeSUT(gate = gate)
            sut.enqueue(signed("first"))
            sut.attach(chat, chatScope(), patcher::patch)
            assertEquals(1, patcher.attempts)

            sut.enqueue(signed("second"))
            sut.enqueue(signed("third"))
            gate.complete(Unit)

            assertEquals(listOf(signed("first"), signed("third")), patcher.sent)
        }

        @Test
        fun `unsigned calls waiting for a chat go as one patch, merged as the backend merges them`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            sut.enqueue(
                metadata(
                    "screen" to JsonValue.Str("home"),
                    "cart" to JsonValue.Object(mapOf("items" to JsonValue.Integer(1))),
                    "tags" to JsonValue.Array(listOf(JsonValue.Str("a")))
                )
            )
            sut.enqueue(
                metadata(
                    "screen" to JsonValue.Str("checkout"),
                    "cart" to JsonValue.Object(mapOf("total" to JsonValue.Decimal(49.5))),
                    "tags" to JsonValue.Array(emptyList())
                )
            )

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(1, patcher.attempts)
            assertEquals(
                listOf(
                    metadata(
                        "screen" to JsonValue.Str("checkout"),
                        "cart" to JsonValue.Object(
                            mapOf("items" to JsonValue.Integer(1), "total" to JsonValue.Decimal(49.5))
                        ),
                        "tags" to JsonValue.Array(emptyList())
                    )
                ),
                patcher.sent
            )
        }

        @Test
        fun `an object set over another kind of value goes as its own patch`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val cleared = metadata("cart" to JsonValue.Null)
            val refilled = metadata("cart" to JsonValue.Object(mapOf("items" to JsonValue.Integer(2))))
            sut.enqueue(cleared)
            sut.enqueue(refilled)

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(listOf(cleared, refilled), patcher.sent)
        }

        @Test
        fun `a call made while a patch is on its way goes after it, and later calls merge into that one`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val (sut, patcher, chat) = makeSUT(gate = gate)
            sut.enqueue(screen("first"))
            sut.attach(chat, chatScope(), patcher::patch)
            assertEquals(1, patcher.attempts)

            sut.enqueue(screen("second"))
            sut.enqueue(screen("third"))
            gate.complete(Unit)

            assertEquals(listOf(screen("first"), screen("third")), patcher.sent)
        }

        @Test
        fun `calls from several threads at once all reach the queue`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val threads = 8
            val callsPerThread = 50
            val start = CountDownLatch(1)

            val workers = (0 until threads).map { t ->
                thread {
                    start.await()
                    repeat(callsPerThread) { i -> sut.enqueue(metadata("key-$t-$i" to JsonValue.Integer(i.toLong()))) }
                }
            }
            start.countDown()
            workers.forEach { it.join() }
            sut.attach(chat, chatScope(), patcher::patch)

            val keys = patcher.sent.flatMap { (it as SessionMetadataPatch.Metadata).values.keys }
            assertEquals(threads * callsPerThread, keys.toSet().size)
        }
    }

    @Nested
    @DisplayName("Outcome of a send")
    inner class Outcome {

        @Test
        fun `a patch answered 408, 429 or 5xx stays queued, with those after it, until the next attach`() = runTest {
            listOf(408, 429, 500, 503).forEach { status ->
                val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to SessionMetadataRequestException(status)))
                val scope = chatScope()
                sut.enqueue(screen("first"))
                sut.enqueue(signed("token"))

                sut.attach(chat, scope, patcher::patch)
                assertEquals(1, patcher.attempts, "status $status")

                sut.attach(chat, scope, patcher::patch)

                assertEquals(listOf(screen("first"), signed("token")), patcher.sent, "status $status")
            }
        }

        @Test
        fun `a patch that got no response stays queued until the next trigger`() = runTest {
            listOf(IOException("timeout"), ChatbotException.NoTokenAvailable(), IllegalStateException()).forEach { error ->
                val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to error))
                sut.enqueue(screen("first"))

                sut.attach(chat, chatScope(), patcher::patch)
                assertEquals(1, patcher.attempts)
                sut.enqueue(signed("token"))

                assertEquals(listOf(screen("first"), signed("token")), patcher.sent, "$error")
            }
        }

        @Test
        fun `a 401 drops the patch, as the web widget drops every 4xx`() = runTest {
            val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to SessionMetadataRequestException(401)))
            val scope = chatScope()
            sut.enqueue(signed("token"))
            sut.enqueue(screen("next"))

            sut.attach(chat, scope, patcher::patch)
            sut.attach(chat, scope, patcher::patch)

            assertEquals(listOf(screen("next")), patcher.sent)
            assertEquals(2, patcher.attempts)
        }

        @Test
        fun `a 409 drops the patch - the backend has no session for it`() = runTest {
            val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to SessionMetadataRequestException(409)))
            sut.enqueue(signed("token"))
            sut.enqueue(screen("next"))

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(listOf(screen("next")), patcher.sent)
            assertEquals(2, patcher.attempts)
        }

        @Test
        fun `a patch the backend refuses is dropped and the next one goes`() = runTest {
            val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to SessionMetadataRequestException(422)))
            sut.enqueue(signed("expired"))
            sut.enqueue(screen("next"))

            sut.attach(chat, chatScope(), patcher::patch)

            assertEquals(listOf(screen("next")), patcher.sent)
            assertEquals(2, patcher.attempts)
        }

        @Test
        fun `a cancelled send keeps the patch for the next attach`() = runTest {
            val (sut, patcher, chat) = makeSUT(failures = mapOf(0 to CancellationException("stopped")))
            val scope = chatScope()
            sut.enqueue(screen("checkout"))

            sut.attach(chat, scope, patcher::patch)
            assertTrue(patcher.sent.isEmpty())
            sut.attach(chat, scope, patcher::patch)

            assertEquals(listOf(screen("checkout")), patcher.sent)
        }
    }

    @Nested
    @DisplayName("Attaching and detaching")
    inner class Attaching {

        @Test
        fun `a detached chat sends nothing, and what is queued goes through the next chat attached`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val next = ScriptedPatcher()
            sut.attach(chat, chatScope(), patcher::patch)

            sut.detach(chat)
            sut.enqueue(screen("checkout"))
            assertEquals(0, patcher.attempts)

            sut.attach(Any(), chatScope(), next::patch)

            assertEquals(listOf(screen("checkout")), next.sent)
            assertEquals(0, patcher.attempts)
        }

        @Test
        fun `detaching stops the patch on its way, which goes again through the next chat`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val (sut, patcher, chat) = makeSUT(gate = gate)
            val next = ScriptedPatcher()
            sut.enqueue(screen("checkout"))
            sut.enqueue(signed("token"))
            sut.attach(chat, chatScope(), patcher::patch)
            assertEquals(1, patcher.attempts)

            sut.detach(chat)
            gate.complete(Unit)
            assertEquals(1, patcher.attempts)

            sut.attach(Any(), chatScope(), next::patch)

            assertEquals(listOf(screen("checkout"), signed("token")), next.sent)
        }

        @Test
        fun `a send that finishes after its chat detached leaves the queue alone`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val patcher = ScriptedPatcher(gate = gate, ignoresCancellation = true)
            val next = ScriptedPatcher()
            val sut = SessionMetadataQueue()
            val chat = Any()
            sut.enqueue(metadata("screen" to JsonValue.Str("home")))
            sut.attach(chat, chatScope(), patcher::patch)

            sut.detach(chat)
            sut.enqueue(metadata("cart" to JsonValue.Integer(2)))
            gate.complete(Unit)
            sut.attach(Any(), chatScope(), next::patch)

            assertEquals(
                listOf(metadata("screen" to JsonValue.Str("home"), "cart" to JsonValue.Integer(2))),
                next.sent
            )
        }

        @Test
        fun `a chat that leaves the screen after another was attached leaves that one sending`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val next = ScriptedPatcher()
            sut.attach(chat, chatScope(), patcher::patch)
            sut.attach(Any(), chatScope(), next::patch)

            sut.detach(chat)
            sut.enqueue(screen("checkout"))

            assertEquals(listOf(screen("checkout")), next.sent)
            assertEquals(0, patcher.attempts)
        }

        @Test
        fun `a chat that went away without detaching sends nothing, and the next chat sends what waits`() = runTest {
            val (sut, patcher, _) = makeSUT()
            val next = ScriptedPatcher()
            val gone = chatScope().apply { cancel() }
            sut.attach(Any(), gone, patcher::patch)

            sut.enqueue(screen("checkout"))
            assertEquals(0, patcher.attempts)

            sut.attach(Any(), chatScope(), next::patch)
            assertEquals(listOf(screen("checkout")), next.sent)
        }
    }

    @Nested
    @DisplayName("Clearing")
    inner class Clearing {

        @Test
        fun `clear drops what is queued and detaches the chat`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val scope = chatScope()
            sut.enqueue(screen("old visitor"))
            sut.attach(chat, scope, patcher::patch)
            assertEquals(1, patcher.sent.size)

            sut.detach(chat)
            sut.enqueue(signed("old visitor's token"))
            sut.clear()
            sut.enqueue(screen("new visitor"))
            assertEquals(listOf(screen("old visitor")), patcher.sent)

            sut.attach(chat, scope, patcher::patch)
            assertEquals(listOf(screen("old visitor"), screen("new visitor")), patcher.sent)
        }

        @Test
        fun `clear detaches an attached chat, so what is set after it waits for the next attach`() = runTest {
            val (sut, patcher, chat) = makeSUT()
            val scope = chatScope()
            sut.attach(chat, scope, patcher::patch)

            sut.clear()
            sut.enqueue(screen("new visitor"))
            assertEquals(0, patcher.attempts)

            sut.attach(chat, scope, patcher::patch)
            assertEquals(listOf(screen("new visitor")), patcher.sent)
        }

        @Test
        fun `clear while a patch is on its way leaves the next visitor's queue to itself`() = runTest {
            val gate = CompletableDeferred<Unit>()
            val (sut, patcher, chat) = makeSUT(gate = gate)
            val next = ScriptedPatcher()
            val scope = chatScope()
            sut.enqueue(screen("old visitor"))
            sut.attach(chat, scope, patcher::patch)
            assertEquals(1, patcher.attempts)

            sut.clear()
            sut.enqueue(screen("new visitor"))
            sut.attach(chat, scope, next::patch)
            gate.complete(Unit)

            assertEquals(listOf(screen("new visitor")), next.sent)
            assertEquals(1, next.attempts)
            assertEquals(1, patcher.attempts)
        }
    }

    private fun makeSUT(
        failures: Map<Int, Throwable> = emptyMap(),
        gate: CompletableDeferred<Unit>? = null
    ): Triple<SessionMetadataQueue, ScriptedPatcher, Any> =
        Triple(SessionMetadataQueue(), ScriptedPatcher(failures = failures, gate = gate), Any())

    // A chat's scope: runs each send at once, and ends with the test.
    private fun TestScope.chatScope(): CoroutineScope =
        CoroutineScope(Job(backgroundScope.coroutineContext[Job]) + UnconfinedTestDispatcher(testScheduler))

    private fun metadata(vararg values: Pair<String, JsonValue>): SessionMetadataPatch =
        SessionMetadataPatch.Metadata(mapOf(*values))
}
