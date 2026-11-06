package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.form.ContactFormActionRemote
import ai.askdiverge.data.model.form.FormSubmissionActionRemote
import ai.askdiverge.data.model.form.OutgoingAttachmentRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.form.SupportTicketActionRemote
import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse

internal fun SubmitActionRequest.mapToRemote() = SubmitActionRequestRemote(
    part_id = partId,
    current_page_url = currentPage,
    action = when (val action = action) {
        is SubmitAction.ContactForm -> ContactFormActionRemote(fields = action.fields)
        is SubmitAction.SupportTicket -> SupportTicketActionRemote(
            name = action.name,
            email = action.email,
            message = action.message,
            attachments = action.attachments.takeIf { it.isNotEmpty() }?.map { it.mapToRemote() }
        )
        is SubmitAction.FormSubmission -> FormSubmissionActionRemote(
            form_id = action.formId,
            values = action.values.takeIf { it.isNotEmpty() },
            files = action.files.takeIf { it.isNotEmpty() }?.mapValues { (_, attachment) -> attachment.mapToRemote() }
        )
    }
)

internal fun SubmitActionResponseRemote.mapToDomain() = SubmitActionResponse(
    submissionId = submission_id,
    confirmationText = confirmation_text
)

private fun OutgoingAttachment.mapToRemote() = OutgoingAttachmentRemote(
    filename = filename,
    mime_type = mimeType,
    data_base64 = dataBase64
)
