package ai.askdiverge.domain.model.form

/**
 * A dynamic form field definition carried inline on a form marker.
 *
 * A missing or unrecognised wire `type` reads as [FieldType.TEXT], so a required field always has
 * a control; the deprecated `phone` alias reads as [FieldType.TEL].
 *
 * [API ref](https://docs.askdiverge.ai/api#model/formfield)
 */
internal data class FormField(
    val key: String,
    val label: String,
    /** Hint text shown in the empty input. `null` when the operator set none. */
    val placeholder: String?,
    val type: FieldType,
    val required: Boolean,
    val options: List<String>,
    val visibleWhen: List<FormFieldCondition>
) {

    /**
     * The input control the client renders for the field. A case is added when the backend
     * introduces a new input type and the SDK ships a control for it.
     */
    internal enum class FieldType {
        TEXT,
        EMAIL,
        TEL,
        TEXTAREA,

        /** File upload. Native upload is a known v1 gap, so no control ships for this case yet. */
        FILE,
        DROPDOWN
    }
}
