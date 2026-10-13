package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import kotlinx.coroutines.flow.Flow

internal class GetSessionInvalidationsUseCase internal constructor(private val repository: ChatbotRepository) {
    operator fun invoke(): Flow<Unit> = repository.getSessionInvalidations()
}
