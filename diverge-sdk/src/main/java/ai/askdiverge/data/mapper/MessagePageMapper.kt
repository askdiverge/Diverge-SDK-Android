package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.domain.model.message.incoming.MessagePage

internal fun MessagePageRemote.mapToDomain(): MessagePage = MessagePage(
    hasMore = has_more,
    messages = messages.map { it.mapToDomain() },
    nextCursor = next_cursor
)
