package ai.askdiverge.data.model.message.incoming.part.table

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val PARAGRAPH_TYPE = "paragraph"
private const val BULLET_LIST_TYPE = "bullet_list"
private const val IMAGE_TYPE = "image"

@Keep
@JsonClass(generateAdapter = false)
internal enum class TableCellBlockTypeRemote(val type: String) {
    @Json(name = PARAGRAPH_TYPE)
    TABLE_CELL_PARAGRAPH_BLOCK(PARAGRAPH_TYPE),

    @Json(name = BULLET_LIST_TYPE)
    TABLE_CELL_BULLET_LIST_BLOCK(BULLET_LIST_TYPE),

    @Json(name = IMAGE_TYPE)
    TABLE_CELL_IMAGE_BLOCK(IMAGE_TYPE)
}
