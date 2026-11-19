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
    private val ratingEnabled: Boolean,
    private val hasVisitorMessage: () -> Boolean,
    private val isLivechatBlocking: () -> Boolean,
    private val livechatOwnsClose: () -> Boolean,
    private val onSessionExpired: () -> Unit,
    private val onReadyToDismiss: () -> Unit
) {

    /** The visitor rated or skipped this ViewModel lifetime, so the prompt is not offered again. */
    private var isDone = false

    private var submitJob: Job? = null

    /** The draft the visitor backed out of, comment included; the next dismiss shows it again. */
    private var parkedDraft: LivechatRatingModel? = null

    private val prompt = RatingPrompt(
        read = { uiStateHolder.uiState.value.conversationRating },
        write = uiStateHolder::setConversationRating,
        submit = ::send
    )

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
        prompt.selectScore(score)
    }

    fun setComment(value: String) {
        prompt.setComment(value)
    }

    fun send() {
        prompt.send()
    }

    fun send(
        score: Int,
        feedback: String?
    ) {
        val submitting = prompt.beginSubmitting(score = score, feedback = feedback) ?: return
        submitJob?.cancel()
        submitJob = scope.launch {
            val result = rateConversationUseCase(rating = score, feedback = feedback)
            // The visitor left the prompt while the rating was on its way, and the host was told
            // to dismiss then.
            val isPromptOpen = uiStateHolder.uiState.value.conversationRating != null
            result
                .onSuccess {
                    isDone = true
                    prompt.drop()
                    if (isPromptOpen) onReadyToDismiss()
                }
                .onFailure { error ->
                    when (error) {
                        is ChatbotException.SessionExpired,
                        is ChatbotException.NoTokenAvailable -> {
                            // Expiry clears this coordinator, which cancels submitJob; detach
                            // this job first so the rest of this branch still runs.
                            submitJob = null
                            prompt.drop()
                            if (isPromptOpen) onReadyToDismiss()
                            onSessionExpired()
                        }
                        else -> {
                            if (isPromptOpen) {
                                uiStateHolder.setConversationRating(submitting.markFailed())
                            }
                        }
                    }
                }
        }
    }

    /**
     * Skip: no prompt again this ViewModel lifetime, then host dismiss. Nothing is posted; a
     * rating already on its way finishes in the background.
     */
    fun skip() {
        isDone = true
        prompt.drop()
        onReadyToDismiss()
    }

    /**
     * Back or a tap beside the dialog: closes the prompt, keeps the chat open and the draft. Once
     * a rating is on its way the visitor has rated, so the chat closes as it does on [skip].
     */
    fun cancel() {
        val current = uiStateHolder.uiState.value.conversationRating ?: return
        if (current.isSubmitting) {
            skip()
            return
        }
        parkedDraft = current
        prompt.drop()
    }

    /**
     * Clears the draft, a parked one and [isDone]: the conversation was reset or deleted, or its
     * session expired or was replaced by the host.
     */
    fun clear() {
        submitJob?.cancel()
        submitJob = null
        isDone = false
        parkedDraft = null
        prompt.drop()
    }

    private fun shouldOffer(): Boolean {
        if (!ratingEnabled) return false
        if (isDone) return false
        if (!hasVisitorMessage()) return false
        if (isLivechatBlocking()) return false
        if (livechatOwnsClose()) return false
        val state = uiStateHolder.uiState.value
        if (state.session.connectionState !is ChatConnectionState.Ready) return false
        // The conversation on screen is being replaced.
        if (state.isResettingConversation || state.isDeletingChatData) return false
        return true
    }

    private fun begin() {
        uiStateHolder.setConversationRating(parkedDraft ?: LivechatRatingModel())
        parkedDraft = null
    }
}
