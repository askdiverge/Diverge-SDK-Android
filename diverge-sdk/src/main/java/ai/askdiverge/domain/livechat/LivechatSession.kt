package ai.askdiverge.domain.livechat

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Polls the visitor's livechat state, and the message log while a session is open, and publishes
 * every change as a [LivechatSnapshot].
 *
 * Each tick reads `GET /livechat/sync`. When the server returns a `sync_cursor`, the next tick
 * waits on it for up to 20 s; otherwise ticks run every 2 s while queued and every 3 s with an
 * agent. Consecutive failures back off exponentially up to 30 s. Polling stops when a read shows
 * no open session, on a 401, and on [teardown] / [stop].
 */
internal class LivechatSession(
    private val repository: ChatbotRepository,
    private val scope: CoroutineScope,
    private val delayMs: suspend (Long) -> Unit = { delay(it) }
) {

    // Unbounded: a snapshot carries the messages after the cursor, and the cursor has already
    // moved past them, so dropping an undelivered snapshot would lose those messages.
    private val snapshotChannel = Channel<LivechatSnapshot>(Channel.UNLIMITED)
    val snapshots: Flow<LivechatSnapshot> = snapshotChannel.receiveAsFlow()

    private val mutex = Mutex()
    private var pollJob: Job? = null

    private var afterSequence: Long = 0
    private var consecutiveFailures = 0
    private var lastStatus: LivechatState.Status = LivechatState.Status.INACTIVE
    private var syncCursor: String? = null
    private var lastPublishedState: LivechatState? = null
    private var lastStateVersion: Int? = null
    private var sessionExpired = false
    private val generation = AtomicLong(0)
    private var hasPublishedState = false

    val isInSession: Boolean get() = lastStatus.isInSession

    /**
     * Reads the current state once. An open session replays its log from the start and starts
     * polling.
     */
    suspend fun bootstrap(): LivechatBootstrapOutcome = mutex.withLock {
        val gen = bumpGenerationLocked()
        sessionExpired = false
        return try {
            val sync = syncLocked(after = 0, waiting = false)
            if (gen != generation.get()) return currentOutcomeLocked()
            consecutiveFailures = 0
            syncCursor = sync.syncCursor
            if (!sync.state.isInSession) {
                publishTransitionLocked(
                    state = sync.state,
                    messages = if (sync.state.status == LivechatState.Status.CLOSED) {
                        sync.messages
                    } else {
                        emptyList()
                    },
                    generation = gen
                )
                LivechatBootstrapOutcome.NOT_IN_SESSION
            } else {
                afterSequence = 0
                publishTransitionLocked(sync.state, sync.messages, generation = gen)
                startPollingLocked(immediate = false)
                LivechatBootstrapOutcome.IN_SESSION
            }
        } catch (_: ChatbotException.SessionExpired) {
            expireLocked(gen)
            LivechatBootstrapOutcome.SESSION_EXPIRED
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (gen == generation.get()) {
                startPollingLocked(immediate = false)
            }
            currentOutcomeLocked()
        }
    }

    /** Stops polling, rewinds the cursor and publishes `inactive`. */
    suspend fun teardown(): Long = mutex.withLock {
        val gen = bumpGenerationLocked()
        sessionExpired = false
        afterSequence = 0
        consecutiveFailures = 0
        stopPollingLocked()
        publishTransitionLocked(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            generation = gen
        )
        gen
    }

    /** Cancels the poll loop. Safe from any thread. */
    fun stop() {
        pollJob?.cancel()
        pollJob = null
        snapshotChannel.close()
    }

    private fun currentOutcomeLocked(): LivechatBootstrapOutcome =
        if (lastStatus.isInSession) {
            LivechatBootstrapOutcome.IN_SESSION
        } else {
            LivechatBootstrapOutcome.NOT_IN_SESSION
        }

    private fun bumpGenerationLocked(): Long {
        val next = generation.incrementAndGet()
        lastStateVersion = null
        syncCursor = null
        return next
    }

    private fun startPollingLocked(immediate: Boolean) {
        stopPollingLocked()
        consecutiveFailures = 0
        pollJob = scope.launch {
            var tickNow = immediate
            while (isActive) {
                if (!tickNow) {
                    val delay = mutex.withLock { nextDelayLocked() }
                    delayMs(delay)
                }
                tickNow = false
                val keepGoing = mutex.withLock {
                    if (sessionExpired) return@withLock false
                    tickLocked(waiting = true)
                    !sessionExpired
                }
                if (!keepGoing) return@launch
            }
        }
    }

    private fun stopPollingLocked() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun tickLocked(
        waiting: Boolean,
        confirming: Boolean = false,
        recovering: Boolean = false
    ) {
        val gen = generation.get()
        val previous = lastStatus
        try {
            val after = if (previous.isInSession) afterSequence else 0L
            val sync = syncLocked(after = after, waiting = waiting)
            if (gen != generation.get()) return
            consecutiveFailures = 0
            syncCursor = sync.syncCursor
            if (isStaleLocked(sync.state)) {
                if (!(previous.isInSession && sync.state.isInSession)) return
                if (confirming) {
                    lastStateVersion = null
                    afterSequence = 0
                    syncCursor = null
                    tickLocked(waiting = false)
                } else {
                    tickLocked(waiting = false, confirming = true)
                }
                return
            }

            if (!previous.isInSession && sync.state.isInSession) {
                afterSequence = 0
            }
            val closedNow =
                (previous.isInSession || recovering) && sync.state.status == LivechatState.Status.CLOSED
            val messages =
                if (sync.state.isInSession || closedNow) sync.messages else emptyList()
            publishTransitionLocked(sync.state, messages, generation = gen)
            if (!sync.state.isInSession) {
                stopPollingLocked()
            }
        } catch (_: ChatbotException.SessionExpired) {
            expireLocked(gen)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (gen != generation.get()) return
            syncCursor = null
            consecutiveFailures += 1
        }
    }

    private suspend fun syncLocked(
        after: Long,
        waiting: Boolean
    ): LivechatSync {
        val messages = mutableListOf<LivechatMessage>()
        var cursor = after
        var waitOn = if (waiting) syncCursor else null
        while (true) {
            val sync = repository.syncLivechat(
                afterSequenceNumber = cursor.takeIf { it > 0 },
                waitMs = if (waitOn == null) null else MAX_WAIT_MS,
                syncCursor = waitOn
            ).getOrThrow()
            messages += sync.messages
            val last = sync.messages.lastOrNull()
            if (!sync.hasMore || last == null) {
                return LivechatSync(
                    state = sync.state,
                    messages = messages,
                    hasMore = false,
                    syncCursor = sync.syncCursor
                )
            }
            cursor = last.sequenceNumber
            waitOn = null
        }
    }

    private fun nextDelayLocked(): Long {
        if (consecutiveFailures > 0) {
            // 2, 4, 8, 16, then 30 s.
            return minOf(30_000L, (1L shl minOf(consecutiveFailures, 5)) * 1_000L)
        }
        if (syncCursor != null) return WAIT_GAP_MS
        return if (lastStatus == LivechatState.Status.ACTIVE) ACTIVE_INTERVAL_MS else WAITING_INTERVAL_MS
    }

    private fun isStaleLocked(state: LivechatState): Boolean {
        val incoming = state.stateVersion ?: return false
        val last = lastStateVersion ?: return false
        return incoming < last
    }

    private fun publishTransitionLocked(
        state: LivechatState,
        messages: List<LivechatMessage>,
        expired: Boolean = false,
        generation: Long? = null
    ) {
        if (generation != null && generation != this.generation.get()) return
        if (isStaleLocked(state)) return
        state.stateVersion?.let { lastStateVersion = it }
        messages.lastOrNull()?.let { afterSequence = it.sequenceNumber }
        if (messages.isEmpty() && !expired && state == lastPublishedState) return
        lastPublishedState = state
        val previous = lastStatus
        lastStatus = state.status
        val isRestored = !hasPublishedState
        hasPublishedState = true
        snapshotChannel.trySend(
            LivechatSnapshot(
                previousStatus = previous,
                state = state,
                messages = messages,
                sessionExpired = expired,
                generation = this.generation.get(),
                isRestored = isRestored
            )
        )
    }

    private fun expireLocked(generation: Long) {
        if (generation != this.generation.get()) return
        sessionExpired = true
        publishTransitionLocked(
            state = LivechatState.of(LivechatState.Status.INACTIVE),
            messages = emptyList(),
            expired = true,
            generation = generation
        )
        stopPollingLocked()
    }

    companion object {
        const val WAITING_INTERVAL_MS = 2_000L
        const val ACTIVE_INTERVAL_MS = 3_000L
        const val MAX_WAIT_MS = 20_000
        const val WAIT_GAP_MS = 250L
    }
}
