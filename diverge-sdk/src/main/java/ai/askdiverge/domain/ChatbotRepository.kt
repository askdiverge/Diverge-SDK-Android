package ai.askdiverge.domain

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.ChatSessionState
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
import ai.askdiverge.domain.model.message.incoming.Message
import kotlinx.coroutines.flow.Flow

internal interface ChatbotRepository {

    suspend fun initialize(): Result<Config>

    suspend fun startSession(): Result<Unit>

    fun invalidateSession()

    fun getSessionInvalidations(): Flow<Unit>

    suspend fun renewToken(): Result<Unit>

    suspend fun deleteChatData(): Result<Unit>

    fun sendMessage(
        message: String,
        currentPage: String?
    ): Flow<StreamEvent>

    fun getMessageHistory(pagingConfig: PagingConfig): Flow<PagingData<Message>>

    suspend fun getLatestMessageId(): Result<String?>

    suspend fun submitAction(request: SubmitActionRequest): Result<SubmitActionResponse>

    suspend fun fetchForm(formId: String): Result<ChatFormDefinition>

    suspend fun getSession(): Result<ChatSessionState>

    suspend fun patchFormValues(
        formId: String,
        values: Map<String, String>
    ): Result<ChatSessionState>

    suspend fun syncLivechat(
        afterSequenceNumber: Long?,
        waitMs: Int?,
        syncCursor: String?
    ): Result<LivechatSync>

    suspend fun requestLivechatHandover(request: LivechatHandoverRequest): Result<LivechatState.Status>

    suspend fun sendLivechatMessage(request: LivechatVisitorMessageRequest): Result<LivechatMessage>

    suspend fun closeLivechat(reason: String?): Result<Unit>

    suspend fun sendLivechatTyping(isTyping: Boolean): Result<Unit>
}
