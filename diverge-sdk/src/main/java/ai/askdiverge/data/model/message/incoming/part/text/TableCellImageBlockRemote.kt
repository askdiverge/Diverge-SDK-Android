package ai.askdiverge.data.model.message.incoming.part.text

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class TableCellImageBlockRemote(
    val url: String,
    val thumbnail_url: String?,
    val alt: String?
) : TableCellBlockRemote
