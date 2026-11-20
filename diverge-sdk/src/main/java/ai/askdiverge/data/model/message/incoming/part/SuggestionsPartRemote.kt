package ai.askdiverge.data.model.message.incoming.part

import ai.askdiverge.data.model.message.incoming.part.suggestion.SuggestionCardRemote
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Authoritative `suggestions` message part.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/suggestionscontent)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class SuggestionsPartRemote(
    val part_id: String,
    val suggestions: List<SuggestionCardRemote>
) : MessagePartRemote
