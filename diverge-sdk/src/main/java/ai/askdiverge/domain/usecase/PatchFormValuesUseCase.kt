package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.form.ChatSessionState

internal class PatchFormValuesUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        formId: String,
        values: Map<String, String>
    ): Result<ChatSessionState> = repository.patchFormValues(formId = formId, values = values)
}
