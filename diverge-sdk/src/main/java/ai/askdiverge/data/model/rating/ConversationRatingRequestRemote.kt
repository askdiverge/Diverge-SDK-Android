package ai.askdiverge.data.model.rating

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/rate`, the inline body of `Rate.rate` in the Chatbot API TypeSpec.
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ConversationRatingRequestRemote(
    /** Rating from 1 (worst) to 5 (best). Wire name [ConversationRatingWire.RATING]. */
    @Json(name = ConversationRatingWire.RATING)
    val rating: Int,
    /**
     * Optional free-text feedback. Wire name [ConversationRatingWire.FEEDBACK]. Null when blank
     * after trim; Moshi omits null properties from the JSON body.
     */
    @Json(name = ConversationRatingWire.FEEDBACK)
    val feedback: String? = null
)
