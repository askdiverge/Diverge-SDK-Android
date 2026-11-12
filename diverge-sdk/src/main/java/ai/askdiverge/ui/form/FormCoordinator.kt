package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.ConversationForm
import ai.askdiverge.domain.model.form.OutgoingAttachment
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.SubmitActionUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.state.PendingBotMessageState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

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
    private val offersAttachments: Boolean,
    private val encodeDispatcher: CoroutineDispatcher,
    private val onSessionExpired: () -> Unit,
    /** A submit succeeded; its form's actions may have started livechat on the server. */
    private val onSubmitted: () -> Unit,
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

    fun onRemoveFile(
        partId: String,
        key: String
    ) {
        uiStateHolder.updateForm(partId) { it.setFile(file = null, key = key) }
    }

    fun onRemoveTicketAttachment(partId: String) {
        uiStateHolder.updateForm(partId) { it.setTicketAttachment(null) }
    }

    /**
     * Reads and encodes the photo [open] streams on [encodeDispatcher] and attaches the result to
     * [destination], or shows [FormBannerError.PhotoFailed] when it cannot be used. Sets
     * [ai.askdiverge.ui.state.ChatbotUiState.isEncodingAttachment] while encoding.
     */
    fun onPhotoPicked(
        destination: FormPhotoDestination,
        open: () -> InputStream?
    ) {
        val partId = when (destination) {
            is FormPhotoDestination.FormField -> destination.partId
            is FormPhotoDestination.TicketAttachment -> destination.partId
        }
        // The draft is gone when the process died while the picker was open.
        val model = uiStateHolder.uiState.value.formModels[partId] ?: run {
            uiStateHolder.onPhotoAttachFailed()
            return
        }
        val (filename, maxDecodedBytes) = when (destination) {
            is FormPhotoDestination.FormField ->
                "${destination.key}.jpg" to OutgoingAttachment.MAX_ACTION_DECODED_BYTES
            is FormPhotoDestination.TicketAttachment -> {
                val cap = model.form.maxAttachmentSizeBytes
                    ?: OutgoingAttachment.MAX_ACTION_DECODED_BYTES
                "attachment.jpg" to cap
            }
        }

        uiStateHolder.setEncodingAttachment(true)
        scope.launch {
            val pending = try {
                withContext(encodeDispatcher) {
                    ImageAttachmentEncoder.encode(
                        open = open,
                        filename = filename,
                        maxDecodedBytes = maxDecodedBytes
                    )
                }
            } finally {
                uiStateHolder.setEncodingAttachment(false)
            }
            if (pending == null) {
                uiStateHolder.updateForm(partId) { it.markPhotoFailed() }
                return@launch
            }
            when (destination) {
                is FormPhotoDestination.FormField ->
                    uiStateHolder.updateForm(partId) {
                        it.setFile(file = pending, key = destination.key)
                    }
                is FormPhotoDestination.TicketAttachment ->
                    uiStateHolder.updateForm(partId) { it.setTicketAttachment(pending) }
            }
        }
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
        if (state.isEncodingAttachment) return

        val (validated, isValid) = model.validate(offersFileFields = offersAttachments)
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
                    onSubmitted()
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
