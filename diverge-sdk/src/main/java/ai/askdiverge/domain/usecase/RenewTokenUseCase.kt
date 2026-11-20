package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class RenewTokenUseCase internal constructor(private val repository: ChatbotRepository) {
    /** See [ChatbotRepository.renewToken]. */
    suspend operator fun invoke(beforeReplacing: suspend () -> Unit): Result<Unit> =
        repository.renewToken(beforeReplacing)
}
