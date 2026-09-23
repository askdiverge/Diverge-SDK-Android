package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.MessageRemote
import ai.askdiverge.data.model.message.incoming.MessageRoleRemote
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole

internal fun MessageRemote.mapToDomain(): Message = Message(
    id = message_id,
    role = role.mapToDomain(),
    parts = parts.map { it.mapToDomain() }
)

private fun MessageRoleRemote.mapToDomain(): MessageRole = when (this) {
    MessageRoleRemote.USER -> MessageRole.USER
    MessageRoleRemote.ASSISTANT -> MessageRole.ASSISTANT
    MessageRoleRemote.AGENT -> MessageRole.AGENT
    MessageRoleRemote.UNKNOWN -> MessageRole.UNKNOWN
}
