package ai.askdiverge

import ai.askdiverge.domain.logging.ChatbotLogger
import ai.askdiverge.domain.metadata.SessionMetadataQueue
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch
import ai.askdiverge.domain.model.metadata.toJsonValuesOrNull

/**
 * Metadata the host attaches to the visitor's chat session, the same way the web widget's
 * `setMetadata` and `setSignedMetadata` do.
 *
 * Create one when the app starts, keep it for as long as the app runs, next to your
 * [ChatbotCallbacks], and pass it to every [ai.askdiverge.ui.compose.ChatbotBottomSheet] as
 * `sessionMetadata`. Calls are sent in order while a chat given this instance is on screen with a
 * session. Those made while none is wait in memory, merged into one patch, for the next chat
 * shown. Nothing is written to disk, so what waits is lost when the process ends.
 *
 * Every method returns at once, can be called from any thread at any time, before a chat is shown
 * too, and never throws.
 */
class ChatbotSessionMetadata {

    internal val queue = SessionMetadataQueue()

    /**
     * Merges [metadata] into the visitor's chat session, where the backend, handover webhooks and
     * Universal API variables read it as `session.metadata`. Repeated calls deep-merge: nested
     * maps merge, any other value replaces the earlier one, `null` included, and a key stays once
     * set. Lists replace the earlier list as a whole. The merged metadata is limited to 32 KiB: the
     * backend refuses a patch over it, alone or merged with what the session already holds, and the
     * patch is dropped with every call merged into it. Metadata comes from the device, so systems
     * that read it treat it as untrusted; put what must be trusted in [setSignedMetadata].
     *
     * A reset of the conversation starts a new session without the metadata sent to the old one.
     * Call again with what the new session needs, from your [ChatbotCallbacks.createNewToken] or
     * later: calls made during a reset wait and go to the new session.
     *
     * @param metadata values to merge, such as the store, language or screen the visitor is on:
     * `String`, `Boolean`, `Int`, `Long`, `Float`, `Double`, `null`, and `List` or `Map<String, *>`
     * of those, nested at most 20 levels deep, the map's own values being the first. Keys are sent
     * as written. They use neither a reserved top-level key (`verified_metadata`,
     * `signed_metadata`) nor the key `__proto__`, `prototype` or `constructor` at any depth, and
     * numbers are finite, as the backend requires of every patch. An empty map does nothing. A
     * call outside those rules is dropped whole, alone, and logs an error through [ChatbotLogger]
     * that names no value. The backend reads numbers as doubles, so a `Long` beyond
     * ±9,007,199,254,740,992 (2^53) arrives rounded: send such an id as a `String`.
     */
    fun setMetadata(metadata: Map<String, Any?>) {
        if (metadata.isEmpty()) return
        // A map the host changes from another thread while it is read is dropped like a bad value.
        val values = try {
            metadata.toJsonValuesOrNull()
        } catch (_: RuntimeException) {
            null
        }
        if (values == null) {
            ChatbotLogger.logError(UNSUPPORTED_METADATA_MESSAGE)
            return
        }
        queue.enqueue(SessionMetadataPatch.Metadata(values))
    }

    /**
     * Sends a token the host's backend signed, so the backend can verify it against the chatbot's
     * public key and merge its claims into the session as `session.verified_metadata`. Claims the
     * chatbot names secret are kept apart for the actions that use them. The SDK never decodes the
     * token.
     *
     * A token is sent while a chat given this instance is on screen with a session. While none is,
     * the newest token waits for the next chat shown, and replaces one set before it. A token the
     * backend refuses (a bad signature, expired by the time it arrives, too long-lived, or a chatbot
     * with no public key) is dropped; sign a new one and call again. The verified claims are used
     * only while the newest token is unexpired, so sign one when the visitor signs in, again as you
     * present the chat, and on a timer shorter than the token's lifetime while the chat is open.
     *
     * @param token an RS256 JWT with `iat` and `exp` at most 15 minutes apart. It is trimmed, and a
     * blank one does nothing.
     */
    fun setSignedMetadata(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        queue.enqueue(SessionMetadataPatch.Signed(trimmed))
    }

    /**
     * Drops every call still waiting to be sent, and stops the patch on its way. Call it when the
     * visitor signs out, before your [ChatbotCallbacks.sessionInvalidations] emits, so nothing set
     * for that visitor reaches the next visitor's session. Calls made after it are sent once the
     * chat has reconnected, is shown again, or the visitor sends a message. Metadata the backend has
     * already applied stays with the old session.
     */
    fun clear() {
        queue.clear()
    }
}

private const val UNSUPPORTED_METADATA_MESSAGE =
    "setMetadata ignored: every value must be a String, Boolean, Int, Long, finite Float or " +
        "Double, null, or a List or Map<String, *> of those, at most 20 levels deep, with no " +
        "top-level key verified_metadata or signed_metadata and no key __proto__, prototype or " +
        "constructor"
