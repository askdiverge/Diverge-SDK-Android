package ai.askdiverge.data.model.message.incoming.part.text.span

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = false)
internal enum class RichTextSpanTypeRemote {
    @Json(name = "text")
    TEXT,

    @Json(name = "bold")
    BOLD,

    @Json(name = "strike")
    STRIKE,

    @Json(name = "link")
    LINK
}
