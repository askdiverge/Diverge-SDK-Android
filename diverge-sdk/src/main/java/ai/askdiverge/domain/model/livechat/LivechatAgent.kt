package ai.askdiverge.domain.model.livechat

/**
 * Livechat agent identity shown beside agent turns.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatagentidentity)
 */
internal data class LivechatAgent(
    /** The agent's ID. `null` when the API sends none. */
    val agentId: String?,
    /** The agent's name as the API sent it. `null` when the API sends none. */
    val displayName: String?,
    /**
     * The agent's avatar URL string as the API sent it. `null` when the API sends `null`; the
     * client then shows its fallback avatar.
     */
    val avatarUrl: String?
)
