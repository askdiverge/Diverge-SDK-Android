package ai.askdiverge.ui.livechat

import androidx.compose.runtime.Immutable

/**
 * Draft and submission state for the livechat CSAT prompt.
 *
 * Two steps: pick a score (1–5), then optionally type feedback when the score is ≤ 3. Scores 4–5
 * skip the feedback step and submit immediately.
 */
@Immutable
internal data class LivechatRatingModel(
    val phase: Phase = Phase.Picking,
    val comment: String = ""
) {

    sealed interface Phase {
        data object Picking : Phase
        data class Feedback(val score: Int) : Phase
        data class Submitting(
            val score: Int,
            val feedback: String?
        ) : Phase
        data class Failed(
            val score: Int,
            val feedback: String?
        ) : Phase
    }

    val isSubmitting: Boolean get() = phase is Phase.Submitting

    val score: Int?
        get() = when (val current = phase) {
            Phase.Picking -> null
            is Phase.Feedback -> current.score
            is Phase.Submitting -> current.score
            is Phase.Failed -> current.score
        }

    /** True once a score of 1–3 has been picked (feedback, submitting, or failed on that score). */
    val needsComment: Boolean
        get() {
            val value = score ?: return false
            if (value > FEEDBACK_THRESHOLD) return false
            return phase !is Phase.Picking
        }

    /**
     * Advances from the scale. Returns the model that needs the free-text step (1–3), or null when
     * the caller should submit immediately (4–5). Unchanged when already submitting or out of range.
     */
    fun pick(score: Int): LivechatRatingModel? {
        if (score !in 1..5 || isSubmitting) return this
        if (score <= FEEDBACK_THRESHOLD) {
            return copy(phase = Phase.Feedback(score), comment = "")
        }
        return null
    }

    /** Marks the model as in-flight. Returns null if already submitting. */
    fun beginSubmitting(
        score: Int,
        feedback: String?
    ): LivechatRatingModel? {
        if (isSubmitting) return null
        return copy(phase = Phase.Submitting(score = score, feedback = feedback))
    }

    fun markFailed(): LivechatRatingModel {
        val (score, feedback) = when (val current = phase) {
            is Phase.Submitting -> current.score to current.feedback
            is Phase.Failed -> current.score to current.feedback
            is Phase.Feedback -> current.score to comment
            Phase.Picking -> return this
        }
        return copy(phase = Phase.Failed(score = score, feedback = feedback))
    }

    /** After a failure, editing the comment returns to the feedback step. */
    fun editingAfterFailure(): LivechatRatingModel {
        val failed = phase as? Phase.Failed ?: return this
        return copy(
            phase = Phase.Feedback(score = failed.score),
            comment = failed.feedback.orEmpty()
        )
    }

    fun withComment(value: String): LivechatRatingModel {
        val next = if (phase is Phase.Failed) editingAfterFailure() else this
        return next.copy(comment = value)
    }

    companion object {
        /** Scores at or below this threshold open the free-text feedback step. */
        const val FEEDBACK_THRESHOLD = 3
    }
}
