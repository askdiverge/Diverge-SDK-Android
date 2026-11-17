package ai.askdiverge.data.model.banner

/**
 * Path and query literals for `GET /api/v1/chat/banners` (`Banners.listBanners` in the Chatbot API
 * TypeSpec). Tests pin these strings.
 */
internal object VisitorBannersWire {

    /**
     * Retrofit path relative to the API base (`…/api/`). Authenticated visitor banners; chatbot id
     * comes from the JWT. A new path needs a backend change first.
     */
    const val PATH = "v1/chat/banners"

    /**
     * Query item for the visitor's current page path or URL. The backend matches `url_pattern` and
     * schedule, then may fall back to default banners. Omit when the host passed no page string.
     */
    const val URL = "url"
}
