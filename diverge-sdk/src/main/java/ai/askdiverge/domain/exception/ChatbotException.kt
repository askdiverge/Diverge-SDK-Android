package ai.askdiverge.domain.exception

import java.io.IOException

sealed class ChatbotException(
    message: String? = null,
    cause: Throwable? = null
) : IOException(message, cause) {
    class SessionExpired(cause: Throwable? = null) :
        ChatbotException(
            message = "Session expired",
            cause = cause
        )

    class NoTokenAvailable :
        ChatbotException(
            message = "No token available — initialize() has not completed yet"
        )
}