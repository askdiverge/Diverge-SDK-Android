package ai.askdiverge.data.model.message.incoming.part.text

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class RichTextBulletListBlockRemote(val items: List<RichTextBulletListItemRemote>) :
    RichTextBlockRemote,
    TableCellBlockRemote
