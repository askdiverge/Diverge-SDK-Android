package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.ProductPartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.TablePartRemote
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.domain.model.message.incoming.part.MessagePart
import ai.askdiverge.domain.model.message.incoming.part.ProductPart
import ai.askdiverge.domain.model.message.incoming.part.RichTextPart
import ai.askdiverge.domain.model.message.incoming.part.TablePart

internal fun MessagePartRemote.mapToDomain(): MessagePart = when (this) {
    is RichTextPartRemote -> RichTextPart(
        id = part_id,
        blocks = blocks.map { it.mapToDomain() }
    )

    is ImagePartRemote -> ImagePart(url = url)

    is ProductPartRemote -> ProductPart(
        id = part_id,
        products = products.map { it.mapToDomain() }
    )

    is TablePartRemote -> TablePart(
        id = part_id,
        caption = caption,
        headers = headers.map { it.mapToDomain() },
        alignments = alignments?.map { it.mapToDomain() }.orEmpty(),
        rows = rows.map { row -> row.map { it.mapToDomain() } }
    )
}
