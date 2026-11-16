package ai.askdiverge.data.model.rating

/**
 * Body literals for `POST /api/v1/chat/rate`, from the inline body of `Rate.rate` in the Chatbot
 * API TypeSpec. Tests pin these strings.
 */
internal object ConversationRatingWire {

    /** `POST /chat/rate` — the visitor's rating from 1 (worst) to 5 (best). */
    const val RATING = "rating"

    /** `POST /chat/rate` — optional free-text comment; omitted when the visitor wrote none. */
    const val FEEDBACK = "feedback"
}
