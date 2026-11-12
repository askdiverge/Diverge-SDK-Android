package ai.askdiverge.domain.model.livechat

/**
 * The visitor's livechat state from `GET /api/v1/chat/livechat/sync`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatstateresponse)
 */
internal data class LivechatState(
    val status: Status,
    /** The agent shown to the visitor; `null` until one joins. */
    val activeAgent: LivechatAgent?,
    val isAgentTyping: Boolean,
    /** ISO 8601 date-time the agent joined; `null` while waiting. */
    val agentJoinedAt: String?,
    val closedBy: ClosedBy?,
    /**
     * The agent who closed the session; `null` while open, and when the visitor or the backend's
     * inactivity sweep closed it.
     */
    val closedByAgentId: String?,
    /**
     * Why the session closed; `null` while open or when none was given. The inactivity sweep sets
     * [INACTIVITY_CLOSE_REASON].
     */
    val closeReason: String?,
    val feedback: Feedback,
    /** Monotonic session version. A poll response older than one already applied is dropped. */
    val stateVersion: Int?
) {

    /** Whether the session still needs polling: queued, or talking to an agent. */
    val isInSession: Boolean get() = status.isInSession

    /**
     * Whether the backend's inactivity sweep closed the session: agent side, no agent id, reason
     * [INACTIVITY_CLOSE_REASON].
     */
    val isClosedForInactivity: Boolean
        get() = status == Status.CLOSED &&
            closedBy == ClosedBy.AGENT &&
            closedByAgentId.isNullOrEmpty() &&
            closeReason == INACTIVITY_CLOSE_REASON

    /**
     * Where the visitor's latest livechat session stands. The client polls while [WAITING] or
     * [ACTIVE]. A case is added when the backend introduces a new session stage.
     */
    enum class Status {
        INACTIVE,
        WAITING,
        ACTIVE,
        CLOSED,
        UNKNOWN;

        val isInSession: Boolean get() = this == WAITING || this == ACTIVE
    }

    /** Who ended the latest session. */
    enum class ClosedBy {
        CUSTOMER,
        AGENT,
        UNKNOWN
    }

    /** Whether the visitor can rate the latest session. */
    data class Feedback(val status: Status) {
        enum class Status {
            NOT_AVAILABLE,
            PENDING,
            SUBMITTED,
            UNKNOWN
        }
    }

    companion object {
        const val INACTIVITY_CLOSE_REASON = "inactivity"

        fun of(
            status: Status,
            feedback: Feedback.Status = Feedback.Status.NOT_AVAILABLE
        ) = LivechatState(
            status = status,
            activeAgent = null,
            isAgentTyping = false,
            agentJoinedAt = null,
            closedBy = null,
            closedByAgentId = null,
            closeReason = null,
            feedback = Feedback(status = feedback),
            stateVersion = null
        )
    }
}
