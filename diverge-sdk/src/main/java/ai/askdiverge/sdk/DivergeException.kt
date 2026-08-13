package ai.askdiverge.sdk

/**
 * Errors thrown while configuring or using the Diverge SDK.
 */
sealed class DivergeException(message: String) : Exception(message) {
    class InvalidApiKey : DivergeException("API key must not be blank.")

    class NotConfigured : DivergeException("Call Diverge.configure before using Diverge.shared.")
}
