package ai.askdiverge

/**
 * Which Diverge backend the chat talks to. The SDK only works against Diverge's own deployments,
 * so a host picks an environment and the SDK owns the URLs.
 *
 * @property baseUrl the Chatbot API root of this deployment; endpoint paths are relative to it.
 */
enum class DivergeEnvironment(internal val baseUrl: String) {

    /** The live API. The default — a host that says nothing gets production. */
    PRODUCTION("https://api.dialogintelligens.dk/api/"),

    /**
     * The shared development API: the same contract as production, for integrating against
     * backend changes before they are released.
     */
    DEVELOPMENT("https://dev.api.dialogintelligens.dk/api/")
}
