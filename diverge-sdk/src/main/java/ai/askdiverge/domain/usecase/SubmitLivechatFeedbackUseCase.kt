package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository

internal class SubmitLivechatFeedbackUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        rating: Int,
        feedback: String?
    ): Result<Unit> = repository.submitLivechatFeedback(rating = rating, feedback = feedback)
}
