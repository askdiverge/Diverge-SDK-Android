package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest

/**
 * Builds a [SubmitActionRequest] from a filled [ConversationForm]. Hidden fields are excluded
 * (same forward pass as [FormValidator.visibleFields]). Ticket attachments ride as a separate
 * array (not field-keyed).
 */
internal object FormActionBuilder {

    /**
     * Builds the request, or `null` when nothing submittable remains — an empty contact /
     * custom payload, or a support ticket missing one of `name` / `email` / `message`.
     *
     * @param files picked files keyed by [FormField.key] (custom-form file fields).
     * @param ticketAttachments ticket-level attachments (support-ticket forms only).
     */
    fun build(
        form: ConversationForm,
        values: Map<String, String>,
        files: Map<String, OutgoingAttachment> = emptyMap(),
        ticketAttachments: List<OutgoingAttachment> = emptyList(),
        currentPage: String? = null
    ): SubmitActionRequest? {
        val visible = FormValidator.visibleFields(form, values)
        val answers = textAnswers(visible, values)

        return when (val kind = form.kind) {
            ConversationForm.Kind.Contact -> {
                // ContactForm.fields is a string map — the wire has no slot for files.
                if (answers.isEmpty()) return null
                SubmitActionRequest(
                    partId = form.partId,
                    currentPage = currentPage,
                    action = SubmitAction.ContactForm(fields = answers)
                )
            }
            is ConversationForm.Kind.SupportTicket -> {
                val name = answers["name"] ?: return null
                val email = answers["email"] ?: return null
                val message = answers["message"] ?: return null
                SubmitActionRequest(
                    partId = form.partId,
                    currentPage = currentPage,
                    action = SubmitAction.SupportTicket(
                        name = name,
                        email = email,
                        message = message,
                        attachments = ticketAttachments
                    )
                )
            }
            is ConversationForm.Kind.Custom -> {
                val fileValues = linkedMapOf<String, OutgoingAttachment>()
                for (field in visible) {
                    if (field.type == FormField.FieldType.FILE) {
                        files[field.key]?.let { fileValues[field.key] = it }
                    }
                }
                if (answers.isEmpty() && fileValues.isEmpty()) return null
                SubmitActionRequest(
                    partId = form.partId,
                    currentPage = currentPage,
                    action = SubmitAction.FormSubmission(
                        formId = kind.formId,
                        values = answers.ifEmpty { emptyMap() },
                        files = fileValues
                    )
                )
            }
        }
    }

    private fun textAnswers(
        visible: List<FormField>,
        values: Map<String, String>
    ): Map<String, String> {
        val answers = linkedMapOf<String, String>()
        for (field in visible) {
            if (field.type == FormField.FieldType.FILE) continue
            val trimmed = values[field.key].orEmpty().trim()
            if (trimmed.isNotEmpty()) answers[field.key] = trimmed
        }
        return answers
    }
}
