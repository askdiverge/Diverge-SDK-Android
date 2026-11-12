package ai.askdiverge.ui.form

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.usecase.FetchFormUseCase
import ai.askdiverge.domain.usecase.GetChatSessionUseCase
import ai.askdiverge.domain.usecase.PatchFormValuesUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Loads, shows and saves the livechat waiting-room form while the visitor is queued.
 *
 * The ViewModel passes every livechat status and the chat's on-screen visibility; the card shows
 * while [ChatbotUiStateHolder] holds a [WaitingFormModel].
 */
internal class WaitingFormCoordinator(
    private val uiStateHolder: ChatbotUiStateHolder,
    private val fetchFormUseCase: FetchFormUseCase,
    private val getChatSessionUseCase: GetChatSessionUseCase,
    private val patchFormValuesUseCase: PatchFormValuesUseCase,
    private val scope: CoroutineScope,
    private val delayMs: suspend (Long) -> Unit,
    private val onSessionExpired: () -> Unit
) {

    private var formId: String? = null
    private var isWaiting = false
    private var isVisible = true
    private var loadJob: Job? = null
    /** This wait shows no card: the form has no field the card can show, or it can't be loaded. */
    private var hasNoCard = false
    /** Bumped whenever the card is cleared, so a load started before can't install a stale card. */
    private val generation = AtomicInteger(0)

    /** Takes the `livechat_waiting` form id from `/config`; null when none is configured. */
    fun configure(formId: String?) {
        this.formId = formId?.takeIf { it.isNotBlank() }
    }

    /**
     * Follows the livechat status. While queued the card loads, and a transient failure is
     * retried with backoff while the chat is on screen; any other status clears it.
     */
    fun statusChanged(status: LivechatState.Status) {
        if (status != LivechatState.Status.WAITING) {
            clear()
            return
        }
        isWaiting = true
        loadIfNeeded()
    }

    /** Pauses retries while the chat is off screen. */
    fun setVisible(visible: Boolean) {
        isVisible = visible
        loadIfNeeded()
    }

    fun onValueChanged(
        key: String,
        value: String
    ) {
        uiStateHolder.updateWaitingForm { it.setValue(value = value, key = key) }
    }

    /**
     * Saves through `PATCH /forms/{id}/values`. A 422 marks the fields; a 409 drops the card; a
     * 401 ends the session; any other failure shows the card's banner.
     */
    fun save() {
        val current = uiStateHolder.uiState.value.livechat.waitingForm ?: return
        if (!current.canSave) return
        val (validated, isValid) = current.validate()
        uiStateHolder.setWaitingForm(validated)
        if (!isValid) return
        val saving = validated.beginSaving() ?: return
        uiStateHolder.setWaitingForm(saving)
        val gen = generation.get()
        scope.launch {
            val result = patchFormValuesUseCase(saving.formId, saving.valuesForPatch())
            if (gen != generation.get()) return@launch
            result
                .onSuccess { session ->
                    if (gen != generation.get()) return@onSuccess
                    uiStateHolder.setWaitingForm(saving.markSaved(session))
                }
                .onFailure { error ->
                    if (gen != generation.get()) return@onFailure
                    saveFailed(saving, error)
                }
        }
    }

    /** Drops the card and stops a load in flight. */
    fun clear() {
        isWaiting = false
        val hasModel = uiStateHolder.uiState.value.livechat.waitingForm != null
        if (!hasModel && loadJob == null && !hasNoCard) return
        generation.incrementAndGet()
        loadJob?.cancel()
        loadJob = null
        hasNoCard = false
        if (hasModel) uiStateHolder.setWaitingForm(null)
    }

    private fun saveFailed(
        model: WaitingFormModel,
        error: Throwable
    ) {
        when (error) {
            is ChatbotException.SessionExpired,
            is ChatbotException.NoTokenAvailable -> {
                uiStateHolder.setWaitingForm(model.markFailed())
                onSessionExpired()
            }
            is FormRequestException.Conflict -> clear()
            is FormRequestException.Validation -> {
                uiStateHolder.setWaitingForm(model.applyServerErrors(error.params))
            }
            else -> uiStateHolder.setWaitingForm(model.markFailed())
        }
    }

    private fun loadIfNeeded() {
        val id = formId
        if (!isWaiting ||
            !isVisible ||
            uiStateHolder.uiState.value.livechat.waitingForm != null ||
            loadJob?.isActive == true ||
            hasNoCard ||
            id == null
        ) {
            return
        }
        val gen = generation.get()
        loadJob = scope.launch {
            var delay = FIRST_RETRY_MS
            while (attempt(formId = id, generation = gen)) {
                try {
                    delayMs(delay)
                } catch (_: CancellationException) {
                    return@launch
                }
                delay = minOf(delay * 2, MAX_RETRY_MS)
            }
        }
    }

    /**
     * One load attempt; true when it should be retried. An attempt for a cleared card, or while
     * the chat is off screen, ends the load, and coming back on screen starts a new one.
     */
    private suspend fun attempt(
        formId: String,
        generation: Int
    ): Boolean {
        if (this.generation.get() != generation) return false
        if (!isVisible) {
            loadJob = null
            return false
        }
        val result = fetchModel(formId)
        if (this.generation.get() != generation) return false
        when (result) {
            is LoadResult.Loaded -> uiStateHolder.setWaitingForm(result.model)
            LoadResult.NoCard -> hasNoCard = true
            LoadResult.Expired -> {
                loadJob = null
                onSessionExpired()
                return false
            }
            LoadResult.Retry -> return true
        }
        loadJob = null
        return false
    }

    private suspend fun fetchModel(formId: String): LoadResult = coroutineScope {
        val definitionDeferred = async { fetchFormUseCase(formId) }
        val sessionDeferred = async { getChatSessionUseCase() }
        val definition = definitionDeferred.await()
        val session = sessionDeferred.await()
        val form = definition.getOrNull()
        val state = session.getOrNull()
        if (form != null && state != null) {
            val model = WaitingFormModel.create(definition = form, sessionValues = state.values)
            return@coroutineScope if (model == null) LoadResult.NoCard else LoadResult.Loaded(model)
        }
        val errors = listOfNotNull(definition.exceptionOrNull(), session.exceptionOrNull())
        if (errors.any { it is ChatbotException.SessionExpired || it is ChatbotException.NoTokenAvailable }) {
            return@coroutineScope LoadResult.Expired
        }
        if (errors.any { it.isTransientLoadFailure() }) LoadResult.Retry else LoadResult.NoCard
    }

    private sealed interface LoadResult {
        data class Loaded(val model: WaitingFormModel) : LoadResult
        data object NoCard : LoadResult
        data object Expired : LoadResult
        data object Retry : LoadResult
    }

    companion object {
        const val FIRST_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 30_000L
    }
}

private fun Throwable.isTransientLoadFailure(): Boolean =
    this !is FormRequestException.NotFound &&
        this !is FormRequestException.Conflict &&
        this !is FormRequestException.Validation &&
        this !is ChatbotException.SessionExpired &&
        this !is ChatbotException.NoTokenAvailable
