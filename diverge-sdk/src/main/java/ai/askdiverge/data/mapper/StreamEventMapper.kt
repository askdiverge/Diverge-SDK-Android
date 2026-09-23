package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.event.StreamDoneEventRemote
import ai.askdiverge.data.model.event.StreamErrorEventRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.event.StreamPartDeltaEventRemote
import ai.askdiverge.data.model.event.StreamPartEventRemote
import ai.askdiverge.data.model.event.StreamStatusEventRemote
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.model.event.StreamErrorEvent
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.event.StreamStatusEvent

internal fun StreamEventRemote.mapToDomain(): StreamEvent? = when (this) {
    is StreamStatusEventRemote -> StreamStatusEvent(status)
    is StreamPartEventRemote -> part?.let { StreamPartEvent(part = it.mapToDomain()) }
    is StreamPartDeltaEventRemote -> mapToDomain()
    is StreamDoneEventRemote -> StreamDoneEvent(message = message.mapToDomain())
    is StreamErrorEventRemote -> StreamErrorEvent(message = message, retryable = retryable)
}
