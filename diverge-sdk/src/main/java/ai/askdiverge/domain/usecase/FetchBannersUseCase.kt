package ai.askdiverge.domain.usecase

import ai.askdiverge.domain.ChatbotRepository
import ai.askdiverge.domain.model.banner.ChatBanner

internal class FetchBannersUseCase internal constructor(
    private val repository: ChatbotRepository
) {
    /**
     * Lists active in-chat banners for [pageUrl] (the host's current-page string). Does not clear
     * the visitor token.
     */
    suspend operator fun invoke(pageUrl: String?): Result<List<ChatBanner>> =
        repository.fetchBanners(pageUrl = pageUrl)
}
