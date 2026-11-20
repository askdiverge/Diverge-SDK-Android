package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatSessionState
import ai.askdiverge.domain.model.form.ChatSessionValue
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.FormField

/**
 * Draft + save state for the livechat waiting-room session form.
 *
 * Unlike [FormSubmissionModel], a successful save keeps the fields filled and shows a
 * "Details saved" caption — it does not collapse to a confirmation. A save sends only the
 * non-empty values of visible fields: the server keeps every value it stored, so a value can be
 * replaced but not removed.
 */
internal data class WaitingFormModel(
    val form: ConversationForm,
    val formId: String,
    val values: Map<String, String> = emptyMap(),
    /** The values the session holds, by key. */
    val savedValues: Map<String, String> = emptyMap(),
    val fieldErrors: Map<String, FormFieldError> = emptyMap(),
    val formError: FormBannerError? = null,
    val phase: Phase = Phase.Editing,
    /**
     * Counts the saves that found a field with an error. The card scrolls to
     * [firstFieldWithError] on each, also when it is the field the last one found.
     */
    val fieldErrorReveals: Int = 0
) {

    sealed interface Phase {
        data object Editing : Phase
        data object Saving : Phase
        data object Saved : Phase
        data object Failed : Phase
    }

    val isSaving: Boolean get() = phase is Phase.Saving

    val isSaved: Boolean get() = phase is Phase.Saved

    /** The draft differs from what the session holds, and no save is in flight. */
    val canSave: Boolean
        get() = !isSaving &&
            valuesForPatch().any { (key, value) -> value != (savedValues[key] ?: "") }

    /** The first visible field with an error, for scrolling into view. */
    val firstFieldWithError: String?
        get() = FormValidator.visibleFields(form, values)
            .firstOrNull { fieldErrors[it.key] != null }
            ?.key

    fun setValue(
        value: String,
        key: String
    ): WaitingFormModel = copy(
        values = values + (key to value),
        fieldErrors = fieldErrors - key,
        formError = null,
        phase = when (phase) {
            is Phase.Failed, is Phase.Saved -> Phase.Editing
            else -> phase
        }
    )

    fun validate(): Pair<WaitingFormModel, Boolean> {
        val result = FormValidator.validate(
            form = form,
            values = values,
            files = emptySet(),
            offersFileFields = false
        )
        val next = copy(
            fieldErrors = result.fieldErrors,
            formError = result.formError
        ).revealFieldErrors()
        return next to result.isValid
    }

    /**
     * The `PATCH` body: trimmed, non-empty values of visible fields. A field the visitor emptied
     * or a condition now hides isn't sent, and the server keeps what it stored for it.
     */
    fun valuesForPatch(): Map<String, String> {
        val out = linkedMapOf<String, String>()
        for (field in FormValidator.visibleFields(form, values)) {
            val trimmed = values[field.key].orEmpty().trim()
            if (trimmed.isNotEmpty()) out[field.key] = trimmed
        }
        return out
    }

    fun beginSaving(): WaitingFormModel? = when (phase) {
        Phase.Saving -> null
        Phase.Editing, Phase.Saved, Phase.Failed -> copy(
            phase = Phase.Saving,
            formError = null
        )
    }

    /**
     * Takes the session the save returned as the saved state: the card's fields show its values,
     * including any the visitor emptied, since the server keeps those.
     */
    fun markSaved(session: ChatSessionState): WaitingFormModel {
        val saved = sessionValuesMap(session.values)
        return copy(
            values = form.fields.associate { field -> field.key to (saved[field.key] ?: "") },
            savedValues = saved,
            phase = Phase.Saved,
            fieldErrors = emptyMap(),
            formError = null
        )
    }

    fun markFailed(): WaitingFormModel = copy(
        phase = Phase.Failed,
        formError = null
    )

    fun applyServerErrors(params: List<FormRequestException.Validation.Param>): WaitingFormModel {
        val known = FormValidator.visibleFields(form, values).map { it.key }.toSet()
        val applied = FormServerErrors.apply(params = params, knownFieldKeys = known)
        return copy(
            phase = Phase.Editing,
            fieldErrors = applied.fieldErrors,
            formError = applied.formError
        ).revealFieldErrors()
    }

    private fun revealFieldErrors(): WaitingFormModel =
        if (fieldErrors.isNotEmpty()) copy(fieldErrorReveals = fieldErrorReveals + 1) else this

    companion object {

        /**
         * The fields the card shows, or null when it has none or no save could succeed.
         * `PATCH …/values` requires every required field of the form, whether or not a condition
         * shows it. A required `file` field or a required field behind a condition means no card;
         * optional file fields are left out of it.
         */
        fun cardFields(from: ChatFormDefinition): List<FormField>? {
            val unsavable = from.fields.any { field ->
                field.required &&
                    (field.type == FormField.FieldType.FILE || field.visibleWhen.isNotEmpty())
            }
            val fields = from.fields.filter { it.type != FormField.FieldType.FILE }
            return if (unsavable || fields.isEmpty()) null else fields
        }

        fun create(
            definition: ChatFormDefinition,
            sessionValues: List<ChatSessionValue>
        ): WaitingFormModel? {
            val fields = cardFields(definition) ?: return null
            val saved = sessionValuesMap(sessionValues)
            return WaitingFormModel(
                form = ConversationForm(
                    partId = "waiting:${definition.formId}",
                    kind = ConversationForm.Kind.Custom(
                        formId = definition.formId,
                        name = definition.name ?: definition.formId,
                        confirmationText = null,
                        minFilledFields = definition.minFilledFields
                    ),
                    fields = fields
                ),
                formId = definition.formId,
                values = fields.associate { field -> field.key to (saved[field.key] ?: "") },
                savedValues = saved
            )
        }

        private fun sessionValuesMap(values: List<ChatSessionValue>): Map<String, String> =
            values.associate { it.key to it.value }
    }
}
