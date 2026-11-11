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
     *
     * Mirrors the server's forward pass: a condition matches only if the field it references is
     * already visible and holds the trimmed value.
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
     * The first visible required field this app can't fill — a `file` field while photo attach
     * isn't offered for this form. While one is visible the form can't be completed.
     *
     * @param offersFileFields whether the host offers photo attach for file fields.
     */
    fun blockingField(
        form: ConversationForm,
        values: Map<String, String>,
        offersFileFields: Boolean
    ): FormField? {
        val fileFieldsFillable = offersFileFields && form.acceptsFileFields
        return visibleFields(form, values).firstOrNull { field ->
            field.required && field.type == FormField.FieldType.FILE && !fileFieldsFillable
        }
    }

    /**
     * Validates the draft. [files] holds keys that currently have a picked file.
     * [offersFileFields] is whether the host offers photo attach.
     */
    fun validate(
        form: ConversationForm,
        values: Map<String, String>,
        files: Set<String> = emptySet(),
        offersFileFields: Boolean = false
    ): Result {
        val fieldErrors = mutableMapOf<String, FormFieldError>()
        val visible = visibleFields(form, values)
        val fileFieldsFillable = offersFileFields && form.acceptsFileFields
        var missingRequired = 0

        for (field in visible) {
            when (field.type) {
                FormField.FieldType.FILE -> {
                    if (field.required && fileFieldsFillable && field.key !in files) {
                        fieldErrors[field.key] = FormFieldError.REQUIRED
                        missingRequired += 1
                    }
                }
                FormField.FieldType.EMAIL -> {
                    val trimmed = values[field.key].orEmpty().trim()
                    when {
                        field.required && trimmed.isEmpty() -> {
                            fieldErrors[field.key] = FormFieldError.REQUIRED
                            missingRequired += 1
                        }
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
                        missingRequired += 1
                    }
                }
            }
        }

        var formError: FormBannerError? = null
        val min = minimumAnswers(form)
        if (min > 0) {
            val filled = visible.count { field ->
                when (field.type) {
                    FormField.FieldType.FILE -> fileFieldsFillable && field.key in files
                    else -> values[field.key].orEmpty().trim().isNotEmpty()
                }
            }
            // A required field's own error already says what is missing, so the banner shows only
            // when filling those fields would still fall short.
            if (filled + missingRequired < min) {
                formError = FormBannerError.MinFilled(min)
            }
        }
        if (blockingField(form, values, offersFileFields) != null) {
            formError = FormBannerError.Unavailable
        }

        return Result(fieldErrors = fieldErrors, formError = formError)
    }

    private fun minimumAnswers(form: ConversationForm): Int = when (form.kind) {
        is ConversationForm.Kind.SupportTicket -> form.minFilledFields
        ConversationForm.Kind.Contact, is ConversationForm.Kind.Custom -> maxOf(1, form.minFilledFields)
    }

    /** Contract email pattern: at least one non-space/@ before and after `@`, with a dot in the domain. */
    private val EMAIL_PATTERN = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value)
}
