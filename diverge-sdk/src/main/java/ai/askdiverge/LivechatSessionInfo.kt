package ai.askdiverge

import ai.askdiverge.domain.trimToNull

/**
 * The visitor's livechat session as the chat last saw it: whether they are queued or talking to an
 * agent, and the agent's name.
 *
 * Delivered to the `onLivechatSessionChange` parameter of
 * [ai.askdiverge.ui.compose.ChatbotBottomSheet] once the session has been read: on each
 * presentation of the sheet, then on every change while it is on screen. After the sheet is
 * dismissed, the last value delivered describes the session as it was then; the next presentation
 * delivers the current one.
 *
 * @param status the phase of the visitor's livechat session.
 * @param agentDisplayName the connected agent's name. It is kept, trimmed, only while [status] is
 *   [Status.ACTIVE] and the name is not blank.
 */
class LivechatSessionInfo(
    val status: Status,
    agentDisplayName: String? = null
) {

    /**
     * The connected agent's name while [status] is [Status.ACTIVE] and the agent has one; `null` in
     * every other phase, and when the name is blank.
     */
    val agentDisplayName: String? =
        if (status == Status.ACTIVE) agentDisplayName.trimToNull() else null

    /**
     * The phase of the visitor's livechat session.
     *
     * [API ref](https://docs.askdiverge.ai/api#model/livechatstatus) — host-facing subset; unknown
     * wire values surface as [INACTIVE].
     */
    enum class Status {
        /**
         * No livechat session is open, or the session is in a phase this SDK version does not
         * know.
         */
        INACTIVE,

        /** The visitor is queued for an agent. */
        WAITING,

        /** An agent is talking to the visitor. */
        ACTIVE,

        /** The session has ended. */
        CLOSED
    }

    override fun equals(other: Any?): Boolean =
        other is LivechatSessionInfo &&
            status == other.status &&
            agentDisplayName == other.agentDisplayName

    override fun hashCode(): Int = 31 * status.hashCode() + agentDisplayName.hashCode()

    override fun toString(): String =
        "LivechatSessionInfo(status=$status, agentDisplayName=$agentDisplayName)"
}
