package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

private const val PARAGRAPH_TYPE = "paragraph"
private const val BULLET_LIST_TYPE = "bullet_list"

@Keep
@JsonClass(generateAdapter = false)
internal enum class BlockTypeRemote(val type: String) {
    @Json(name = PARAGRAPH_TYPE)
    RICH_TEXT_PARAGRAPH_BLOCK(PARAGRAPH_TYPE),

    @Json(name = BULLET_LIST_TYPE)
    RICH_TEXT_BULLET_LIST_BLOCK(BULLET_LIST_TYPE)
}
