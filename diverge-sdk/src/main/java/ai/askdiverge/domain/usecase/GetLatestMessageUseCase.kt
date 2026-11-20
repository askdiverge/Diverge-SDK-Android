package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.message.incoming.Message

/**
 * Returns the newest message in the conversation, or null when the conversation has none.
 *
 * @param repository source of the message history.
 */
internal class GetLatestMessageUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(): Result<Message?> = repository.getLatestMessage()
}
