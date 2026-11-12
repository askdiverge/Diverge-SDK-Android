package ai.askdiverge.domain.exception

/**
 * Failures from visitor livechat endpoints that the UI branches on.
 */
internal sealed class LivechatRequestException(message: String) : Exception(message) {

    /** A 409: livechat unavailable, or no open session to close. */
    class Conflict : LivechatRequestException(message = "Livechat conflicts with server state")
}
