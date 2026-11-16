package ai.askdiverge.ui.livechat

import ai.askdiverge.domain.exception.ChatbotException
import ai.askdiverge.domain.exception.LivechatRequestException
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.usecase.SubmitLivechatFeedbackUseCase
import ai.askdiverge.ui.ChatbotUiStateHolder
import ai.askdiverge.ui.rating.RatingPrompt
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

    /** The latest closed session already has feedback on the wire. */
    private var closedWithSubmittedFeedback = false

    /** The visitor has written to the assistant since this screen saw the session close. */
    private var messagedSinceClose = false

    /** The chat is visible. */
    private var isVisible = true

    /**
     * Bumped when a new session replaces the closed one, so a rating sent for the old one can't
     * end or close anything after it.
     */
    private val epoch = AtomicInteger(0)

    private val prompt = RatingPrompt(
        read = { uiStateHolder.uiState.value.livechat.rating },
        write = uiStateHolder::setLivechatRating,
        submit = ::send
    )

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
        closedWithSubmittedFeedback =
            state.status == LivechatState.Status.CLOSED &&
                state.feedback.status == LivechatState.Feedback.Status.SUBMITTED
        if (state.status != LivechatState.Status.CLOSED) {
            sawClose = false
            isDone = false
            closedWithSubmittedFeedback = false
            epoch.incrementAndGet()
            dropModel()
            return
        }
        if (previousStatus.isInSession && !isRestored) {
            sawClose = true
            messagedSinceClose = false
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
        prompt.selectScore(score)
    }

    fun setComment(value: String) {
        prompt.setComment(value)
    }

    fun send() {
        prompt.send()
    }

    /**
     * Sends the rating. A 409 means the session holds a rating already, so the prompt closes. Any
     * other refused rating stays on the prompt for another try, and the state is read again in
     * case the server already holds one; if a snapshot showed the feedback no longer pending while
     * it was sent, the prompt closes instead. The result of a rating sent for a session a new one
     * has since replaced, or for a prompt that has since gone, is dropped.
     */
    fun send(
        score: Int,
        feedback: String?
    ) {
        prompt.beginSubmitting(score = score, feedback = feedback) ?: return
        submit(score = score, feedback = feedback)
    }

    private fun submit(
        score: Int,
        feedback: String?
    ) {
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
                        else -> when {
                            uiStateHolder.uiState.value.livechat.rating == null -> Unit
                            !hasPendingFeedback -> {
                                isDone = true
                                dropModel()
                            }
                            else -> {
                                uiStateHolder.uiState.value.livechat.rating
                                    ?.let { uiStateHolder.setLivechatRating(it.markFailed()) }
                                refresh()
                            }
                        }
                    }
                }
        }
    }

    /**
     * The visitor dismissed the prompt, which is not asked about again for this close. A score
     * picked for the comment step is sent without the comment, and a rating already on its way
     * finishes in the background; with no score picked, nothing is sent.
     */
    fun skip() {
        val phase = uiStateHolder.uiState.value.livechat.rating?.phase
        isDone = true
        dropModel()
        if (phase is LivechatRatingModel.Phase.Feedback) {
            submit(score = phase.score, feedback = null)
        }
    }

    /** The visitor wrote to the assistant; a close seen before that no longer owns a dismiss. */
    fun onVisitorMessage() {
        messagedSinceClose = true
    }

    /**
     * True when livechat CSAT owns the close: its prompt is showing, or this screen saw the session
     * close, its feedback is pending, given or skipped, and the visitor has not written to the
     * assistant since. A session found already closed does not count.
     */
    fun suppressesConversationRating(): Boolean =
        uiStateHolder.uiState.value.livechat.rating != null ||
            (sawClose &&
                !messagedSinceClose &&
                (isDone || hasPendingFeedback || closedWithSubmittedFeedback))

    /** Drops the draft (teardown / reset). */
    fun clear() {
        sawClose = false
        isDone = false
        hasPendingFeedback = false
        closedWithSubmittedFeedback = false
        messagedSinceClose = false
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
        prompt.drop()
    }
}
