package ai.askdiverge.domain.metadata

import ai.askdiverge.domain.exception.SessionMetadataRequestException
import ai.askdiverge.domain.model.metadata.JsonValue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Sends one patch to the visitor's session. */
internal typealias SessionMetadataPatcher = suspend (SessionMetadataPatch) -> Result<Unit>

/**
 * Holds the metadata the host sets until a chat is on screen with a session, then sends it one
 * patch at a time, oldest first, as the web widget and the iOS SDK do. What waits is kept to what
 * the session needs: unsigned calls merged into one patch by the backend's own rule, and the newest
 * signed token. A patch that fails for a reason that may pass (no response, 408, 429, a 5xx) stays
 * queued, with those behind it, and goes again on the next call, the next visitor send or the next
 * attach. One the backend answers with any other status, a 401 included, is dropped.
 *
 * Lives in memory only, for as long as its holder. Every call can come from any thread and returns
 * at once: one lock guards the state, and it is never held across a send.
 */
internal class SessionMetadataQueue {

    private val lock = Any()

    // While a delivery runs, the first patch is on its way and is never changed.
    private var pending: List<SessionMetadataPatch> = emptyList()

    // The chat on screen with a session, and how to send through it.
    private var chat: Any? = null
    private var scope: CoroutineScope? = null
    private var patcher: SessionMetadataPatcher? = null
    private var delivery: Job? = null

    // Bumped whenever the chat to send through changes, so a delivery that began before it leaves
    // the queue alone.
    private var generation = 0L

    /**
     * Queues [patch] and sends it when a chat is attached. Unsigned metadata merges into the unsigned
     * patch still waiting, where the two can go as one. A signed token replaces the token still
     * waiting: the backend takes the newest token as the visitor's current claims.
     */
    fun enqueue(patch: SessionMetadataPatch) {
        synchronized(lock) {
            val sending = pending.take(if (delivery == null) 0 else 1)
            val waiting = pending.drop(sending.size).toMutableList()
            when (patch) {
                is SessionMetadataPatch.Signed -> {
                    waiting.removeAll { it is SessionMetadataPatch.Signed }
                    waiting += patch
                }
                is SessionMetadataPatch.Metadata -> {
                    val index = waiting.indexOfLast { it is SessionMetadataPatch.Metadata }
                    val merged = (waiting.getOrNull(index) as? SessionMetadataPatch.Metadata)
                        ?.values
                        ?.merging(patch.values)
                    if (merged != null) {
                        waiting[index] = SessionMetadataPatch.Metadata(merged)
                    } else {
                        waiting += patch
                    }
                }
            }
            pending = sending + waiting
            deliverLocked()
        }?.start()
    }

    /**
     * Sends through [patcher] in [scope] from now on: [chat] is on screen with a session. Also sends
     * what an earlier failure left queued. A delivery through another chat stops, and its patch goes
     * again through this one.
     */
    fun attach(
        chat: Any,
        scope: CoroutineScope,
        patcher: SessionMetadataPatcher
    ) {
        synchronized(lock) {
            if (this.chat !== chat) stopDeliveryLocked()
            this.chat = chat
            this.scope = scope
            this.patcher = patcher
            deliverLocked()
        }?.start()
    }

    /**
     * Stops sending through [chat], which left the screen, lost its session or is deleting the
     * visitor's data. What is queued, the patch on its way included, waits for the next attach. A
     * chat that is not the attached one changes nothing.
     */
    fun detach(chat: Any) {
        synchronized(lock) {
            if (this.chat !== chat) return
            forgetChatLocked()
        }
    }

    /** Drops everything queued and detaches the chat. */
    fun clear() {
        synchronized(lock) {
            pending = emptyList()
            forgetChatLocked()
        }
    }

    private fun forgetChatLocked() {
        chat = null
        scope = null
        patcher = null
        stopDeliveryLocked()
    }

    private fun stopDeliveryLocked() {
        generation += 1
        val stopped = delivery
        delivery = null
        stopped?.cancel()
    }

    // Returns the delivery to start once the lock is released, so a send never runs under it.
    private fun deliverLocked(): Job? {
        if (delivery != null || pending.isEmpty()) return null
        val scope = scope ?: return null
        val patcher = patcher ?: return null
        val generation = generation
        val job = scope.launch(start = CoroutineStart.LAZY) { drain(patcher, generation) }
        delivery = job
        // A delivery that never ran, its scope already gone, must not hold the queue.
        job.invokeOnCompletion { synchronized(lock) { if (delivery === job) delivery = null } }
        return job
    }

    private suspend fun drain(
        patcher: SessionMetadataPatcher,
        generation: Long
    ) {
        while (true) {
            val patch = synchronized(lock) {
                if (generation != this.generation) return
                pending.firstOrNull() ?: run {
                    delivery = null
                    return
                }
            }
            val done = send(patch, patcher)
            synchronized(lock) {
                if (generation != this.generation) return
                if (!done) {
                    delivery = null
                    return
                }
                pending = pending.drop(1)
            }
        }
    }

    // Whether [patch] is done with: sent, or refused for good. `false` keeps it queued.
    private suspend fun send(
        patch: SessionMetadataPatch,
        patcher: SessionMetadataPatcher
    ): Boolean {
        val result = try {
            patcher(patch)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        val error = result.exceptionOrNull() ?: return true
        return error is SessionMetadataRequestException && !error.isTransient()
    }
}

// The same request may succeed later: the server timed out, limited the rate or failed.
private fun SessionMetadataRequestException.isTransient(): Boolean =
    status == 408 || status == 429 || status >= 500

/**
 * [patch] over these values by the backend's rule: an object merges into the object under the same
 * key, and any other value replaces what is there. `null` when [patch] puts an object over a value
 * of another kind: sent in turn, the backend replaces whatever the session holds under that key,
 * and sent as one it would merge into it.
 */
private fun Map<String, JsonValue>.merging(patch: Map<String, JsonValue>): Map<String, JsonValue>? {
    val merged = LinkedHashMap(this)
    for ((key, value) in patch) {
        val current = merged[key]
        when {
            current is JsonValue.Object && value is JsonValue.Object ->
                merged[key] = JsonValue.Object(current.values.merging(value.values) ?: return null)
            current != null && value is JsonValue.Object -> return null
            else -> merged[key] = value
        }
    }
    return merged
}
