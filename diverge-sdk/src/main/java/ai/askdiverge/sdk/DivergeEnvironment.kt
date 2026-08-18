package ai.askdiverge.sdk

/**
 * Deployment environment for the Diverge SDK.
 *
 * Named `DivergeEnvironment` to avoid colliding with `android.os.Environment`.
 * Enum names are uppercase in Kotlin (`SANDBOX`); wire/docs use lowercase raw values
 * matching iOS (`sandbox`, `production`) via [wireName].
 */
enum class DivergeEnvironment(val apiBaseUrl: String, val wireName: String) {
    SANDBOX("https://sandbox.api.askdiverge.ai", "sandbox"),
    PRODUCTION("https://api.askdiverge.ai", "production"),
}
