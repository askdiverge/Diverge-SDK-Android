package ai.askdiverge.data.model.message.outgoing

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * Body part for an outgoing `ImageInput` on `/messages` or `/livechat/messages`.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/imageinput)
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class OutgoingImagePartRemote(
    /** Bare base64 image bytes. Wire name [OutgoingMessageWire.DATA]. */
    val data: String,
    /** MIME type, for example `image/jpeg`. Wire name [OutgoingMessageWire.MIME]. */
    val mime: String,
    /**
     * Optional original filename. Wire name [OutgoingMessageWire.FILENAME]. Null when the composer
     * pick had no name; Moshi omits null properties from the JSON body.
     */
    val filename: String? = null
) : OutgoingMessagePartRemote
