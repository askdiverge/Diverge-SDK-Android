package ai.askdiverge.domain.livechat

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.model.livechat.LivechatClientContext
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import ai.askdiverge.domain.model.message.incoming.part.MessagePart
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.mockk
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
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
                        hasMore = false,
                        syncCursor = "cursor"
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.WAITING),
                        messages = listOf(sampleMessage(id = "m2", sequence = 6)),
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
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.INACTIVE),
                        messages = emptyList(),
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
        fun `refresh after a close reads after the last message without waiting`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.ACTIVE),
                        messages = listOf(sampleMessage(id = "m1", sequence = 3)),
                        hasMore = false,
                        syncCursor = "c1"
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(
                            status = LivechatState.Status.CLOSED,
                            feedback = LivechatState.Feedback.Status.PENDING
                        ),
                        messages = listOf(sampleMessage(id = "m2", sequence = 4)),
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(
                            status = LivechatState.Status.CLOSED,
                            feedback = LivechatState.Feedback.Status.SUBMITTED
                        ),
                        messages = emptyList(),
                        hasMore = false,
                        syncCursor = null
                    )
                )
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            session.close(reason = null)
            runCurrent()

            session.refresh()
            runCurrent()

            coVerifySequence {
                repository.syncLivechat(null, null, null)
                repository.closeLivechat(null)
                repository.syncLivechat(3, null, null)
                repository.syncLivechat(4, null, null)
            }
            val last = snapshots.last()
            assertEquals(LivechatState.Status.CLOSED, last.previousStatus)
            assertEquals(LivechatState.Feedback.Status.SUBMITTED, last.state.feedback.status)
            session.stop()
            collectJob.cancel()
        }

        @Test
        fun `teardown does not wait for a refresh in flight`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }
            val session = makeSUT(repository)
            val refresh = launch { session.refresh() }
            runCurrent()

            val teardown = async { session.teardown() }
            runCurrent()

            assertTrue(teardown.isCompleted)
            refresh.cancel()
            session.stop()
        }

        @Test
        fun `refresh hands a session opened since the close to polling`() = runTest {
            val repository = mockk<ChatbotRepository>()
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            val reopened = LivechatState.of(LivechatState.Status.WAITING).copy(stateVersion = 1)
            coEvery { repository.syncLivechat(any(), any(), any()) } returnsMany listOf(
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(LivechatState.Status.ACTIVE).copy(stateVersion = 4),
                        messages = listOf(sampleMessage(id = "m1", sequence = 3)),
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(
                        state = LivechatState.of(
                            status = LivechatState.Status.CLOSED,
                            feedback = LivechatState.Feedback.Status.PENDING
                        ).copy(stateVersion = 5),
                        messages = emptyList(),
                        hasMore = false,
                        syncCursor = null
                    )
                ),
                Result.success(
                    LivechatSync(state = reopened, messages = emptyList(), hasMore = false, syncCursor = null)
                ),
                Result.success(
                    LivechatSync(
                        state = reopened,
                        messages = listOf(sampleMessage(id = "n1", sequence = 1)),
                        hasMore = false,
                        syncCursor = null
                    )
                )
            )
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            session.close(reason = null)
            runCurrent()

            session.refresh()
            runCurrent()

            assertTrue(
                snapshots.any {
                    it.previousStatus == LivechatState.Status.CLOSED &&
                        it.state.status == LivechatState.Status.WAITING
                }
            )
            assertTrue(snapshots.any { snapshot -> snapshot.messages.any { it.messageId == "n1" } })
            assertTrue(session.isInSession)
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
                hasMore = false,
                syncCursor = null
            )
            val closed = LivechatSync(
                state = LivechatState.of(LivechatState.Status.CLOSED),
                messages = emptyList(),
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

            session.close(reason = "Switched back to AI chatbot mode")
            runCurrent()

            assertFalse(session.isInSession)
            coVerify { repository.closeLivechat("Switched back to AI chatbot mode") }
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

            assertFailsWith<IOException> { session.close(reason = "Switched back to AI chatbot mode") }
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

            session.close(reason = "Switched back to AI chatbot mode")
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
        fun `cancelling a close during its read publishes nothing`() = runTest {
            val repository = waitingRepository(messages = emptyList(), status = LivechatState.Status.ACTIVE)
            coEvery { repository.closeLivechat(any()) } returns Result.success(Unit)
            val session = makeSUT(repository)
            val snapshots = mutableListOf<LivechatSnapshot>()
            val collectJob = launch { session.snapshots.collect { snapshots += it } }
            session.bootstrap()
            runCurrent()
            coEvery { repository.syncLivechat(any(), any(), any()) } coAnswers { awaitCancellation() }

            val close = launch { session.close(reason = "Switched back to AI chatbot mode") }
            runCurrent()
            close.cancel()
            runCurrent()

            assertTrue(close.isCancelled)
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

            advanceTimeBy(LivechatSession.TYPING_IDLE_MS + 1)
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
                        hasMore = false,
                        syncCursor = null
                    )
                )
            ) andThen Result.success(
                LivechatSync(
                    state = LivechatState.of(LivechatState.Status.ACTIVE),
                    messages = emptyList(),
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
                hasMore = false,
                syncCursor = null
            )
        )
        return repository
    }

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
