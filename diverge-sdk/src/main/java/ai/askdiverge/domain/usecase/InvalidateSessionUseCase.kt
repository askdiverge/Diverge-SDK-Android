package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class InvalidateSessionUseCase internal constructor(private val repository: ChatbotRepository) {
    operator fun invoke() = repository.invalidateSession()
}
