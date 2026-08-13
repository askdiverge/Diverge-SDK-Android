package ai.askdiverge.sdk

/**
 * Configuration used to initialize the Diverge SDK.
 */
data class Configuration(
    val apiKey: String,
    val environment: Environment,
) {
    override fun toString(): String =
        "Configuration(apiKey=${redact(apiKey)}, environment=$environment)"

    companion object {
        /** Redacts an API key for logs (keeps a short prefix/suffix when long enough). */
        @JvmStatic
        fun redact(apiKey: String): String {
            val trimmed = apiKey.trim()
            if (trimmed.length <= 8) return "***"
            return "${trimmed.take(4)}…${trimmed.takeLast(4)}"
        }
    }
}
