package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.mapper.mapAuthError
import ai.askdiverge.data.mapper.mapFormError
import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.error.ApiErrorEnvelopeRemote
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
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamService
import ai.askdiverge.data.runCatchingCancellable
import ai.askdiverge.data.safeApiCall
import ai.askdiverge.domain.logging.ChatbotLogger
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

internal class ChatbotRemoteDataSourceImpl(
    private val service: ChatbotService,
    private val streamService: ChatbotStreamService,
    private val errorEnvelopeAdapter: JsonAdapter<ApiErrorEnvelopeRemote>
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

    override fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote> =
        streamService.sendMessage(message)

    override suspend fun submitAction(body: SubmitActionRequestRemote): Result<SubmitActionResponseRemote> =
        safeApiCall(
            errorMessage = "Could not submit action",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.submitAction(body)
        }

    override suspend fun fetchForm(formId: String): Result<ChatFormDefinitionRemote> =
        safeApiCall(
            errorMessage = "Could not fetch form",
            mapError = { it.mapFormError(errorEnvelopeAdapter) }
        ) {
            service.fetchForm(formId)
        }

    override suspend fun syncLivechat(
        afterSequenceNumber: Long?,
        waitMs: Int?,
        syncCursor: String?
    ): Result<LivechatSyncRemote> = safeApiCall(
        errorMessage = "Could not sync livechat",
        mapError = { it.mapAuthError() }
    ) {
        service.syncLivechat(
            afterSequenceNumber = afterSequenceNumber,
            waitMs = waitMs,
            syncCursor = syncCursor
        )
    }

    override suspend fun requestLivechatHandover(
        body: LivechatHandoverRequestRemote
    ): Result<LivechatSessionResponseRemote> = safeApiCall(
        errorMessage = "Could not request livechat handover",
        mapError = { it.mapAuthError() }
    ) {
        service.requestLivechatHandover(body)
    }

    override suspend fun sendLivechatMessage(
        body: LivechatVisitorMessageRequestRemote
    ): Result<LivechatVisitorMessageResponseRemote> = safeApiCall(
        errorMessage = "Could not send livechat message",
        mapError = { it.mapAuthError() }
    ) {
        service.sendLivechatMessage(body)
    }

    override suspend fun sendLivechatTyping(body: LivechatTypingRequestRemote): Result<Unit> =
        emptyBodyCall(errorMessage = "Could not send livechat typing") {
            service.sendLivechatTyping(body)
        }

    override suspend fun closeLivechat(body: LivechatCloseRequestRemote): Result<Unit> =
        emptyBodyCall(errorMessage = "Could not close livechat") {
            service.closeLivechat(body)
        }

    /**
     * Endpoints that answer with an empty body: Moshi cannot decode `Unit`, so success is the
     * status alone.
     */
    private suspend fun emptyBodyCall(
        errorMessage: String,
        call: suspend () -> Response<okhttp3.ResponseBody>
    ): Result<Unit> = runCatchingCancellable {
        val response = call()
        if (response.isSuccessful) {
            Unit
        } else {
            throw response.mapAuthError() ?: Exception(errorMessage)
        }
    }.onFailure { error -> ChatbotLogger.logError(error) }
}
