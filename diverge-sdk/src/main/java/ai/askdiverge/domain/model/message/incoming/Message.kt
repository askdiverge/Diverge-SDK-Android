package ai.askdiverge.domain.model.message.incoming

import ai.askdiverge.domain.model.message.incoming.part.MessagePart

internal data class Message(
    val id: String,
    val role: MessageRole,
    val parts: List<MessagePart>
)
