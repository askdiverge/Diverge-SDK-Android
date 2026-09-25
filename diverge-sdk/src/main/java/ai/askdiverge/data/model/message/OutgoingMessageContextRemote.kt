package ai.askdiverge.data.model.message

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
internal data class OutgoingMessageContextRemote(val page: String)
