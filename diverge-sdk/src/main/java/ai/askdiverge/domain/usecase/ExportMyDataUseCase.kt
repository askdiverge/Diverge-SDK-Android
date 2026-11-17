package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.export.ExportDestination

internal class ExportMyDataUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    /**
     * Saves the visitor's GDPR export as JSON to [destination], which is deleted when the export
     * fails or is cancelled. Does not clear the session token.
     */
    suspend operator fun invoke(destination: ExportDestination): Result<Unit> =
        repository.exportMyData(destination)

    /** Removes [destination] without exporting, for a save that arrives when no export can run. */
    suspend fun discard(destination: ExportDestination): Result<Unit> =
        repository.discardExport(destination)
}
