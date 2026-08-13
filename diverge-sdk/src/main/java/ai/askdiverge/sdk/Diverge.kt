package ai.askdiverge.sdk

import java.util.concurrent.atomic.AtomicReference

/**
 * Diverge ecommerce SDK namespace.
 *
 * Call [configure] once at app launch before using [shared].
 */
object Diverge {
    /** Semantic version of this SDK build (from the repo `VERSION` file). */
    val VERSION: String
        get() = BuildConfig.SDK_VERSION

    private val sharedClient = AtomicReference<DivergeClient?>(null)

    /** Whether [configure] has been called successfully. */
    val isConfigured: Boolean
        get() = sharedClient.get() != null

    /**
     * The shared client after a successful [configure].
     * @throws DivergeException.NotConfigured if configure was never called.
     */
    val shared: DivergeClient
        get() = sharedClient.get() ?: throw DivergeException.NotConfigured()

    /**
     * Configures the SDK and replaces any previous shared client.
     * @throws DivergeException.InvalidApiKey if the key is blank.
     */
    @JvmStatic
    fun configure(configuration: Configuration): DivergeClient {
        val trimmed = configuration.apiKey.trim()
        if (trimmed.isEmpty()) {
            throw DivergeException.InvalidApiKey()
        }
        val normalized = configuration.copy(apiKey = trimmed)
        val client = DivergeClient(normalized)
        sharedClient.set(client)
        return client
    }

    /**
     * Resets the shared client.
     *
     * Intended for unit tests. Host apps should not call this in production.
     */
    @JvmStatic
    @JvmName("resetForTesting")
    fun reset() {
        sharedClient.set(null)
    }
}
