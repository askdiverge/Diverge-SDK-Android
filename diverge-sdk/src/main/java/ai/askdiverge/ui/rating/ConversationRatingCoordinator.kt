package ai.askdiverge.ui.rating

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.usecase.RateConversationUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.livechat.LivechatRatingModel
import ai.askdiverge.ui.state.ChatConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Offers a conversation rating when the visitor dismisses the sheet after sending a message, and
 * posts it to `POST /chat/rate`. Livechat CSAT stays on [ai.askdiverge.ui.livechat.LivechatRatingCoordinator].
 */
internal class ConversationRatingCoordinator(
    private val uiStateHolder: ChatbotUiStateHolder,
    private val rateConversationUseCase: RateConversationUseCase,
    private val scope: CoroutineScope,
    private val isRatingEnabled: () -> Boolean,
    private val hasVisitorMessage: () -> Boolean,
    private val isLivechatBlocking: () -> Boolean,
    private val livechatOwnsClose: () -> Boolean,
    private val onSessionExpired: () -> Unit,
    private val onReadyToDismiss: () -> Unit
) {

    /** A successful rate this ViewModel lifetime; Skip does not set this. */
    private var hasRated = false

    private var submitJob: Job? = null

    /**
     * @return true when the host may dismiss now; false when the rating dialog was shown instead
     * (or is already showing).
     */
    fun onDismissRequested(): Boolean {
        if (uiStateHolder.uiState.value.conversationRating != null) return false
        if (!shouldOffer()) return true
        begin()
        return false
    }

    fun selectScore(score: Int) {
        val current = uiStateHolder.uiState.value.conversationRating ?: return
        val next = current.pick(score)
        if (next == null) {
            send(score = score, feedback = null)
            return
        }
        if (next !== current) {
            uiStateHolder.setConversationRating(next)
        }
    }

    fun setComment(value: String) {
        uiStateHolder.updateConversationRating { it.withComment(value) }
    }

    fun send() {
        val current = uiStateHolder.uiState.value.conversationRating ?: return
        val score = current.score ?: return
        if (!current.needsComment) return
        send(score = score, feedback = current.comment)
    }

    fun send(
        score: Int,
        feedback: String?
    ) {
        val current = uiStateHolder.uiState.value.conversationRating ?: return
        val submitting = current.beginSubmitting(score = score, feedback = feedback) ?: return
        uiStateHolder.setConversationRating(submitting)
        submitJob?.cancel()
        submitJob = scope.launch {
            rateConversationUseCase(rating = score, feedback = feedback)
                .onSuccess {
                    hasRated = true
                    dropModel()
                    onReadyToDismiss()
                }
                .onFailure { error ->
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> {
                            dropModel()
                            onSessionExpired()
                        }
                        else -> {
                            if (uiStateHolder.uiState.value.conversationRating != null) {
                                uiStateHolder.setConversationRating(submitting.markFailed())
                            }
                        }
                    }
                }
        }
    }

    /** Skip / dialog dismiss: no POST, then host dismiss. */
    fun skip() {
        if (uiStateHolder.uiState.value.conversationRating?.isSubmitting == true) return
        dropModel()
        onReadyToDismiss()
    }

    /** Clears draft and [hasRated] (reset / delete / session expiry). */
    fun clear() {
        submitJob?.cancel()
        submitJob = null
        hasRated = false
        dropModel()
    }

    private fun shouldOffer(): Boolean {
        if (!isRatingEnabled()) return false
        if (hasRated) return false
        if (!hasVisitorMessage()) return false
        if (isLivechatBlocking()) return false
        if (livechatOwnsClose()) return false
        if (uiStateHolder.uiState.value.session.connectionState !is ChatConnectionState.Ready) {
            return false
        }
        return true
    }

    private fun begin() {
        uiStateHolder.setConversationRating(LivechatRatingModel())
    }

    private fun dropModel() {
        if (uiStateHolder.uiState.value.conversationRating != null) {
            uiStateHolder.setConversationRating(null)
        }
    }
}
