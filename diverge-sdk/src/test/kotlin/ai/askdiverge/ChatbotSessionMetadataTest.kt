package ai.askdiverge

import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.metadata.ScriptedPatcher
import ai.askdiverge.domain.model.metadata.JsonValue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatbotSessionMetadataTest {

    @BeforeTest
    fun setUp() {
        mockkObject(ChatbotLogger)
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(ChatbotLogger)
    }

    @Nested
    @DisplayName("setMetadata")
    inner class SetMetadata {

        @Test
        fun `Kotlin values go out as JSON values, nested and with nulls kept`() = runTest {
            val (sut, patcher) = makeSUT()

            sut.setMetadata(
                mapOf(
                    "storeSlug" to "boozt-se",
                    "screen" to "product/${42}",
                    "loggedIn" to false,
                    "visits" to 3,
                    "customerNumber" to 9_000_000_000L,
                    "rating" to 0.1f,
                    "total" to 49.5,
                    "coupon" to null,
                    "cart" to mapOf("items" to listOf("sku-1", 2, mapOf("gift" to true)))
                )
            )

            assertEquals(
                listOf<SessionMetadataPatch>(
                    SessionMetadataPatch.Metadata(
                        mapOf(
                            "storeSlug" to JsonValue.Str("boozt-se"),
                            "screen" to JsonValue.Str("product/42"),
                            "loggedIn" to JsonValue.Bool(false),
                            "visits" to JsonValue.Integer(3),
                            "customerNumber" to JsonValue.Integer(9_000_000_000L),
                            "rating" to JsonValue.Decimal(0.1),
                            "total" to JsonValue.Decimal(49.5),
                            "coupon" to JsonValue.Null,
                            "cart" to JsonValue.Object(
                                mapOf(
                                    "items" to JsonValue.Array(
                                        listOf(
                                            JsonValue.Str("sku-1"),
                                            JsonValue.Integer(2),
                                            JsonValue.Object(mapOf("gift" to JsonValue.Bool(true)))
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                patcher.sent
            )
        }

        @Test
        fun `an empty map sends nothing`() = runTest {
            val (sut, patcher) = makeSUT()

            sut.setMetadata(emptyMap())

            assertEquals(0, patcher.attempts)
        }

        @Test
        fun `an unsupported value, NaN or infinity drops the call without throwing`() = runTest {
            val rejected = listOf(
                mapOf("joined" to java.util.Date()),
                mapOf("ratio" to Double.NaN),
                mapOf("ratio" to Float.POSITIVE_INFINITY),
                mapOf("cart" to mapOf(1 to "one")),
                mapOf("tags" to listOf("ok", Any()))
            )
            rejected.forEach { metadata ->
                val (sut, patcher) = makeSUT()

                sut.setMetadata(mapOf("kept" to "no") + metadata)

                assertEquals(0, patcher.attempts, "$metadata")
            }
        }

        @Test
        fun `a dropped call logs an error that names no value`() = runTest {
            val (sut, _) = makeSUT()
            val logged = slot<String>()

            sut.setMetadata(mapOf("email" to "visitor@example.com", "joined" to java.util.Date()))

            verify(exactly = 1) { ChatbotLogger.logError(capture(logged)) }
            assertFalse(logged.captured.contains("visitor@example.com"))
            assertFalse(logged.captured.contains("email"))
        }

        @Test
        fun `a map that contains itself drops the call without throwing`() = runTest {
            val (sut, patcher) = makeSUT()
            val looped = HashMap<String, Any?>()
            looped["self"] = looped

            sut.setMetadata(looped)

            assertEquals(0, patcher.attempts)
        }
    }

    @Nested
    @DisplayName("setSignedMetadata")
    inner class SetSignedMetadata {

        @Test
        fun `a token is trimmed and a blank one sends nothing`() = runTest {
            val (sut, patcher) = makeSUT()

            sut.setSignedMetadata("  \n ")
            sut.setSignedMetadata("  header.claims.signature\n")

            assertEquals(listOf<SessionMetadataPatch>(SessionMetadataPatch.Signed("header.claims.signature")), patcher.sent)
        }
    }

    @Nested
    @DisplayName("clear")
    inner class Clear {

        @Test
        fun `clear drops what waits, so an earlier visitor's metadata stays behind`() = runTest {
            val sut = ChatbotSessionMetadata()
            val patcher = ScriptedPatcher()
            sut.setMetadata(mapOf("customer" to "first visitor"))
            sut.setSignedMetadata("first visitor's token")

            sut.clear()
            sut.setMetadata(mapOf("customer" to "next visitor"))
            sut.queue.attach(chat = Any(), scope = chatScope(), patcher = patcher::patch)

            assertEquals(
                listOf<SessionMetadataPatch>(
                    SessionMetadataPatch.Metadata(mapOf("customer" to JsonValue.Str("next visitor")))
                ),
                patcher.sent
            )
            assertTrue(patcher.tried.none { it is SessionMetadataPatch.Signed })
        }
    }

    // The public calls with a chat already attached, so each one goes at once.
    private fun TestScope.makeSUT(): Pair<ChatbotSessionMetadata, ScriptedPatcher> {
        val sut = ChatbotSessionMetadata()
        val patcher = ScriptedPatcher()
        sut.queue.attach(chat = Any(), scope = chatScope(), patcher = patcher::patch)
        return sut to patcher
    }

    private fun TestScope.chatScope(): CoroutineScope =
        CoroutineScope(Job(backgroundScope.coroutineContext[Job]) + UnconfinedTestDispatcher(testScheduler))
}
