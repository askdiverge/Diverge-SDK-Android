package ai.askdiverge.data.model.message.incoming.part.text.span

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class RichTextSpanRemote(
    val text: String,
    val type: RichTextSpanTypeRemote,
    val url: String? = null
)
