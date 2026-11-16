package ai.askdiverge.data.model.rating

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body for `POST /api/v1/chat/rate`, the inline body of `Rate.rate` in the Chatbot API TypeSpec.
 * Wire names `rating` and `feedback`; tests pin the encoded body.
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ConversationRatingRequestRemote(
    /** Rating from 1 (worst) to 5 (best). */
    val rating: Int,
    /**
     * Optional free-text feedback. Null when blank after trim; Moshi omits null properties from the
     * JSON body.
     */
    val feedback: String? = null
)
