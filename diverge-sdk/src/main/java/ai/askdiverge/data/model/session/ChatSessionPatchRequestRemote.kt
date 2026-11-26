package ai.askdiverge.data.model.session

import ai.askdiverge.domain.model.metadata.JsonValue
import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body for `PATCH /api/v1/chat/session`, `ChatSessionPatchRequest` in the Chatbot API TypeSpec.
 * The SDK sets exactly one field per request; Moshi omits the `null` one from the JSON body.
 */
@Keep
@JsonClass(generateAdapter = true)
internal data class ChatSessionPatchRequestRemote(
    /**
     * Values to deep-merge into the session's metadata, keys as the host wrote them. A
     * [JsonValue.Null] inside is written as JSON `null`. Wire name [SessionMetadataWire.METADATA].
     */
    @Json(name = SessionMetadataWire.METADATA)
    val metadata: Map<String, JsonValue>?,
    /** The host-signed JWT. Wire name [SessionMetadataWire.SIGNED_METADATA]. */
    @Json(name = SessionMetadataWire.SIGNED_METADATA)
    val signed_metadata: String?
)
