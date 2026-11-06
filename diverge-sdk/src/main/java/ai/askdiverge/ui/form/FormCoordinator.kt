package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.state.PendingBotMessageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Form draft lifecycle and submit/hydrate calls. Kept out of [ai.askdiverge.ui.ChatbotViewModel]
 * so that class stays orchestration-sized.
 */
internal class FormCoordinator(
    private val uiStateHolder: ChatbotUiStateHolder,
    private val submitActionUseCase: SubmitActionUseCase,
    private val fetchFormUseCase: FetchFormUseCase,
    private val scope: CoroutineScope,
    private val currentPage: String?,
    private val onSessionExpired: () -> Unit,
    private val defaultConfirmation: () -> String
) {

    fun ensureForm(form: ConversationForm) {
        if (uiStateHolder.ensureForm(form)) {
            val formId = (form.kind as? ConversationForm.Kind.Custom)?.formId ?: return
            hydrate(partId = form.partId, formId = formId)
        }
    }

    fun ensureFormPart(part: MessagePartUiModel) {
        if (part is MessagePartUiModel.Form) ensureForm(part.form)
    }

    fun onValueChanged(
        partId: String,
        key: String,
        value: String
    ) {
        uiStateHolder.updateForm(partId) { it.setValue(value = value, key = key) }
    }

    fun onReload(partId: String) {
        val model = uiStateHolder.uiState.value.formModels[partId] ?: return
        if (!model.hydrationFailed) return
        val formId = (model.form.kind as? ConversationForm.Kind.Custom)?.formId ?: return
        hydrate(partId = partId, formId = formId)
    }

    fun onSubmit(partId: String) {
        val state = uiStateHolder.uiState.value
        val model = state.formModels[partId] ?: return
        if (model.isSubmitting || model.isSubmitted) return
        // Backend persists the form when the reply stream ends; submit earlier is rejected.
        if (state.pendingBotMessage != PendingBotMessageState.None) return

        val (validated, isValid) = model.validate()
        uiStateHolder.updateForm(partId) { validated }
        if (!isValid) return

        val request = validated.buildRequest(currentPage) ?: run {
            uiStateHolder.updateForm(partId) { it.markFailed() }
            return
        }
        var began = false
        uiStateHolder.updateForm(partId) { current ->
            current.beginSubmitting()?.also { began = true }
        }
        if (!began) return

        scope.launch {
            submitActionUseCase(request)
                .onSuccess { response ->
                    val text = FormSubmissionModel.confirmation(
                        server = response.confirmationText,
                        form = validated.form,
                        defaultConfirmation = defaultConfirmation()
                    )
                    uiStateHolder.updateForm(partId) { it.markSubmitted(confirmation = text) }
                }
                .onFailure { error ->
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> onSessionExpired()
                        is FormRequestException.Conflict ->
                            uiStateHolder.updateForm(partId) { it.markAlreadySent() }
                        is FormRequestException.Validation ->
                            uiStateHolder.updateForm(partId) { it.applyServerErrors(error.params) }
                        else -> uiStateHolder.updateForm(partId) { it.markFailed() }
                    }
                }
        }
    }

    private fun hydrate(
        partId: String,
        formId: String
    ) {
        uiStateHolder.updateForm(partId) { it.beginHydrating() }
        scope.launch {
            fetchFormUseCase(formId)
                .onSuccess { definition ->
                    uiStateHolder.updateForm(partId) { it.applyHydratedDefinition(definition) }
                }
                .onFailure { error ->
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> onSessionExpired()
                        is FormRequestException.NotFound ->
                            uiStateHolder.updateForm(partId) { it.markUnavailable() }
                        else -> uiStateHolder.updateForm(partId) { it.failHydrating() }
                    }
                }
        }
    }
}
