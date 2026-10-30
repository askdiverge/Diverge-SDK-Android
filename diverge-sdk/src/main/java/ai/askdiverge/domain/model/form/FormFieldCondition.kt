package ai.askdiverge.domain.model.form

/**
 * A condition evaluated against the visitor's answer to an earlier form field. Only [EQUALS] is
 * supported today; reserved for future operators.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/formfieldcondition)
 */
internal data class FormFieldCondition(
    /** Key of a dropdown field defined earlier in the same `fields` array. */
    val field: String,
    val operator: Operator,
    val value: String
) {

    /**
     * How [value] is compared with the visitor's answer to [field]. Unrecognised wire values map
     * to [UNKNOWN].
     */
    internal enum class Operator {
        EQUALS,
        UNKNOWN
    }
}
