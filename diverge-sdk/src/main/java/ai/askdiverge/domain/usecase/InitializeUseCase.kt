package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.Config

internal class InitializeUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(): Result<Config> = repository.initialize()
}
