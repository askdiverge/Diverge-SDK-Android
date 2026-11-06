package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.exception.FormRequestException
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse

/**
 * Submits a filled form. An action the contract rejects fails with
 * [FormRequestException.Validation] without reaching the network.
 */
internal class SubmitActionUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(request: SubmitActionRequest): Result<SubmitActionResponse> =
        if (request.action.isSubmittable) {
            repository.submitAction(request)
        } else {
            Result.failure(FormRequestException.Validation(params = emptyList()))
        }
}
