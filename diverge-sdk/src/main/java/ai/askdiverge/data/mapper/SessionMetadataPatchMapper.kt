package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.session.ChatSessionPatchRequestRemote
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch

/** The `PATCH /chat/session` body for this patch: one field set, the other left out. */
internal fun SessionMetadataPatch.mapToRemote(): ChatSessionPatchRequestRemote = when (this) {
    is SessionMetadataPatch.Metadata -> ChatSessionPatchRequestRemote(
        metadata = values,
        signed_metadata = null
    )
    is SessionMetadataPatch.Signed -> ChatSessionPatchRequestRemote(
        metadata = null,
        signed_metadata = token
    )
}
