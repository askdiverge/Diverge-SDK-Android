package ai.askdiverge.domain.metadata

import ai.askdiverge.domain.model.metadata.JsonValue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Stands in for `PATCH /chat/session`. Attempt `n` (from 0) fails with `failures[n]`, a
 * [CancellationException] thrown rather than returned; every other attempt succeeds. While [gate]
 * is not complete, every attempt waits on it; with [ignoresCancellation], a cancelled attempt
 * keeps waiting and finishes as scripted, as a request already on the wire can.
 */
internal class ScriptedPatcher(
    private val failures: Map<Int, Throwable> = emptyMap(),
    private val gate: CompletableDeferred<Unit>? = null,
    private val ignoresCancellation: Boolean = false
) {

    private val attemptCount = AtomicInteger(0)

    /** Every patch tried, in order, those still waiting on the gate included. */
    val tried = CopyOnWriteArrayList<SessionMetadataPatch>()

    /** The patches that were applied, in order. */
    val sent = CopyOnWriteArrayList<SessionMetadataPatch>()

    val attempts: Int get() = attemptCount.get()

    suspend fun patch(patch: SessionMetadataPatch): Result<Unit> {
        val attempt = attemptCount.getAndIncrement()
        tried += patch
        if (ignoresCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
        val failure = failures[attempt]
        if (failure is CancellationException) throw failure
        if (failure != null) return Result.failure(failure)
        sent += patch
        return Result.success(Unit)
    }
}

internal fun screen(name: String): SessionMetadataPatch =
    SessionMetadataPatch.Metadata(mapOf("screen" to JsonValue.Str(name)))

internal fun signed(token: String): SessionMetadataPatch = SessionMetadataPatch.Signed(token)
