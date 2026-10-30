package ai.askdiverge.domain.model.form

/**
 * Full form definition from `GET /api/v1/chat/forms/{form_id}`, used to hydrate a `show_form`
 * marker that arrives without its fields.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatformdefinition)
 */
internal data class ChatFormDefinition(
    val formId: String,
    /** Form name rendered to the visitor, trimmed. `null` when [formId] is the display label. */
    val name: String?,
    /** Copy shown after submit. `null` when the form sets none. */
    val confirmationText: String?,
    val fields: List<FormField>,
    val minFilledFields: Int
)
