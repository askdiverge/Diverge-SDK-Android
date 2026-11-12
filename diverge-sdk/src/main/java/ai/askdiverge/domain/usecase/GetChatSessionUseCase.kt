package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.form.ChatSessionState

internal class GetChatSessionUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(): Result<ChatSessionState> = repository.getSession()
}
