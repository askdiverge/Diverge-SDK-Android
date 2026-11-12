package ai.askdiverge.domain.livechat

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatCloseReason
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import ai.askdiverge.domain.model.message.incoming.part.MessagePart
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("LivechatSession")
internal class LivechatSessionTest {

    @Nested
    @DisplayName("bootstrap")
    inner class Bootstrap {

        @Test
        fun `an open session replays the log and reports inSession`() = runTest {
            val repository = waitingRepository(
                messages = listOf(sampleMessage(id = "m1", sequence = 3))
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            val outcome = session.bootstrap()
            runCurrent()

            assertEquals(LivechatBootstrapOutcome.IN_SESSION, outcome)
            assertTrue(session.isInSession)
            assertEquals(listOf("m1"), snapshots.first().messages.map { it.messageId })
            assertTrue(snapshots.first().isRestored)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `pickUp publishes an open session as new`() = runTest {
            val repository = waitingRepository(
                messages = listOf(sampleMessage(id = "m1", sequence = 3))
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            val outcome = session.pickUp()
            runCurrent()

            assertEquals(LivechatBootstrapOutcome.IN_SESSION, outcome)
            assertEquals(LivechatState.Status.INACTIVE, snapshots.first().previousStatus)
            assertEquals(LivechatState.Status.WAITING, snapshots.first().state.status)
            assertFalse(snapshots.first().isRestored)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `idle bootstrap reports notInSession and does not keep polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.INACTIVE),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = null
                )
            )
            val session = makeSUT(repository)

            assertEquals(LivechatBootstrapOutcome.NOT_IN_SESSION, session.bootstrap())
            runCurrent()
            advanceTimeBy(LivechatSession.WAITING_INTERVAL_MS * 2)
            runCurrent()

            assertFalse(session.isInSession)
            coVerify(exactly = 1) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }

        @Test
        fun `a 401 reports session expired`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returns Result.failure(ChatbotException.SessionExpired())
            val session = makeSUT(repository)

            assertEquals(LivechatBootstrapOutcome.SESSION_EXPIRED, session.bootstrap())
            session.stop()
        }
    }

    @Nested
    @DisplayName("polling")
    inner class Polling {

        @Test
        fun `cursor advances with published messages`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.WAITING),
                        messages = listOf(sampleMessage(id = "m1", sequence = 5)),
                        lastSequenceNumber = 5,
                        hasMore = false,
                        syncCursor = "cursor"
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.WAITING),
                        messages = listOf(sampleMessage(id = "m2", sequence = 6)),
                        lastSequenceNumber = 6,
                        hasMore = false,
                        syncCursor = "cursor-2"
                    )
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            advanceTimeBy(LivechatSession.WAIT_GAP_MS)
            runCurrent()

            coVerify {
                repository.syncLivechat(
                    afterSequenceNumber = 5,
                    waitMs = LivechatSession.MAX_WAIT_MS,
                    syncCursor = "cursor"
                )
            }
            session.stop()
        }

        @Test
        fun `inactive status stops polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.WAITING),
                        messages = emptyList(),
                        lastSequenceNumber = null,
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.INACTIVE),
                        messages = emptyList(),
                        lastSequenceNumber = null,
                        hasMore = false,
                        syncCursor = null
                    )
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            advanceTimeBy(LivechatSession.WAITING_INTERVAL_MS)
            runCurrent()

            assertFalse(session.isInSession)
            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }

        @Test
        fun `teardown publishes inactive and clears inSession`() = runTest {
            val repository = waitingRepository(
                messages = emptyList(),
                status = LivechatState.Status.ACTIVE
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            session.bootstrap()
            runCurrent()
            assertTrue(session.isInSession)

            session.teardown()
            runCurrent()

            assertTrue(snapshots.any { it.state.status == LivechatState.Status.INACTIVE })
            assertFalse(session.isInSession)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `refresh ticks once without waiting`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.ACTIVE),
                        messages = emptyList(),
                        lastSequenceNumber = null,
                        hasMore = false,
                        syncCursor = "c1"
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(
                            status = LivechatState.Status.CLOSED,
                            feedback = LivechatState.Feedback.Status.SUBMITTED
                        ),
                        messages = emptyList(),
                        lastSequenceNumber = null,
                        hasMore = false,
                        syncCursor = null
                    )
                )
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            session.bootstrap()
            runCurrent()
            session.pause()
            session.refresh()
            runCurrent()

            assertTrue(snapshots.any { it.state.status == LivechatState.Status.CLOSED })
            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("handover and close")
    inner class HandoverAndClose {

        @Test
        fun `handover publishes waiting and starts polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.requestLivechatHandover(any())
            } returns Result.success(LivechatState.Status.WAITING)
            coEvery {
                repository.syncLivechat(any(), any(), any())
            } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.WAITING),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = null
                )
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            session.handover(
                source = LivechatHandoverRequest.Source.MANUAL_BUTTON,
                partId = null,
                clientContext = LivechatClientContext.native(page = null)
            )
            runCurrent()

            assertTrue(session.isInSession)
            assertEquals(LivechatState.Status.WAITING, snapshots.last().state.status)
            advanceTimeBy(LivechatSession.WAITING_INTERVAL_MS)
            runCurrent()
            coVerify(atLeast = 1) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `a 409 on handover fails with Conflict`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.requestLivechatHandover(any())
            } returns Result.failure(LivechatRequestException.Conflict())
            val session = makeSUT(repository)

            assertFailsWith<LivechatRequestException.Conflict> {
                session.handover(
                    source = LivechatHandoverRequest.Source.MANUAL_BUTTON,
                    partId = null,
                    clientContext = null
                )
            }
            assertFalse(session.isInSession)
            session.stop()
        }

        @Test
        fun `close leaves the session inactive`() = runTest {
            val waiting = LivechatSync(
                state = LivechatState.of(LivechatState.Status.WAITING),
                messages = emptyList(),
                lastSequenceNumber = null,
                hasMore = false,
                syncCursor = null
            )
            val closed = LivechatSync(
                state = LivechatState.of(LivechatState.Status.CLOSED),
                messages = emptyList(),
                lastSequenceNumber = null,
                hasMore = false,
                syncCursor = null
            )
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(waiting),
                Result.success(closed)
            ) andThen Result.success(closed)
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()
            assertTrue(session.isInSession)

            session.close(reason = LivechatCloseReason.ENDED_BY_VISITOR)
            runCurrent()

            assertFalse(session.isInSession)
            coVerify { repository.closeLivechat(LivechatCloseReason.ENDED_BY_VISITOR) }
            session.stop()
        }

        @Test
        fun `a 401 on handover reports session expired`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.requestLivechatHandover(any())
            } returns Result.failure(ChatbotException.SessionExpired())
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            assertFailsWith<ChatbotException.SessionExpired> {
                session.handover(
                    source = LivechatHandoverRequest.Source.ASSISTANT_MARKER,
                    partId = "part-1",
                    clientContext = null
                )
            }
            runCurrent()
            assertTrue(snapshots.any { it.sessionExpired })
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `a failed close resumes polling the open session`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.closeLivechat(any()) } returns Result.failure(IOException("timeout"))
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            assertFailsWith<IOException> { session.close(reason = LivechatCloseReason.ENDED_BY_VISITOR) }
            runCurrent()

            assertTrue(session.isInSession)
            // The bootstrap read, then the immediate read polling resumes with.
            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }

        @Test
        fun `a close whose read still shows a session keeps polling`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.close(reason = LivechatCloseReason.ENDED_BY_VISITOR)
            advanceTimeBy(LivechatSession.ACTIVE_INTERVAL_MS + 1)
            runCurrent()

            assertTrue(session.isInSession)
            // Bootstrap, the read after the close, then the next poll.
            coVerify(exactly = 3) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }

        @Test
        fun `teardown does not wait for a handover in flight`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.requestLivechatHandover(any()) } coAnswers { awaitCancellation() }
            val session = makeSUT(repository)
            val handover = launch {
                session.handover(
                    source = LivechatHandoverRequest.Source.MANUAL_BUTTON,
                    partId = null,
                    clientContext = null
                )
            }
            runCurrent()

            val teardown = async { session.teardown() }
            runCurrent()

            assertTrue(teardown.isCompleted)
            handover.cancel()
            session.stop()
        }

        @Test
        fun `pickUp during a handover keeps the status the handover returns`() = runTest {
            val repository = mockk<ChatbotRepository>()
            val handover = CompletableDeferred<Result<LivechatState.Status>>()
            coEvery { repository.requestLivechatHandover(any()) } coAnswers { handover.await() }
            // The server queues the visitor when the handover request lands.
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers {
                val status = if (handover.isCompleted) {
                    LivechatState.Status.WAITING
                } else {
                    LivechatState.Status.INACTIVE
                }
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(status),
                        messages = emptyList(),
                        lastSequenceNumber = null,
                        hasMore = false,
                        syncCursor = null
                    )
                )
            }
            val session = makeSUT(repository)
            val request = launch {
                session.handover(
                    source = LivechatHandoverRequest.Source.MANUAL_BUTTON,
                    partId = null,
                    clientContext = null
                )
            }
            runCurrent()

            session.pickUp()
            handover.complete(Result.success(LivechatState.Status.WAITING))
            runCurrent()

            assertTrue(session.isInSession)
            session.stop()
            request.cancel()
        }

        @Test
        fun `cancelling a close during its read publishes nothing`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            runCurrent()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }

            val close = launch { session.close(reason = LivechatCloseReason.ENDED_BY_VISITOR) }
            runCurrent()
            close.cancel()
            runCurrent()

            assertTrue(close.isCompleted)
            assertTrue(snapshots.none { it.state.status == LivechatState.Status.CLOSED })
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `a handover without a token does not start polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery {
                repository.requestLivechatHandover(any())
            } returns Result.failure(ChatbotException.NoTokenAvailable())
            val session = makeSUT(repository)

            assertFailsWith<ChatbotException.NoTokenAvailable> {
                session.handover(
                    source = LivechatHandoverRequest.Source.MANUAL_BUTTON,
                    partId = null,
                    clientContext = null
                )
            }
            advanceTimeBy(60_000)
            runCurrent()

            coVerify(exactly = 0) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }
    }

    @Nested
    @DisplayName("locking, paging and pausing")
    inner class LockingPagingPausing {

        @Test
        fun `teardown does not wait for a parked long-poll`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }
            coEvery { repository.syncLivechat(any(), isNull(), any()) } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.WAITING),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = "cursor-1"
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            advanceTimeBy(LivechatSession.WAIT_GAP_MS + 1)
            runCurrent()

            val teardown = async { session.teardown() }
            runCurrent()

            assertTrue(teardown.isCompleted)
            session.stop()
        }

        @Test
        fun `resume returns at once while polling already runs`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }
            coEvery { repository.syncLivechat(any(), isNull(), any()) } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.WAITING),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = "cursor-1"
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            advanceTimeBy(LivechatSession.WAIT_GAP_MS + 1)
            runCurrent()

            val resume = async { session.resume() }
            runCurrent()

            assertTrue(resume.isCompleted)
            session.stop()
        }

        @Test
        fun `a page that does not advance the cursor ends the read`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.INACTIVE),
                    messages = listOf(sampleMessage(id = "m1", sequence = 1)),
                    lastSequenceNumber = 1,
                    hasMore = true,
                    syncCursor = null
                )
            )
            val session = makeSUT(repository)

            session.bootstrap()

            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }

        @Test
        fun `pause stops polling and resume reads at once`() = runTest {
            val repository = waitingRepository(messages = emptyList())
            val session = makeSUT(repository)
            session.bootstrap()

            session.pause()
            advanceTimeBy(10 * LivechatSession.WAITING_INTERVAL_MS)
            runCurrent()
            coVerify(exactly = 1) { repository.syncLivechat(any(), any(), any()) }

            session.resume()
            runCurrent()
            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
        }
    }

    @Nested
    @DisplayName("failures and cursor")
    inner class FailuresAndCursor {

        @Test
        fun `after a failed bootstrap read the log the next read finds is marked restored`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.failure(IOException("offline")),
                Result.success(waitingSync(sampleMessage(id = "m1", sequence = 1)))
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            assertEquals(LivechatBootstrapOutcome.NOT_IN_SESSION, session.bootstrap())
            advanceTimeBy(LivechatSession.WAITING_INTERVAL_MS + 1)
            runCurrent()

            val snapshot = snapshots.single()
            assertEquals(listOf("m1"), snapshot.messages.map { it.messageId })
            assertTrue(snapshot.isRestored)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `after a failed bootstrap read the session pickUp finds is marked restored`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.failure(IOException("offline")),
                Result.success(waitingSync(sampleMessage(id = "m1", sequence = 1)))
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            assertEquals(LivechatBootstrapOutcome.NOT_IN_SESSION, session.bootstrap())
            val outcome = session.pickUp()
            runCurrent()

            assertEquals(LivechatBootstrapOutcome.IN_SESSION, outcome)
            val snapshot = snapshots.single()
            assertEquals(listOf("m1"), snapshot.messages.map { it.messageId })
            assertTrue(snapshot.isRestored)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `a 401 while polling expires the session and stops polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(waitingSync()),
                Result.failure(ChatbotException.SessionExpired())
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()

            advanceTimeBy(10 * LivechatSession.WAITING_INTERVAL_MS)
            runCurrent()

            assertTrue(snapshots.last().sessionExpired)
            assertFalse(session.isInSession)
            coVerify(exactly = 2) { repository.syncLivechat(any(), any(), any()) }
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `consecutive failed reads back off exponentially`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(waitingSync()),
                Result.failure(IOException("offline"))
            )
            val delays = mutableListOf<Long>()
            val session = LivechatSession(
                repository = repository,
                scope = this,
                delayMs = { delays += it; delay(it) }
            )
            session.bootstrap()

            advanceTimeBy(2_000L + 2_000L + 4_000L + 8_000L + 1)
            runCurrent()

            assertEquals(listOf(2_000L, 2_000L, 4_000L, 8_000L, 16_000L), delays)
            session.stop()
        }

        @Test
        fun `has_more pages are read in turn and published together`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(null, any(), any()) } returns Result.success(
                waitingSync(sampleMessage(id = "m1", sequence = 1)).copy(hasMore = true)
            )
            coEvery { repository.syncLivechat(1, any(), any()) } returns Result.success(
                waitingSync(sampleMessage(id = "m2", sequence = 2))
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }

            session.bootstrap()
            runCurrent()

            assertEquals(listOf("m1", "m2"), snapshots.single().messages.map { it.messageId })
            coVerify { repository.syncLivechat(afterSequenceNumber = 1, waitMs = null, syncCursor = null) }
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `a message that failed to decode at the end of a page is not requested again`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returns Result.success(
                waitingSync(sampleMessage(id = "m1", sequence = 1)).copy(lastSequenceNumber = 2)
            )
            val session = makeSUT(repository)
            session.bootstrap()

            advanceTimeBy(LivechatSession.WAITING_INTERVAL_MS + 1)
            runCurrent()

            coVerify { repository.syncLivechat(afterSequenceNumber = 2, waitMs = null, syncCursor = null) }
            coVerify(exactly = 0) { repository.syncLivechat(afterSequenceNumber = 1, waitMs = any(), syncCursor = any()) }
            session.stop()
        }
    }

    private fun waitingSync(vararg messages: LivechatMessage) = LivechatSync(
        state = LivechatState.of(LivechatState.Status.WAITING),
        messages = messages.toList(),
        lastSequenceNumber = messages.lastOrNull()?.sequenceNumber,
        hasMore = false,
        syncCursor = null
    )

    @Nested
    @DisplayName("typing")
    inner class Typing {

        @Test
        fun `visitor typing while active posts true then false after idle`() = runTest {
            val repository = waitingRepository(
                messages = emptyList(),
                status = LivechatState.Status.ACTIVE
            )
            coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()
            coVerify(exactly = 1) { repository.sendLivechatTyping(true) }

            advanceTimeBy(LivechatTyping.IDLE_MS + 1)
            runCurrent()
            coVerify(exactly = 1) { repository.sendLivechatTyping(false) }
            session.stop()
        }

        @Test
        fun `visitor typing while waiting does not post`() = runTest {
            val repository = waitingRepository(messages = emptyList())
            coEvery { repository.sendLivechatTyping(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()

            coVerify(exactly = 0) { repository.sendLivechatTyping(any()) }
            session.stop()
        }

        @Test
        fun `a typing post that keeps failing stops after the attempt cap`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            var stoppedPosts = 0
            coEvery { repository.sendLivechatTyping(true) } returns Result.success(Unit)
            coEvery { repository.sendLivechatTyping(false) } answers {
                stoppedPosts += 1
                Result.failure(IOException("offline"))
            }
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()
            advanceTimeBy(LivechatTyping.IDLE_MS + 1)
            advanceTimeBy(60_000)
            runCurrent()
            session.stop()

            assertEquals(LivechatTyping.ATTEMPTS, stoppedPosts)
        }

        @Test
        fun `a rejected typing post is not retried`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.sendLivechatTyping(any()) } returns
                Result.failure(LivechatRequestException.Conflict())
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()
            advanceTimeBy(LivechatTyping.IDLE_MS + 1)
            advanceTimeBy(60_000)
            session.visitorTyping(isEmpty = false)
            runCurrent()
            session.stop()

            coVerify(exactly = 1) { repository.sendLivechatTyping(any()) }
        }

        @Test
        fun `pause stops retrying a failing typing post`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.sendLivechatTyping(any()) } returns Result.failure(IOException("offline"))
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()
            session.pause()
            advanceTimeBy(60_000)
            runCurrent()
            session.stop()

            coVerify(exactly = 1) { repository.sendLivechatTyping(true) }
            // One best-effort post on pause, since the first may have reached the agent.
            coVerify(exactly = 1) { repository.sendLivechatTyping(false) }
        }

        @Test
        fun `typing failures are logged once`() = runTest {
            mockkObject(ChatbotLogger)
            try {
                val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
                coEvery { repository.sendLivechatTyping(true) } returns Result.success(Unit)
                coEvery { repository.sendLivechatTyping(false) } returns Result.failure(IOException("offline"))
                val session = makeSUT(repository)
                session.bootstrap()
                runCurrent()

                repeat(2) {
                    session.visitorTyping(isEmpty = false)
                    runCurrent()
                    advanceTimeBy(LivechatTyping.IDLE_MS + 10_000)
                }
                session.stop()

                coVerify(exactly = 2 * LivechatTyping.ATTEMPTS) { repository.sendLivechatTyping(false) }
                verify(exactly = 1) { ChatbotLogger.logError(any<Throwable>()) }
            } finally {
                unmockkObject(ChatbotLogger)
            }
        }

        @Test
        fun `a 401 on a typing post ends the visitor session`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.sendLivechatTyping(any()) } returns
                Result.failure(ChatbotException.SessionExpired())
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            runCurrent()

            session.visitorTyping(isEmpty = false)
            runCurrent()

            assertTrue(snapshots.any { it.sessionExpired })
            coVerify(exactly = 1) { repository.sendLivechatTyping(any()) }
            session.stop()
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("agent typing")
    inner class AgentTyping {

        @Test
        fun `a failed poll clears the agent typing flag`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.ACTIVE).copy(isAgentTyping = true),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = null
                )
            ) andThen Result.failure(IOException("offline"))
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            runCurrent()
            assertTrue(snapshots.last().state.isAgentTyping)

            advanceTimeBy(LivechatSession.ACTIVE_INTERVAL_MS + 1)
            runCurrent()

            assertFalse(snapshots.last().state.isAgentTyping)
            assertEquals(LivechatState.Status.ACTIVE, snapshots.last().state.status)
            session.stop()
            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("freshAttachmentUrl")
    inner class FreshAttachmentUrl {

        @Test
        fun `finds the attachment without advancing the poll cursor`() = runTest {
            val file = MessageFile(
                filename = "a.pdf",
                url = "https://cdn.example/fresh.pdf",
                attachmentId = "att-9",
                mimeType = "application/pdf",
                sizeBytes = 10,
                urlExpiresAt = null
            )
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.ACTIVE),
                        messages = listOf(sampleMessage(id = "m1", sequence = 5)),
                        lastSequenceNumber = 5,
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.ACTIVE),
                        messages = listOf(
                            sampleMessage(
                                id = "m2",
                                sequence = 9,
                                parts = listOf(file)
                            )
                        ),
                        lastSequenceNumber = 9,
                        hasMore = false,
                        syncCursor = null
                    )
                )
            ) andThen Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.ACTIVE),
                    messages = emptyList(),
                    lastSequenceNumber = null,
                    hasMore = false,
                    syncCursor = null
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            val url = session.freshAttachmentUrl("att-9")

            assertEquals("https://cdn.example/fresh.pdf", url)
            // Poll after bootstrap should still use after=5 (sequence from the first message), not 9.
            advanceTimeBy(LivechatSession.ACTIVE_INTERVAL_MS + 1)
            runCurrent()
            coVerify {
                repository.syncLivechat(
                    afterSequenceNumber = 5,
                    waitMs = any(),
                    syncCursor = any()
                )
            }
            session.stop()
        }

        @Test
        fun `returns null when the log has no matching attachment`() = runTest {
            val repository = waitingRepository(
                messages = emptyList(),
                status = LivechatState.Status.ACTIVE
            )
            val session = makeSUT(repository)
            session.bootstrap()

            assertNull(session.freshAttachmentUrl("missing"))
            session.stop()
        }

        @Test
        fun `a published attachment is read from the page that starts at its message`() = runTest {
            val stale = sampleFile(url = "https://cdn.example/stale.pdf")
            val fresh = sampleFile(url = "https://cdn.example/fresh.pdf")
            val repository = waitingRepository(
                messages = listOf(
                    sampleMessage(id = "m1", sequence = 3),
                    sampleMessage(id = "m2", sequence = 7, parts = listOf(stale))
                ),
                status = LivechatState.Status.ACTIVE
            )
            coEvery {
                repository.syncLivechat(afterSequenceNumber = 6, waitMs = null, syncCursor = null)
            } returns Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.ACTIVE),
                    messages = listOf(sampleMessage(id = "m2", sequence = 7, parts = listOf(fresh))),
                    lastSequenceNumber = 7,
                    hasMore = true,
                    syncCursor = null
                )
            )
            val session = makeSUT(repository)
            session.bootstrap()
            runCurrent()

            assertEquals("https://cdn.example/fresh.pdf", session.freshAttachmentUrl("att-9"))
            coVerify(exactly = 1) {
                repository.syncLivechat(afterSequenceNumber = 6, waitMs = null, syncCursor = null)
            }
            session.stop()
        }

        @Test
        fun `a 401 ends the visitor session and returns null`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            runCurrent()
            coEvery { repository.syncLivechat(any(), any(), any()) } returns
                Result.failure(ChatbotException.SessionExpired())

            assertNull(session.freshAttachmentUrl("att-9"))
            runCurrent()

            assertTrue(snapshots.any { it.sessionExpired })
            assertFalse(session.isInSession)
            session.stop()
            collectJob.cancel()
        }
    }

    private fun TestScope.makeSUT(repository: ChatbotRepository) = LivechatSession(
        repository = repository,
        scope = this
    )

    private fun waitingRepository(
        messages: List<LivechatMessage>,
        status: LivechatState.Status = LivechatState.Status.WAITING
    ): ChatbotRepository {
        val repository = mockk<ChatbotRepository>()
        coEvery {
            repository.syncLivechat(any(), any(), any())
        } returns Result.success(
            LivechatSync(
                state = LivechatState.of(status),
                messages = messages,
                lastSequenceNumber = messages.lastOrNull()?.sequenceNumber,
                hasMore = false,
                syncCursor = null
            )
        )
        return repository
    }

    private fun sampleFile(url: String) = MessageFile(
        filename = "a.pdf",
        url = url,
        attachmentId = "att-9",
        mimeType = "application/pdf",
        sizeBytes = 10,
        urlExpiresAt = "2026-01-01T00:00:00Z"
    )

    private fun sampleMessage(
        id: String,
        sequence: Long,
        parts: List<MessagePart> = emptyList()
    ) = LivechatMessage(
        messageId = id,
        role = MessageRole.AGENT,
        agent = null,
        parts = parts,
        createdAt = "2026-01-01T00:00:00Z",
        sequenceNumber = sequence
    )
}
