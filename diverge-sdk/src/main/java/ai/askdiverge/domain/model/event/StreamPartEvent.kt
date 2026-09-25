package ai.askdiverge.domain.model.event

import ai.askdiverge.domain.model.message.incoming.part.MessagePart

internal data class StreamPartEvent(val part: MessagePart) : StreamEvent
