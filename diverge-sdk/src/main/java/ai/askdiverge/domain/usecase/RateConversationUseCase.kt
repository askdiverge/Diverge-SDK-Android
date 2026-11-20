package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class RateConversationUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        rating: Int,
        feedback: String?
    ): Result<Unit> = repository.rateConversation(rating = rating, feedback = feedback)
}
