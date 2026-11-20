package ai.askdiverge.data.model.message.incoming.part.suggestion

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * One card inside a `suggestions` part.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/suggestioncard)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class SuggestionCardRemote(
    val id: String,
    val title: String,
    val description: String? = null,
    val image_url: String,
    val prompt_text: String
)
