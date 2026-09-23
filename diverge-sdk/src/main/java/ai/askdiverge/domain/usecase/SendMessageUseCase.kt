package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.event.StreamEvent
import kotlinx.coroutines.flow.Flow

internal class SendMessageUseCase internal constructor(private val repository: ChatbotRepository) {
    operator fun invoke(
        message: String,
        currentPage: String?
    ): Flow<StreamEvent> = repository.sendMessage(
        message = message,
        currentPage = currentPage
    )
}
