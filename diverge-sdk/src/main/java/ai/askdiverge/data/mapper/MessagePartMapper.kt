package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.form.ShowContactFormRemote
import ai.askdiverge.data.model.form.ShowFormRemote
import ai.askdiverge.data.model.form.ShowSupportTicketRemote
import ai.askdiverge.data.model.message.incoming.part.ImagePartRemote
import ai.askdiverge.data.model.message.incoming.part.MessagePartRemote
import ai.askdiverge.data.model.message.incoming.part.ProductPartRemote
import ai.askdiverge.data.model.message.incoming.part.RichTextPartRemote
import ai.askdiverge.data.model.message.incoming.part.TablePartRemote
import ai.askdiverge.domain.model.form.ShowContactForm
import ai.askdiverge.domain.model.form.ShowForm
import ai.askdiverge.domain.model.form.ShowSupportTicket
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

    is ShowContactFormRemote -> ShowContactForm(
        partId = part_id,
        fields = fields.map { it.mapToDomain() }
    )

    is ShowSupportTicketRemote -> ShowSupportTicket(
        partId = part_id,
        fields = fields.map { it.mapToDomain() },
        attachmentsAccepted = attachments_accepted,
        maxAttachmentSizeBytes = max_attachment_size_bytes
    )

    is ShowFormRemote -> ShowForm(
        partId = part_id,
        formId = form_id,
        name = name?.trim()?.takeIf { it.isNotEmpty() },
        confirmationText = confirmation_text,
        fields = fields.map { it.mapToDomain() },
        minFilledFields = min_filled_fields
    )

    else -> error("Unexpected message part remote: ${this::class.java.name}")
}
