package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Wire `request_image_upload` message part.
 *
 * Arrives as a full `part` / history / `done` only — never via `part_delta`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/requestimageuploadmarker)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class RequestImageUploadRemote(
    val part_id: String,
    val accepted_types: List<String>,
    val max_size_bytes: Int
) : MessagePartRemote
