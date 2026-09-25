package ai.askdiverge.domain.model.event

import ai.askdiverge.domain.model.message.incoming.Message

internal data class StreamDoneEvent(val message: Message) : StreamEvent
