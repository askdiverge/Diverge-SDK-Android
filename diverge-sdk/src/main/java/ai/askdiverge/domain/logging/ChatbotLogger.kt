package ai.askdiverge.domain.logging

import androidx.annotation.RestrictTo

/**
 * Receives the chatbot's error logs. A host app implements it and passes it to [ChatbotLogger.install];
 * nothing is logged until one is installed.
 */
interface ChatbotLogger {

    fun logError(message: String)

    fun logError(throwable: Throwable)

    companion object : ChatbotLogger {

        @Volatile
        private var logger: ChatbotLogger? = null

        fun install(logger: ChatbotLogger) {
            this.logger = logger
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun logError(message: String) {
            logger?.logError(message)
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        override fun logError(throwable: Throwable) {
            logger?.logError(throwable)
        }
    }
}