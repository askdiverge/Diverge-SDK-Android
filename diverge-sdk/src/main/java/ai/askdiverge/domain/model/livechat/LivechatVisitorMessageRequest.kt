package ai.askdiverge.domain.model.livechat

/**
 * Body for `POST /api/v1/chat/livechat/messages`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatvisitormessagerequest)
 */
internal data class LivechatVisitorMessageRequest(
    val text: String,
    /** Host page context, forwarded as `context.page`. `null` when the host supplies none. */
    val currentPage: String?,
    /**
     * Minted once per message and resent on retry. The server stores the message once per
     * operation id.
     */
    val clientOperationId: String
)
