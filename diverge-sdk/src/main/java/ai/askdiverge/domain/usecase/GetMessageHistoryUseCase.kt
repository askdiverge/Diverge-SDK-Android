package ai.askdiverge.domain.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.message.incoming.Message
import kotlinx.coroutines.flow.Flow

/**
 * Returns the paged message history. The caller decides the paging configuration (page sizes can
 * differ per presentation context); the repository owns the Pager construction and the ViewModel
 * caches the stream in its scope.
 *
 * @param repository source of the paged message history.
 */
internal class GetMessageHistoryUseCase internal constructor(private val repository: ChatbotRepository) {
    operator fun invoke(pagingConfig: PagingConfig): Flow<PagingData<Message>> = repository.getMessageHistory(pagingConfig = pagingConfig)
}
