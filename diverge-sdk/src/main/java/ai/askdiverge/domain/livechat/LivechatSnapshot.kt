package ai.askdiverge.domain.livechat

import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState

/**
 * A livechat change published by [LivechatSession].
 *
 * [messages] holds only the messages after the previous snapshot's cursor.
 */
internal data class LivechatSnapshot(
    val previousStatus: LivechatState.Status,
    val state: LivechatState,
    val messages: List<LivechatMessage>,
    /** The visitor session ended (401); polling has stopped. */
    val sessionExpired: Boolean,
    /**
     * The session's generation when this was published; see [LivechatSession.teardown].
     * Snapshots of an older generation belong to the session being replaced.
     */
    val generation: Long,
    /**
     * The first state the session published, found already in place rather than seen changing.
     */
    val isRestored: Boolean
)
