package ai.askdiverge.domain.model.livechat

import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput

/**
 * Body for `POST /api/v1/chat/livechat/messages`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatvisitormessagerequest)
 */
internal data class LivechatVisitorMessageRequest(
    /** Plain text part; empty when the visitor sends photos only. */
    val text: String,
    /** Image parts after the text, if any. */
    val images: List<OutgoingImageInput>,
    /** Host page context, forwarded as `context.page`. `null` when the host supplies none. */
    val currentPage: String?,
    /**
     * Minted once per message and resent on retry. The server stores the message once per
     * operation id.
     */
    val clientOperationId: String
)
