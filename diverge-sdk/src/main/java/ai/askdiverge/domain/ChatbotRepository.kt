package ai.askdiverge.domain

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ai.askdiverge.domain.model.Config
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.message.incoming.Message
import kotlinx.coroutines.flow.Flow

internal interface ChatbotRepository {

    suspend fun initialize(): Result<Config>

    suspend fun startSession(): Result<Unit>

    fun invalidateSession()

    suspend fun renewToken(): Result<Unit>

    suspend fun deleteChatData(): Result<Unit>

    fun sendMessage(
        message: String,
        currentPage: String?
    ): Flow<StreamEvent>

    fun getMessageHistory(pagingConfig: PagingConfig): Flow<PagingData<Message>>
}
