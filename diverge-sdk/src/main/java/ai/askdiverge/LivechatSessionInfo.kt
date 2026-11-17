package ai.askdiverge

/**
 * The visitor's livechat session as the chat last saw it: whether they are queued or talking to an
 * agent, and the agent's name.
 *
 * Delivered through [ChatbotCallbacks.onLivechatSessionChange] while the chat is on screen. After
 * the sheet is dismissed, the last value delivered describes the session as it was then; the next
 * presentation delivers the current one.
 *
 * @param status the phase of the visitor's livechat session. New values may be added in minor
 *   releases, so a `when` over it needs an `else` branch.
 * @param agentDisplayName the connected agent's name while [status] is [Status.ACTIVE] and the
 *   agent has one; always `null` in every other phase, and when the name is blank.
 */
data class LivechatSessionInfo(
    val status: Status,
    val agentDisplayName: String? = null
) {

    /**
     * The phase of the visitor's livechat session.
     *
     * [API ref](https://docs.askdiverge.ai/api#model/livechatstatus) — host-facing subset; unknown
     * wire values surface as [INACTIVE].
     */
    enum class Status {
        /** No livechat session is open. */
        INACTIVE,

        /** The visitor is queued for an agent. */
        WAITING,

        /** An agent is talking to the visitor. */
        ACTIVE,

        /** The session has ended. */
        CLOSED
    }

    companion object {
        /**
         * Builds the host view of a session the poller reports as [status], with [agentName] while
         * active. Blank names and non-active phases drop the name.
         */
        fun of(
            status: Status,
            agentName: String? = null
        ): LivechatSessionInfo {
            val trimmed = agentName?.trim()?.takeIf { it.isNotEmpty() }
            return LivechatSessionInfo(
                status = status,
                agentDisplayName = if (status == Status.ACTIVE) trimmed else null
            )
        }
    }
}
