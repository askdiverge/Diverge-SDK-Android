package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.form.ChatFormDefinition

internal class FetchFormUseCase internal constructor(private val repository: ChatbotRepository) {
    suspend operator fun invoke(formId: String): Result<ChatFormDefinition> =
        repository.fetchForm(formId)
}
