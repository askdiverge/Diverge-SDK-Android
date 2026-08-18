package ai.askdiverge.sdk

/**
 * Configuration used to initialize the Diverge SDK.
 *
 * Named `DivergeConfiguration` to avoid colliding with
 * `android.content.res.Configuration`.
 */
class DivergeConfiguration(
    val apiKey: String,
    val environment: DivergeEnvironment,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DivergeConfiguration) return false
        return apiKey == other.apiKey && environment == other.environment
    }

    override fun hashCode(): Int {
        var result = apiKey.hashCode()
        result = 31 * result + environment.hashCode()
        return result
    }

    override fun toString(): String =
        "DivergeConfiguration(apiKey=${redact(apiKey)}, environment=$environment)"

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
