package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatCloseRequestRemote
import ai.askdiverge.data.model.livechat.LivechatHandoverRequestRemote
import ai.askdiverge.data.model.livechat.LivechatSessionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatSyncRemote
import ai.askdiverge.data.model.livechat.LivechatTypingRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageResponseRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import kotlinx.coroutines.flow.Flow

internal interface ChatbotRemoteDataSource {
    suspend fun getConfig(): Result<ConfigRemote>

    suspend fun getMessageHistory(
        cursor: String?,
        pageSize: Int
    ): Result<MessagePageRemote>

    fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote>

    suspend fun submitAction(body: SubmitActionRequestRemote): Result<SubmitActionResponseRemote>

    suspend fun fetchForm(formId: String): Result<ChatFormDefinitionRemote>

    suspend fun syncLivechat(
        afterSequenceNumber: Long?,
        waitMs: Int?,
        syncCursor: String?
    ): Result<LivechatSyncRemote>

    suspend fun requestLivechatHandover(
        body: LivechatHandoverRequestRemote
    ): Result<LivechatSessionResponseRemote>

    suspend fun sendLivechatMessage(
        body: LivechatVisitorMessageRequestRemote
    ): Result<LivechatVisitorMessageResponseRemote>

    suspend fun closeLivechat(body: LivechatCloseRequestRemote): Result<Unit>

    suspend fun sendLivechatTyping(body: LivechatTypingRequestRemote): Result<Unit>
}
