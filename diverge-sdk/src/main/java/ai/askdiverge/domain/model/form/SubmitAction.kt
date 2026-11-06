package ai.askdiverge.domain.model.form

/**
 * What the visitor submitted on `POST /api/v1/chat/actions`, discriminated on wire `type`. Hidden
 * fields must be excluded before building.
 *
 * [API ref](https://docs.askdiverge.ai/api#tag/visitor-actions/POST/api/v1/chat/actions)
 */
internal sealed interface SubmitAction {

    /** Whether the contract accepts this action; one it rejects is never sent. */
    val isSubmittable: Boolean

    /** Contact / lead form — arbitrary key → string map. The contract requires at least one field. */
    data class ContactForm(val fields: Map<String, String>) : SubmitAction {
        override val isSubmittable: Boolean
            get() = fields.isNotEmpty()
    }

    /**
     * Support ticket — fixed name / email / message plus attachments, empty when the visitor
     * attached none. The contract requires name, email and message to be non-empty.
     */
    data class SupportTicket(
        val name: String,
        val email: String,
        val message: String,
        val attachments: List<OutgoingAttachment>
    ) : SubmitAction {
        override val isSubmittable: Boolean
            get() = name.isNotEmpty() && email.isNotEmpty() && message.isNotEmpty()
    }

    /**
     * Custom form — text values and/or file values keyed by [FormField.key]. The contract requires
     * at least one value or file.
     */
    data class FormSubmission(
        val formId: String,
        val values: Map<String, String>,
        val files: Map<String, OutgoingAttachment>
    ) : SubmitAction {
        override val isSubmittable: Boolean
            get() = values.isNotEmpty() || files.isNotEmpty()
    }
}
