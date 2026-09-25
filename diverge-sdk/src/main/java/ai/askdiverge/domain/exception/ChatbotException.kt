package ai.askdiverge.domain.exception

/**
 * A reason the chat cannot reach Diverge with the session the host supplies through
 * [ChatbotCallbacks][ai.askdiverge.ChatbotCallbacks].
 *
 * The chat raises and handles these itself; host code never receives one and has nothing to catch.
 * For every subtype the chat shows its retry screen, and a retry discards the token the chat holds
 * and asks [ChatbotCallbacks.fetchToken][ai.askdiverge.ChatbotCallbacks.fetchToken] for a new one.
 * The message is what the chat writes to logcat under the tag `Diverge`.
 */
sealed class ChatbotException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    /**
     * Diverge no longer accepts the session token: it answered HTTP 401 while the chat was sending a
     * message.
     *
     * The retry asks [ChatbotCallbacks.fetchToken][ai.askdiverge.ChatbotCallbacks.fetchToken] for a
     * token again, so it must return one Diverge accepts, not the token it just rejected.
     *
     * @param cause the failure behind the rejection, or `null` when the 401 is all the chat has.
     */
    class SessionExpired(cause: Throwable? = null) :
        ChatbotException(
            message = "Session expired",
            cause = cause
        )

    /**
     * The chat was about to send an authenticated request to Diverge while it held no session token:
     * before [ChatbotCallbacks.fetchToken][ai.askdiverge.ChatbotCallbacks.fetchToken] first
     * succeeds, or after the chat discards its token for a retry or a data deletion and before
     * `fetchToken` succeeds again.
     *
     * The token the host returns plays no part in it, so there is nothing for the host to change;
     * report it to Diverge with the log.
     */
    class NoTokenAvailable :
        ChatbotException(
            message = "No token available — initialize() has not completed yet"
        )
}
