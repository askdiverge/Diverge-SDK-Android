package ai.askdiverge.ui.streaming

import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.message.incoming.part.TablePart
import ai.askdiverge.domain.model.message.incoming.part.table.TableCell
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

private const val STREAMING_PART_ID = "streaming"

internal class TablePartBuilder : PartBuilder {

    private var started: StreamDeltaEvent.TablePartStarted? = null
    private val rows = mutableListOf<List<TableCell>>()

    override fun handle(delta: StreamDeltaEvent) {
        when (delta) {
            is StreamDeltaEvent.TablePartStarted -> started = delta
            is StreamDeltaEvent.RowAppended -> rows.add(delta.row)
            else -> Unit
        }
    }

    override fun build(): MessagePartUiModel? {
        if (started?.headers.isNullOrEmpty() && rows.isEmpty()) return null
        return TablePart(
            id = STREAMING_PART_ID,
            caption = started?.caption,
            headers = started?.headers.orEmpty(),
            alignments = started?.alignments.orEmpty(),
            rows = rows.toList()
        ).mapToUi()
    }
}
