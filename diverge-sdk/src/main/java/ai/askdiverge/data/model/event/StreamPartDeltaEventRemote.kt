package ai.askdiverge.data.model.event

import androidx.annotation.Keep
import ai.askdiverge.data.model.event.delta.DeltaRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class StreamPartDeltaEventRemote(
    val part_id: String,
    val delta: DeltaRemote
) : StreamEventRemote
