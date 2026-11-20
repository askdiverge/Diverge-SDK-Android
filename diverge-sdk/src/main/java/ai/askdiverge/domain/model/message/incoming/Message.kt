package ai.askdiverge.domain.model.message.incoming

import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.message.incoming.part.MessagePart

internal data class Message(
    val id: String,
    val role: MessageRole,
    /** Livechat agent identity for `role: agent` messages. `null` for other roles. */
    val agent: LivechatAgent?,
    val parts: List<MessagePart>,
    /**
     * When the message was stored, as an RFC 3339 date-time. History keeps the stored value as
     * written, often with microseconds and an offset of its own. `null` when the response carried
     * none.
     */
    val createdAt: String?
)
