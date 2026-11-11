package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
import java.util.UUID

internal class SendLivechatMessageUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(
        message: String,
        currentPage: String?,
        clientOperationId: String = UUID.randomUUID().toString()
    ): Result<LivechatMessage> = repository.sendLivechatMessage(
        LivechatVisitorMessageRequest(
            text = message,
            currentPage = currentPage,
            clientOperationId = clientOperationId
        )
    )
}
