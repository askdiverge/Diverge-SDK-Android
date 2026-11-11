package ai.askdiverge.domain.model.livechat

/**
 * One `GET /livechat/sync` response: the visitor's livechat state and the next page of messages.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-livechat/GET/api/v1/chat/livechat/sync)
 */
internal data class LivechatSync(
    val state: LivechatState,
    /** Messages after the requested sequence number, oldest first. */
    val messages: List<LivechatMessage>,
    val hasMore: Boolean,
    /**
     * Change hint the next request waits on. `null` when the server cannot wait; the client then
     * polls at a fixed interval.
     */
    val syncCursor: String?
)
