package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal interface PartBuilder {
    fun handle(delta: StreamDeltaEvent)
    fun build(): MessagePartUiModel?
}
