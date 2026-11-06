package ai.askdiverge.ui.form

import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField
import ai.askdiverge.domain.model.form.FormFieldCondition

/**
 * Pure validation for a [ConversationForm] draft. Hidden fields (whose `visible_when` conditions
 * do not match) are never required and never counted toward `min_filled_fields`.
 */
internal object FormValidator {

    /** Contract cap on the ticket `email` (`@maxLength(254)`); the server rejects longer values. */
    const val MAX_EMAIL_LENGTH = 254

    data class Result(
        val fieldErrors: Map<String, FormFieldError>,
        val formError: FormBannerError?
    ) {
        val isValid: Boolean get() = fieldErrors.isEmpty() && formError == null
    }

    /**
     * Visible fields in definition order — the single visibility API shared by the card, the
     * validator and the request builder.
     */
    fun visibleFields(
        form: ConversationForm,
        values: Map<String, String>
    ): List<FormField> {
        val visible = mutableListOf<FormField>()
        val visibleKeys = mutableSetOf<String>()
        for (field in form.fields) {
            val shown = field.visibleWhen.all { condition ->
                if (condition.field !in visibleKeys) return@all false
                val answer = values[condition.field].orEmpty().trim()
                when (condition.operator) {
                    FormFieldCondition.Operator.EQUALS -> answer == condition.value
                    FormFieldCondition.Operator.UNKNOWN -> false
                }
            }
            if (shown) {
                visible += field
                visibleKeys += field.key
            }
        }
        return visible
    }

    /**
     * The first visible required field this app has no control for — a `file` field until photo
     * attach ships.
     */
    fun blockingField(
        form: ConversationForm,
        values: Map<String, String>
    ): FormField? = visibleFields(form, values).firstOrNull { field ->
        field.required && field.type == FormField.FieldType.FILE
    }

    fun validate(
        form: ConversationForm,
        values: Map<String, String>
    ): Result {
        val fieldErrors = mutableMapOf<String, FormFieldError>()
        val visible = visibleFields(form, values)

        for (field in visible) {
            when (field.type) {
                FormField.FieldType.FILE -> Unit
                FormField.FieldType.EMAIL -> {
                    val trimmed = values[field.key].orEmpty().trim()
                    when {
                        field.required && trimmed.isEmpty() -> fieldErrors[field.key] = FormFieldError.REQUIRED
                        trimmed.isNotEmpty() &&
                            (!isValidEmail(trimmed) || trimmed.length > MAX_EMAIL_LENGTH) ->
                            fieldErrors[field.key] = FormFieldError.INVALID_EMAIL
                    }
                }
                FormField.FieldType.TEXT,
                FormField.FieldType.TEL,
                FormField.FieldType.TEXTAREA,
                FormField.FieldType.DROPDOWN -> {
                    val trimmed = values[field.key].orEmpty().trim()
                    if (field.required && trimmed.isEmpty()) {
                        fieldErrors[field.key] = FormFieldError.REQUIRED
                    }
                }
            }
        }

        var formError: FormBannerError? = null
        val min = minimumAnswers(form)
        if (min > 0) {
            val filled = visible.count { field ->
                field.type != FormField.FieldType.FILE &&
                    values[field.key].orEmpty().trim().isNotEmpty()
            }
            if (filled < min) formError = FormBannerError.MinFilled(min)
        }
        if (blockingField(form, values) != null) {
            formError = FormBannerError.Unavailable
        }

        return Result(fieldErrors = fieldErrors, formError = formError)
    }

    private fun minimumAnswers(form: ConversationForm): Int = when (form.kind) {
        is ConversationForm.Kind.SupportTicket -> form.minFilledFields
        ConversationForm.Kind.Contact, is ConversationForm.Kind.Custom -> maxOf(1, form.minFilledFields)
    }

    /** Contract email pattern: at least one non-space/@ before and after `@`, with a dot in the domain. */
    fun isValidEmail(value: String): Boolean =
        Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(value)
}
