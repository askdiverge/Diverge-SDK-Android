package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class ExportMyDataUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    /**
     * Downloads the visitor's GDPR export as raw JSON bytes. Does not clear the session token.
     */
    suspend operator fun invoke(): Result<ByteArray> = repository.exportMyData()
}
