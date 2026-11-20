package ai.askdiverge.domain.livechat

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.logging.ChatbotLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Tells the connected agent whether the visitor is typing, through [post].
 *
 * A burst of draft changes posts `true` once, and `false` after [IDLE_MS] without a change or when
 * the draft empties. Only the latest wanted value is posted. A failed post is retried [RETRY_MS]
 * apart, at most [ATTEMPTS] times per value. A rejection (401, 409, no token) is not retried, and
 * nothing more is posted until [reset]. The first failure that stops posting is logged; later ones
 * are not.
 *
 * Every function is safe from any thread.
 */
internal class LivechatTyping(
    private val scope: CoroutineScope,
    private val delayMs: suspend (Long) -> Unit,
    /** Whether an agent is connected to a valid visitor session, so posting makes sense. */
    private val canPost: () -> Boolean,
    private val post: suspend (isTyping: Boolean) -> Result<Unit>
) {

    private val lock = Any()

    // Everything below is guarded by lock.
    private var wanted = false
    private var sent = false
    private var rejected = false
    private var hasLogged = false

    // Bumped whenever posting state is discarded, so an earlier loop's late result is ignored.
    private var epoch = 0
    private var idleJob: Job? = null
    private var postJob: Job? = null

    /** The visitor's draft changed; [isEmpty] when it has no text left. */
    fun onDraftChanged(isEmpty: Boolean) {
        synchronized(lock) {
            idleJob?.cancel()
            if (isEmpty || !canPost()) {
                requestLocked(false)
                return
            }
            requestLocked(true)
            idleJob = scope.launch {
                delayMs(IDLE_MS)
                end()
            }
        }
    }

    /** Tells the agent the visitor stopped typing, if they were last told otherwise. */
    fun end() {
        synchronized(lock) {
            idleJob?.cancel()
            idleJob = null
            requestLocked(false)
        }
    }

    /**
     * The chat left the screen: stops retrying and, when the agent may have been told the visitor
     * is typing, tells them once that they stopped.
     */
    fun pause() {
        synchronized(lock) {
            val mayBeTyping = sent || wanted
            discardLocked()
            if (mayBeTyping && !rejected && canPost()) {
                // One attempt, whatever it returns.
                scope.launch { post(false) }
            }
        }
    }

    /** Forgets typing without posting: the session it belonged to is no longer active. */
    fun reset() {
        synchronized(lock) {
            discardLocked()
            rejected = false
            hasLogged = false
        }
    }

    private fun discardLocked() {
        idleJob?.cancel()
        idleJob = null
        postJob?.cancel()
        postJob = null
        wanted = false
        sent = false
        epoch += 1
    }

    private fun requestLocked(isTyping: Boolean) {
        wanted = isTyping
        if (rejected || wanted == sent || postJob?.isActive == true || !canPost()) return
        val token = epoch
        postJob = scope.launch { sendLoop(token) }
    }

    private suspend fun sendLoop(token: Int) {
        var failures = 0
        while (true) {
            val value = synchronized(lock) {
                if (token != epoch) return
                if (rejected || wanted == sent || !canPost()) {
                    postJob = null
                    return
                }
                wanted
            }
            val error = post(value).exceptionOrNull()
            var toLog: Throwable? = null
            val retry = synchronized(lock) {
                if (token != epoch) return
                when {
                    error == null -> {
                        sent = value
                        failures = 0
                        false
                    }

                    error.isRejection() -> {
                        rejected = true
                        postJob = null
                        toLog = error.takeUnless { hasLogged }
                        hasLogged = true
                        null
                    }

                    else -> {
                        failures += 1
                        if (failures < ATTEMPTS) {
                            true
                        } else {
                            // Give this value up; a different wanted value still gets its attempts.
                            sent = value
                            failures = 0
                            toLog = error.takeUnless { hasLogged }
                            hasLogged = true
                            false
                        }
                    }
                }
            }
            toLog?.let(ChatbotLogger::logError)
            when (retry) {
                null -> return
                true -> delayMs(RETRY_MS)
                false -> Unit
            }
        }
    }

    private fun Throwable.isRejection(): Boolean =
        this is ChatbotException.SessionExpired ||
            this is ChatbotException.NoTokenAvailable ||
            this is LivechatRequestException

    companion object {
        const val IDLE_MS = 1_500L
        const val RETRY_MS = 1_000L
        const val ATTEMPTS = 3
    }
}
