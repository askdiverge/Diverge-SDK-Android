package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.text.RichTextBlockRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class RichTextPartRemote(
    val part_id: String,
    val blocks: List<RichTextBlockRemote>
) : MessagePartRemote
