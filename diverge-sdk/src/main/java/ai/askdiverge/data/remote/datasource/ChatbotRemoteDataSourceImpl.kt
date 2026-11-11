package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.mapper.mapAuthError
import ai.askdiverge.data.mapper.mapFormError
import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.error.ApiErrorEnvelopeRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
import ai.askdiverge.data.model.livechat.LivechatSyncRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageRequestRemote
import ai.askdiverge.data.model.livechat.LivechatVisitorMessageResponseRemote
import ai.askdiverge.data.model.message.incoming.MessagePageRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import ai.askdiverge.data.remote.service.ChatbotService
import ai.askdiverge.data.remote.service.ChatbotStreamService
import ai.askdiverge.data.safeApiCall
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.flow.Flow

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

    override suspend fun sendLivechatMessage(
        body: LivechatVisitorMessageRequestRemote
    ): Result<LivechatVisitorMessageResponseRemote> = safeApiCall(
        errorMessage = "Could not send livechat message",
        mapError = { it.mapAuthError() }
    ) {
        service.sendLivechatMessage(body)
    }
}
