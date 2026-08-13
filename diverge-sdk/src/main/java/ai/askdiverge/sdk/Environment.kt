package ai.askdiverge.sdk

/**
 * Deployment environment for the Diverge SDK.
 *
 * Enum names are uppercase in Kotlin (`SANDBOX`); wire/docs use lowercase raw values
 * matching iOS (`sandbox`, `production`) via [wireName].
 */
enum class Environment(val apiBaseUrl: String, val wireName: String) {
    SANDBOX("https://sandbox.api.askdiverge.ai", "sandbox"),
    PRODUCTION("https://api.askdiverge.ai", "production"),
}
