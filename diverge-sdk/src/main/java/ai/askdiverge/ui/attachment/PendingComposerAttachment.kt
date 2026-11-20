package ai.askdiverge.ui.attachment

import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * A visitor-picked photo ready as a composer chip and an outgoing `ImageInput` part.
 *
 * [previewUrl] is a small JPEG `data:` URL for the chip, so the chip neither holds nor decodes a
 * second copy of the full [image] payload.
 */
@Immutable
internal data class PendingComposerAttachment(
    val id: String = UUID.randomUUID().toString(),
    val image: OutgoingImageInput,
    val previewUrl: String
)
