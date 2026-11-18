package ai.askdiverge.domain.model.livechat

/**
 * Why the visitor's side closes a livechat session on `POST /livechat/close`. The agent sees the
 * reason beside the closed session. A case is added when the SDK gains another way to close one.
 */
internal enum class LivechatCloseReason {
    /** The visitor ended the session from the header control. */
    ENDED_BY_VISITOR,

    /** The visitor reset the chat while a session was open. */
    RESET
}
