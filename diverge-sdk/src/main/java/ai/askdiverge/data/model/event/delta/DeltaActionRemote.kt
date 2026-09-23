package ai.askdiverge.data.model.event.delta

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The architecture for DeltaActionRemote is inconsistent
 * as [START_PART] acts as an indicator of the next received data for texts
 * products, etc. but does also contain extra data for tables.
 * This makes the class have multiple responsibilities depending on the
 * data received.
 */
@Keep
@JsonClass(generateAdapter = false)
internal enum class DeltaActionRemote {
    @Json(name = "start_part")
    START_PART,

    @Json(name = "end_part")
    END_PART,

    @Json(name = "start_block")
    START_BLOCK,

    @Json(name = "end_block")
    END_BLOCK,

    @Json(name = "append_text")
    APPEND_TEXT,

    @Json(name = "append_span")
    APPEND_SPAN,

    @Json(name = "append_item")
    APPEND_ITEM,

    @Json(name = "append_product")
    APPEND_PRODUCT,

    @Json(name = "append_row")
    APPEND_ROW,

    UNKNOWN
}
