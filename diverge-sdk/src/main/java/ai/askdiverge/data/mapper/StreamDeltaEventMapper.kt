package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.event.StreamPartDeltaEventRemote
import ai.askdiverge.data.model.event.delta.DeltaActionRemote
import ai.askdiverge.data.model.event.delta.DeltaBlockTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaPartTypeRemote
import ai.askdiverge.data.model.event.delta.DeltaRemote
import ai.askdiverge.domain.model.event.StreamDeltaEvent
import ai.askdiverge.domain.model.event.delta.DeltaBlockType
import ai.askdiverge.domain.model.event.delta.DeltaPartType

internal fun StreamPartDeltaEventRemote.mapToDomain(): StreamDeltaEvent? = when (delta.action) {
    DeltaActionRemote.START_PART -> delta.toPartStartedEvent(partId = part_id)

    DeltaActionRemote.END_PART -> StreamDeltaEvent.PartEnded(partId = part_id)

    DeltaActionRemote.START_BLOCK -> delta.block_index?.let { blockIndex ->
        StreamDeltaEvent.BlockStarted(
            partId = part_id,
            blockIndex = blockIndex,
            blockType = delta.block_type.toDomain()
        )
    }

    DeltaActionRemote.END_BLOCK -> StreamDeltaEvent.BlockEnded(partId = part_id)

    DeltaActionRemote.APPEND_TEXT -> delta.text?.let { text ->
        delta.block_index?.let { blockIndex ->
            StreamDeltaEvent.TextAppended(
                partId = part_id,
                blockIndex = blockIndex,
                text = text
            )
        }
    }

    DeltaActionRemote.APPEND_SPAN -> delta.span?.let { span ->
        delta.block_index?.let { blockIndex ->
            StreamDeltaEvent.SpanAppended(
                partId = part_id,
                blockIndex = blockIndex,
                span = span.mapToDomain()
            )
        }
    }

    DeltaActionRemote.APPEND_ITEM -> delta.item?.let { item ->
        delta.block_index?.let { blockIndex ->
            StreamDeltaEvent.ItemAppended(
                partId = part_id,
                blockIndex = blockIndex,
                item = item.mapToDomain()
            )
        }
    }

    DeltaActionRemote.APPEND_PRODUCT -> delta.product?.let { product ->
        StreamDeltaEvent.ProductAppended(
            partId = part_id,
            product = product.mapToDomain()
        )
    }

    DeltaActionRemote.APPEND_ROW -> delta.row?.let { row ->
        StreamDeltaEvent.RowAppended(
            partId = part_id,
            row = row.map { it.mapToDomain() }
        )
    }

    DeltaActionRemote.UNKNOWN -> null
}

/**
 * Supporting a payload on the table event requires us to generate
 * the entire data structure instead of relying on the part_type
 * for choosing the builder in [StreamingMessageBuilder][ai.askdiverge.ui.streaming.StreamingMessageBuilder].
 */
private fun DeltaRemote.toPartStartedEvent(partId: String): StreamDeltaEvent = if (part_type == DeltaPartTypeRemote.TABLE) {
    StreamDeltaEvent.TablePartStarted(
        partId = partId,
        partType = part_type.toDomain(),
        caption = caption,
        headers = headers?.map { it.mapToDomain() } ?: emptyList(),
        alignments = alignments?.map { it.mapToDomain() }.orEmpty()
    )
} else {
    StreamDeltaEvent.PartStarted(
        partId = partId,
        partType = part_type.toDomain()
    )
}

private fun DeltaPartTypeRemote?.toDomain(): DeltaPartType = when (this) {
    DeltaPartTypeRemote.RICH_TEXT -> DeltaPartType.RICH_TEXT
    DeltaPartTypeRemote.PRODUCTS -> DeltaPartType.PRODUCTS
    DeltaPartTypeRemote.TABLE -> DeltaPartType.TABLE
    DeltaPartTypeRemote.UNKNOWN, null -> DeltaPartType.UNKNOWN
}

private fun DeltaBlockTypeRemote?.toDomain(): DeltaBlockType = when (this) {
    DeltaBlockTypeRemote.PARAGRAPH -> DeltaBlockType.PARAGRAPH
    DeltaBlockTypeRemote.BULLET_LIST -> DeltaBlockType.BULLET_LIST
    DeltaBlockTypeRemote.UNKNOWN, null -> DeltaBlockType.UNKNOWN
}
