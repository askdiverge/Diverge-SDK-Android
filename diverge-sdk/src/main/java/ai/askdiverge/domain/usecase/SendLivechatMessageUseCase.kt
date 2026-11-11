package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest

internal class SendLivechatMessageUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        message: String,
        currentPage: String?,
        /** Idempotency key; the same id on a retry lets the server drop the duplicate. */
        clientOperationId: String
    ): Result<LivechatMessage> = repository.sendLivechatMessage(
        LivechatVisitorMessageRequest(
            text = message,
            currentPage = currentPage,
            clientOperationId = clientOperationId
        )
    )
}
