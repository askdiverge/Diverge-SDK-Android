package ai.askdiverge.data.model.form

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body for `PATCH /api/v1/chat/forms/{form_id}/values`.
 *
 * Only provided keys are updated; omitted keys keep the stored session value.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatformvaluespatchrequest)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatFormValuesPatchRequestRemote(
    val values: Map<String, String>
)
