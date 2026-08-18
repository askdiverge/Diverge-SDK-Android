package ai.askdiverge.sdk

/**
 * Configured SDK session. Obtain via [Diverge.configure] or [Diverge.shared].
 */
class DivergeClient internal constructor(
    val configuration: DivergeConfiguration,
) {
    val version: String
        get() = Diverge.VERSION

    val apiBaseUrl: String
        get() = configuration.environment.apiBaseUrl
}
