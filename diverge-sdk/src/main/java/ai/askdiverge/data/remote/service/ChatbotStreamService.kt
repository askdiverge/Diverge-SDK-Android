package ai.askdiverge.data.remote.service

import ai.askdiverge.data.model.event.StreamEventRemote
import ai.askdiverge.data.model.message.outgoing.SendMessageRemote
import kotlinx.coroutines.flow.Flow

internal interface ChatbotStreamService {
    fun sendMessage(message: SendMessageRemote): Flow<StreamEventRemote>
}
