package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class DeleteChatDataUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteChatData()
}
