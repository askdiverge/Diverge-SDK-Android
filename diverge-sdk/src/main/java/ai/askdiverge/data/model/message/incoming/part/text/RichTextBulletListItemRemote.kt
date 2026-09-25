package ai.askdiverge.data.model.message.incoming.part.text

import androidx.annotation.Keep
import ai.askdiverge.data.model.message.incoming.part.text.span.RichTextSpanRemote
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class RichTextBulletListItemRemote(val spans: List<RichTextSpanRemote>)
