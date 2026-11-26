package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.metadata.SessionMetadataPatch

internal class PatchSessionMetadataUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    /**
     * Sends [patch] to the visitor's session. A response outside 2xx fails with
     * [ai.askdiverge.domain.exception.SessionMetadataRequestException] carrying its status; the
     * visitor token is kept whatever the answer.
     */
    suspend operator fun invoke(patch: SessionMetadataPatch): Result<Unit> =
        repository.patchSessionMetadata(patch)
}
