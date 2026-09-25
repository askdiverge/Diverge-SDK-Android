package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

internal interface ChatbotService {

    @GET("v1/chat/config")
    suspend fun config(): Response<ConfigRemote>

    @GET("v1/chat/messages")
    suspend fun getMessageHistory(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int
    ): Response<MessagePageRemote>
}
