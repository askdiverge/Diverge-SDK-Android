package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ChatbotService {

    @GET("v1/chat/config")
    suspend fun config(): Response<ConfigRemote>

    @GET("v1/chat/messages")
    suspend fun getMessageHistory(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int
    ): Response<MessagePageRemote>

    @POST("v1/chat/actions")
    suspend fun submitAction(@Body body: SubmitActionRequestRemote): Response<SubmitActionResponseRemote>

    @GET("v1/chat/forms/{form_id}")
    suspend fun fetchForm(@Path("form_id") formId: String): Response<ChatFormDefinitionRemote>
}
