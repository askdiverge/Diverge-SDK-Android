package ai.askdiverge.domain.model.livechat

import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.incoming.part.MessagePart

/**
 * A message in the livechat session log: the usual message fields plus its position in the log.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechatmessage)
 */
internal data class LivechatMessage(
    val messageId: String,
    val role: MessageRole,
    val agent: LivechatAgent?,
    val parts: List<MessagePart>,
    /** ISO 8601 date-time. */
    val createdAt: String,
    /** Increases by one per message in the session; the poll cursor. */
    val sequenceNumber: Long
) {
    fun toMessage() = Message(
        id = messageId,
        role = role,
        agent = agent,
        parts = parts
    )
}
