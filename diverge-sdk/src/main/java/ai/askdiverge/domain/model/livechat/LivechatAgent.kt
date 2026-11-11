package ai.askdiverge.domain.model.livechat

/**
 * Livechat agent identity shown beside agent turns.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatagentidentity)
 */
internal data class LivechatAgent(
    val agentId: String,
    val displayName: String,
    /**
     * The agent's avatar URL string as the API sent it. `null` when the API sends `null`; the
     * client then shows its fallback avatar. Relative paths are resolved later against the API
     * base when a download or image load needs an absolute URL.
     */
    val avatarUrl: String?
)
