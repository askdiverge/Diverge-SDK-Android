package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaPartType
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal class StreamingMessageBuilder(private val partBuilderFactory: PartBuilderFactory) {

    private var currentBuilder: PartBuilder? = null
    private var completedParts: List<MessagePartUiModel> = emptyList()

    fun process(delta: StreamDeltaEvent): List<MessagePartUiModel> {
        when (delta) {
            is StreamDeltaEvent.PartStarted -> startPart(delta.partType)

            // Tables receive headers in the Started event which deviates
            // from other start events. This requires us to
            // choose a builder and start the process handling immediately.
            // This breaks the current architecture for handling starting events.
            is StreamDeltaEvent.TablePartStarted -> {
                startPart(delta.partType)
                currentBuilder?.handle(delta)
            }

            is StreamDeltaEvent.PartEnded -> finishPart()

            else -> currentBuilder?.handle(delta)
        }
        return completedParts + listOfNotNull(currentBuilder?.build())
    }

    private fun startPart(partType: DeltaPartType) {
        currentBuilder = partBuilderFactory.create(partType)
        completedParts = emptyList()
    }

    private fun finishPart() {
        currentBuilder?.build()?.let { completedParts = completedParts + it }
        currentBuilder = null
    }

    fun reset() {
        currentBuilder = null
        completedParts = emptyList()
    }
}
