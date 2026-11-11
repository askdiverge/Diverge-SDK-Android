package ai.askdiverge.domain.livechat

/**
 * A session-local divider line the SDK mints on livechat transitions. It is not wire-derived; the
 * conversation shows it between turns so the visitor can see when they joined the queue, when an
 * agent joined, and when the session ended.
 */
internal data class LivechatNote(val kind: Kind) {

    sealed class Kind {
        /** The visitor entered the queue. */
        data object Queued : Kind()

        /** An agent joined; carries the display name for copy. */
        data class AgentJoined(val displayName: String) : Kind()

        /** The livechat session ended. */
        data object Ended : Kind()

        /** The backend closed the session because the visitor stopped replying. */
        data object EndedForInactivity : Kind()
    }
}
