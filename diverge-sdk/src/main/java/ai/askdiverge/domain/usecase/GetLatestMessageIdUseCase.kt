package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

/**
 * Returns the id of the newest message in the conversation, or null when the conversation has none.
 *
 * @param repository source of the message history.
 */
internal class GetLatestMessageIdUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(): Result<String?> = repository.getLatestMessageId()
}
