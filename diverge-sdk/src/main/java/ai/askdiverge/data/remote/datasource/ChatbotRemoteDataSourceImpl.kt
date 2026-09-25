package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamService
import ai.askdiverge.data.safeApiCall
import kotlinx.coroutines.flow.Flow

internal class ChatbotRemoteDataSourceImpl(
    private val service: ChatbotService,
    private val streamService: ChatbotStreamService
) : ChatbotRemoteDataSource {

    override suspend fun getConfig(): Result<ConfigRemote> = safeApiCall(errorMessage = "Config error") {
        service.config()
    }

    override suspend fun getMessageHistory(
        cursor: String?,
        pageSize: Int
    ): Result<MessagePageRemote> = safeApiCall(errorMessage = "Could not get messages") {
        service.getMessageHistory(
            cursor = cursor,
            limit = pageSize
        )
    }

    override fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote> = streamService.sendMessage(message)
}
