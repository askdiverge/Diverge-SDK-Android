package ai.askdiverge.data.model.message.incoming.part

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class ImagePartRemote(val url: String) : MessagePartRemote
