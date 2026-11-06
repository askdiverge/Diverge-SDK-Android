package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.SubmitActionRequest

/**
 * Per-form draft and submission state. [ChatbotUiStateHolder] holds one per `partId` and changes
 * it only through these methods.
 */
internal data class FormSubmissionModel(
    val form: ConversationForm,
    val values: Map<String, String> = emptyMap(),
    val fieldErrors: Map<String, FormFieldError> = emptyMap(),
    val formError: FormBannerError? = null,
    val phase: Phase = Phase.Editing,
    val isHydrating: Boolean = false,
    val hydrationFailed: Boolean = false,
    val isUnavailable: Boolean = false
) {

    sealed interface Phase {
        data object Editing : Phase

        data object Submitting : Phase

        data class Submitted(val confirmation: String) : Phase

        data class Failed(val message: FormBannerError = FormBannerError.SubmitFailed) : Phase
    }

    val isSubmitted: Boolean get() = phase is Phase.Submitted

    val isSubmitting: Boolean get() = phase is Phase.Submitting

    val hasFields: Boolean get() = form.fields.isNotEmpty()

    val isCompletable: Boolean
        get() = !isUnavailable && FormValidator.blockingField(form, values) == null

    fun setValue(
        value: String,
        key: String
    ): FormSubmissionModel = copy(
        values = values + (key to value),
        fieldErrors = fieldErrors - key,
        phase = if (phase is Phase.Failed) Phase.Editing else phase
    )

    fun validate(): Pair<FormSubmissionModel, Boolean> {
        val result = FormValidator.validate(form, values)
        return copy(
            fieldErrors = result.fieldErrors,
            formError = result.formError
        ) to result.isValid
    }

    fun buildRequest(currentPage: String? = null): SubmitActionRequest? =
        FormActionBuilder.build(form, values, currentPage)

    fun beginSubmitting(): FormSubmissionModel? = when (phase) {
        Phase.Submitting, is Phase.Submitted -> null
        Phase.Editing, is Phase.Failed -> copy(phase = Phase.Submitting, formError = null)
    }

    fun markSubmitted(confirmation: String) = copy(
        phase = Phase.Submitted(confirmation),
        fieldErrors = emptyMap(),
        formError = null
    )

    fun markFailed(error: FormBannerError = FormBannerError.SubmitFailed) =
        copy(phase = Phase.Failed(error))

    fun markAlreadySent() = markSubmitted(confirmation = ALREADY_SENT_TOKEN)

    fun applyServerErrors(params: List<FormRequestException.Validation.Param>): FormSubmissionModel {
        val known = FormValidator.visibleFields(form, values).map { it.key }.toSet()
        val applied = FormServerErrors.apply(params, known)
        return copy(
            phase = Phase.Editing,
            fieldErrors = applied.fieldErrors,
            formError = applied.formError
        )
    }

    fun beginHydrating(): FormSubmissionModel {
        if (form.fields.isNotEmpty() || isSubmitted) return this
        return copy(isHydrating = true, hydrationFailed = false)
    }

    fun failHydrating() = copy(isHydrating = false, hydrationFailed = true)

    fun markUnavailable() = copy(
        isHydrating = false,
        hydrationFailed = false,
        isUnavailable = true
    )

    fun applyHydratedDefinition(definition: ChatFormDefinition): FormSubmissionModel {
        if (form.fields.isNotEmpty() || isSubmitted) return this
        val kind = form.kind as? ConversationForm.Kind.Custom ?: return this
        val definitionName = definition.name?.trim()?.takeIf { it.isNotEmpty() }
        return copy(
            form = form.copy(
                kind = kind.copy(
                    name = definitionName ?: kind.name,
                    confirmationText = definition.confirmationText ?: kind.confirmationText,
                    minFilledFields = if (definition.minFilledFields > 0) {
                        definition.minFilledFields
                    } else {
                        kind.minFilledFields
                    }
                ),
                fields = definition.fields
            ),
            isHydrating = false,
            hydrationFailed = false
        )
    }

    companion object {
        /**
         * Sentinel confirmation for a 409 already-submitted response. The card resolves it to
         * [ai.askdiverge.sdk.R.string.chat_form_already_sent].
         */
        const val ALREADY_SENT_TOKEN = "__already_sent__"

        fun confirmation(
            server: String?,
            form: ConversationForm,
            defaultConfirmation: String
        ): String {
            val trimmed = server?.trim().orEmpty()
            if (trimmed.isNotEmpty()) return trimmed
            return form.confirmationText?.trim()?.takeIf { it.isNotEmpty() } ?: defaultConfirmation
        }
    }
}
