package ai.askdiverge.ui.attachment

import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * A visitor-picked photo ready as a composer chip and an outgoing `ImageInput` part.
 *
 * [displayUrl] is a `data:` URL for the optimistic bubble and the chip preview.
 */
@Immutable
internal data class PendingComposerAttachment(
    val id: String = UUID.randomUUID().toString(),
    val image: OutgoingImageInput,
    val displayUrl: String
)
