package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput

internal class SendLivechatMessageUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        message: String,
        images: List<OutgoingImageInput>,
        currentPage: String?,
        /** Idempotency key; the same id on a retry lets the server drop the duplicate. */
        clientOperationId: String
    ): Result<LivechatMessage> = repository.sendLivechatMessage(
        LivechatVisitorMessageRequest(
            text = message,
            images = images,
            currentPage = currentPage,
            clientOperationId = clientOperationId
        )
    )
}
