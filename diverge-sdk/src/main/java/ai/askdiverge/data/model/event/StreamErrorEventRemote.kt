package ai.askdiverge.data.model.event

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class StreamErrorEventRemote(
    val code: String,
    val message: String,
    val retryable: Boolean
) : StreamEventRemote
