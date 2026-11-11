package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatSyncRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageResponseRemote
import ai.askdiverge.data.model.livechat.LivechatWire
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

    @GET("v1/chat/livechat/sync")
    suspend fun syncLivechat(
        @Query(LivechatWire.AFTER_SEQUENCE_NUMBER) afterSequenceNumber: Long?,
        @Query(LivechatWire.WAIT_MS) waitMs: Int?,
        @Query(LivechatWire.SYNC_CURSOR) syncCursor: String?
    ): Response<LivechatSyncRemote>

    @POST("v1/chat/livechat/messages")
    suspend fun sendLivechatMessage(
        @Body body: LivechatVisitorMessageRequestRemote
    ): Response<LivechatVisitorMessageResponseRemote>
}
