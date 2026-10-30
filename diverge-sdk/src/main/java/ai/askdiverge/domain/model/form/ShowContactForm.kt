package ai.askdiverge.domain.model.form

/**
 * Instructs the client to display a contact / lead form. The marker carries every field the form
 * renders. Arrives whole, as a part event, in history or in `done`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/showcontactformmarker)
 */
internal data class ShowContactForm(
    val partId: String,
    val fields: List<FormField>
)
