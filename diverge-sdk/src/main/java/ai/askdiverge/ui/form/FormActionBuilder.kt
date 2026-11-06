package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.SubmitAction
import ai.askdiverge.domain.model.form.SubmitActionRequest

/**
 * Builds a [SubmitActionRequest] from a filled [ConversationForm]. It sends the trimmed,
 * non-empty answers of visible fields; `file` fields carry no answer until photo attach ships.
 */
internal object FormActionBuilder {

    /**
     * Builds the request, or `null` when nothing submittable remains — an empty contact / custom
     * payload, or a support ticket missing one of `name` / `email` / `message`.
     */
    fun build(
        form: ConversationForm,
        values: Map<String, String>,
        currentPage: String? = null
    ): SubmitActionRequest? {
        val answers = linkedMapOf<String, String>()
        for (field in FormValidator.visibleFields(form, values)) {
            if (field.type == FormField.FieldType.FILE) continue
            val trimmed = values[field.key].orEmpty().trim()
            if (trimmed.isNotEmpty()) answers[field.key] = trimmed
        }

        return when (val kind = form.kind) {
            ConversationForm.Kind.Contact -> {
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
                        attachments = emptyList()
                    )
                )
            }
            is ConversationForm.Kind.Custom -> {
                if (answers.isEmpty()) return null
                SubmitActionRequest(
                    partId = form.partId,
                    currentPage = currentPage,
                    action = SubmitAction.FormSubmission(
                        formId = kind.formId,
                        values = answers,
                        files = emptyMap()
                    )
                )
            }
        }
    }
}
