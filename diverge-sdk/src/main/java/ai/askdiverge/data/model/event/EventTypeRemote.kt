package ai.askdiverge.data.model.event

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlin.collections.get

@Keep
@JsonClass(generateAdapter = false)
internal enum class EventTypeRemote(val type: String) {
    @Json(name = "status")
    STATUS("status"),

    @Json(name = "part_delta")
    PART_DELTA("part_delta"),

    @Json(name = "part")
    PART("part"),

    @Json(name = "done")
    DONE("done"),

    @Json(name = "error")
    ERROR("error");

    companion object {
        private val typeMap = entries.associateBy { it.type }

        fun fromType(type: String?): EventTypeRemote? = typeMap[type]
    }
}
