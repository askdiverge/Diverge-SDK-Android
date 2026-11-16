package ai.askdiverge.ui.rating

import ai.askdiverge.ui.livechat.LivechatRatingModel

/**
 * The score, comment and send steps of a 1–5 rating prompt whose draft lives in one
 * [LivechatRatingModel] slot of the UI state. The owning coordinator decides what [submit] sends
 * and what its result does.
 */
internal class RatingPrompt(
    private val read: () -> LivechatRatingModel?,
    private val write: (LivechatRatingModel?) -> Unit,
    private val submit: (score: Int, feedback: String?) -> Unit
) {

    /** A 1–3 score opens the comment step; 4–5 submits at once. */
    fun selectScore(score: Int) {
        val current = read() ?: return
        val next = current.pick(score)
        if (next == null) {
            submit(score, null)
            return
        }
        if (next !== current) {
            write(next)
        }
    }

    fun setComment(value: String) {
        val current = read() ?: return
        write(current.withComment(value))
    }

    /** Submits a picked 1–3 score with its comment. */
    fun send() {
        val current = read() ?: return
        val score = current.score ?: return
        if (!current.needsComment) return
        submit(score, current.comment)
    }

    /**
     * Moves the draft to submitting and returns it, or null when there is no draft or one is
     * already in flight.
     */
    fun beginSubmitting(
        score: Int,
        feedback: String?
    ): LivechatRatingModel? {
        val current = read() ?: return null
        val submitting = current.beginSubmitting(score = score, feedback = feedback) ?: return null
        write(submitting)
        return submitting
    }

    fun drop() {
        if (read() != null) {
            write(null)
        }
    }
}
