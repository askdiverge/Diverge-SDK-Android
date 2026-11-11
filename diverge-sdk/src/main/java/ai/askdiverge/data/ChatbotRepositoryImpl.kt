package ai.askdiverge.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ai.askdiverge.ChatbotCallbacks
import ai.askdiverge.data.local.datasource.TokenLocalDataSource
import ai.askdiverge.data.mapper.mapToDomain
import ai.askdiverge.data.mapper.mapToRemote
import ai.askdiverge.data.model.livechat.LivechatCloseRequestRemote
import ai.askdiverge.data.model.livechat.LivechatTypingRequestRemote
import ai.askdiverge.data.model.message.OutgoingMessageContextRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingMessageRemote
import ai.askdiverge.data.model.message.outgoing.OutgoingTextPartRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.MessageHistoryPagingSource
import ai.askdiverge.data.remote.datasource.ChatbotRemoteDataSource
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.form.ChatFormDefinition
import ai.askdiverge.domain.model.form.SubmitActionRequest
import ai.askdiverge.domain.model.form.SubmitActionResponse
import ai.askdiverge.domain.model.livechat.LivechatHandoverRequest
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatState
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.livechat.LivechatVisitorMessageRequest
import ai.askdiverge.domain.model.message.incoming.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

internal class ChatbotRepositoryImpl(
    private val chatbotCallbacks: ChatbotCallbacks,
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val remoteDataSource: ChatbotRemoteDataSource
) : ChatbotRepository {

    override suspend fun initialize(): Result<Config> = runCatchingCancellable {
        resolveToken().getOrThrow()
        remoteDataSource.getConfig().map { it.mapToDomain() }.getOrThrow()
    }

    // A token is what a session is, so putting one in place starts one. The caller only needs to
    // know whether it succeeded, and the config is left alone.
    override suspend fun startSession(): Result<Unit> = runCatchingCancellable { resolveToken().getOrThrow() }.map { }

    override fun invalidateSession() = tokenLocalDataSource.clear()

    override fun getSessionInvalidations(): Flow<Unit> = chatbotCallbacks.sessionInvalidations()

    override suspend fun renewToken(): Result<Unit> = runCatchingCancellable {
        chatbotCallbacks.createNewToken().map(tokenLocalDataSource::setToken).getOrThrow()
    }

    override suspend fun deleteChatData(): Result<Unit> = runCatchingCancellable {
        chatbotCallbacks.deleteChatData().getOrThrow()
    }

    override fun sendMessage(
        message: String,
        currentPage: String?
    ): Flow<StreamEvent> {
        val sendMessageRemote = SendMessageRemote(
            message = OutgoingMessageRemote(
                parts = listOf(OutgoingTextPartRemote(text = message)),
                context = currentPage?.let { OutgoingMessageContextRemote(page = it) }
            )
        )
        return remoteDataSource.sendMessage(sendMessageRemote).mapNotNull { it.mapToDomain() }
    }

    override fun getMessageHistory(pagingConfig: PagingConfig): Flow<PagingData<Message>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { MessageHistoryPagingSource(remoteDataSource = remoteDataSource) }
    ).flow

    override suspend fun getLatestMessageId(): Result<String?> = remoteDataSource.getMessageHistory(
        cursor = null,
        pageSize = 1
    ).map { page -> page.messages.firstOrNull()?.message_id }

    override suspend fun submitAction(request: SubmitActionRequest): Result<SubmitActionResponse> =
        remoteDataSource.submitAction(request.mapToRemote()).map { it.mapToDomain() }

    override suspend fun fetchForm(formId: String): Result<ChatFormDefinition> =
        remoteDataSource.fetchForm(formId).map { it.mapToDomain() }

    override suspend fun syncLivechat(
        afterSequenceNumber: Long?,
        waitMs: Int?,
        syncCursor: String?
    ): Result<LivechatSync> = remoteDataSource.syncLivechat(
        afterSequenceNumber = afterSequenceNumber,
        waitMs = waitMs,
        syncCursor = syncCursor
    ).map { it.mapToDomain() }

    override suspend fun requestLivechatHandover(
        request: LivechatHandoverRequest
    ): Result<LivechatState.Status> =
        remoteDataSource.requestLivechatHandover(request.mapToRemote()).map { it.mapToDomain() }

    override suspend fun sendLivechatMessage(
        request: LivechatVisitorMessageRequest
    ): Result<LivechatMessage> =
        remoteDataSource.sendLivechatMessage(request.mapToRemote()).map { it.message.mapToDomain() }

    override suspend fun sendLivechatTyping(isTyping: Boolean): Result<Unit> =
        remoteDataSource.sendLivechatTyping(LivechatTypingRequestRemote(is_typing = isTyping))

    override suspend fun closeLivechat(reason: String?): Result<Unit> =
        remoteDataSource.closeLivechat(LivechatCloseRequestRemote(reason = reason))

    /**
     * Returns the cached token, or asks the host for one and caches it. Nothing is cached on the
     * first connect or after a data deletion; a conversation reset supplies its own token instead.
     *
     * @return the token, or the host's own failure so callers surface its message.
     */
    private suspend fun resolveToken(): Result<String> {
        tokenLocalDataSource.getToken()?.let { cachedToken -> return Result.success(cachedToken) }

        return chatbotCallbacks.fetchToken().onSuccess(tokenLocalDataSource::setToken)
    }
}
