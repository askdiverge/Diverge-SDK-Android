package ai.askdiverge.data.remote.datasource

import ai.askdiverge.data.model.ConfigRemote
import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.form.ChatFormDefinitionRemote
import ai.askdiverge.data.model.form.SubmitActionRequestRemote
import ai.askdiverge.data.model.form.SubmitActionResponseRemote
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
}
