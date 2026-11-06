package ai.askdiverge.domain.model.form

/**
 * Instructs the client to display a client-managed custom chat form. On submit the client sends
 * the visitor's values with this [partId], and the backend runs the form's submit actions.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showformmarker)
 */
internal data class ShowForm(
    val partId: String,
    val formId: String,
    /**
     * Form name rendered to the visitor, trimmed. The backend sends the `form_id` for a form the
     * operator did not name. `null` when the marker carries no name or a blank one.
     */
    val name: String?,
    /** Copy shown after submit. `null` when the form sets none. */
    val confirmationText: String?,
    /** Empty when the marker arrives thin; `GET …/forms/{form_id}` supplies the definition. */
    val fields: List<FormField>,
    val minFilledFields: Int
)
