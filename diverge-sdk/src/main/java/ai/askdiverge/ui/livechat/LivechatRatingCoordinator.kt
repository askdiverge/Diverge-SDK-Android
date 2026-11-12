package ai.askdiverge.ui.livechat

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Asks the visitor to rate a livechat session that ended while this chat was watching, and sends
 * the rating. The ViewModel passes every livechat snapshot and whether the chat is on screen; the
 * prompt shows while [ChatbotUiStateHolder] holds a [LivechatRatingModel].
 */
internal class LivechatRatingCoordinator(
    private val uiStateHolder: ChatbotUiStateHolder,
    private val submitLivechatFeedbackUseCase: SubmitLivechatFeedbackUseCase,
    private val scope: CoroutineScope,
    private val onSessionExpired: () -> Unit,
    private val refresh: suspend () -> Unit
) {

    /** This screen saw the current session close, so its feedback may be asked for. */
    private var sawClose = false

    /** The visitor rated or skipped this close. */
    private var isDone = false

    /** The latest session is closed with feedback still to give. */
    private var hasPendingFeedback = false

    /** The chat is visible. */
    private var isVisible = true

    /**
     * Bumped when a new session replaces the closed one, so a rating sent for the old one can't
     * end or close anything after it.
     */
    private val epoch = AtomicInteger(0)

    /**
     * Follows a livechat snapshot. The prompt opens when a close seen live has feedback pending,
     * and goes away when a snapshot shows the feedback no longer pending or a new session starts.
     */
    fun update(
        previousStatus: LivechatState.Status,
        state: LivechatState,
        isRestored: Boolean
    ) {
        hasPendingFeedback =
            state.status == LivechatState.Status.CLOSED &&
                state.feedback.status == LivechatState.Feedback.Status.PENDING
        if (state.status != LivechatState.Status.CLOSED) {
            sawClose = false
            isDone = false
            epoch.incrementAndGet()
            dropModel()
            return
        }
        if (previousStatus.isInSession && !isRestored) {
            sawClose = true
        }
        if (hasPendingFeedback) {
            offerIfReady()
        } else {
            val model = uiStateHolder.uiState.value.livechat.rating
            if (model?.isSubmitting != true) {
                dropModel()
            }
        }
    }

    /** Follows the chat going on or off screen. A close seen while off screen is asked once back. */
    fun setVisible(visible: Boolean) {
        isVisible = visible
        offerIfReady()
    }

    fun selectScore(score: Int) {
        val current = uiStateHolder.uiState.value.livechat.rating ?: return
        val next = current.pick(score)
        if (next == null) {
            send(score = score, feedback = null)
            return
        }
        if (next !== current) {
            uiStateHolder.setLivechatRating(next)
        }
    }

    fun setComment(value: String) {
        uiStateHolder.updateLivechatRating { it.withComment(value) }
    }

    fun send() {
        val current = uiStateHolder.uiState.value.livechat.rating ?: return
        val score = current.score ?: return
        if (!current.needsComment) return
        val trimmed = current.comment.trim()
        send(score = score, feedback = trimmed.takeIf { it.isNotEmpty() })
    }

    /**
     * Sends the rating. A 409 means the session holds a rating already, so the prompt closes. Any
     * other refused rating stays on the prompt for another try, and the state is read again in
     * case the server already holds one. The result of a rating sent for a session a new one has
     * since replaced is dropped.
     */
    fun send(
        score: Int,
        feedback: String?
    ) {
        val current = uiStateHolder.uiState.value.livechat.rating ?: return
        val submitting = current.beginSubmitting(score = score, feedback = feedback) ?: return
        uiStateHolder.setLivechatRating(submitting)
        val submitEpoch = epoch.get()
        scope.launch {
            val result = submitLivechatFeedbackUseCase(rating = score, feedback = feedback)
            if (submitEpoch != epoch.get()) return@launch
            result
                .onSuccess {
                    if (submitEpoch != epoch.get()) return@onSuccess
                    isDone = true
                    hasPendingFeedback = false
                    dropModel()
                }
                .onFailure { error ->
                    if (submitEpoch != epoch.get()) return@onFailure
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> {
                            dropModel()
                            onSessionExpired()
                        }
                        is LivechatRequestException.Conflict -> {
                            isDone = true
                            hasPendingFeedback = false
                            dropModel()
                        }
                        else -> {
                            uiStateHolder.setLivechatRating(submitting.markFailed())
                            refresh()
                        }
                    }
                }
        }
    }

    /** The visitor skipped the prompt; nothing is sent and this close isn't asked about again. */
    fun skip() {
        isDone = true
        dropModel()
    }

    /** Drops the draft (teardown / reset). */
    fun clear() {
        sawClose = false
        isDone = false
        hasPendingFeedback = false
        epoch.incrementAndGet()
        dropModel()
    }

    private fun offerIfReady() {
        if (!isVisible ||
            !hasPendingFeedback ||
            !sawClose ||
            isDone ||
            uiStateHolder.uiState.value.livechat.rating != null
        ) {
            return
        }
        uiStateHolder.setLivechatRating(LivechatRatingModel())
    }

    private fun dropModel() {
        if (uiStateHolder.uiState.value.livechat.rating != null) {
            uiStateHolder.setLivechatRating(null)
        }
    }
}
