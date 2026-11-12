package ai.askdiverge.domain.model.livechat

import java.util.Locale

/**
 * The `client_context` on `POST /livechat/handover`, shown to agents while the visitor waits.
 * Blank values are dropped and long ones truncated so a context never fails the handover.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/livechathandoverrequest)
 */
internal data class LivechatClientContext(
    val browser: String? = null,
    val browserLanguage: String? = null,
    val browserVersion: String? = null,
    val currentPageUrl: String? = null,
    val language: String? = null,
    val os: String? = null
) {
    companion object {
        /** `os` for this SDK; agents see it beside the visitor's language and page. */
        const val NATIVE_OS = "Android"

        private const val FIELD_MAX = 255
        private const val PAGE_MAX = 2048

        /**
         * Native Android context: device UI language as `browser_language`, host page as
         * `current_page_url`, and `os` = `Android`.
         */
        fun native(
            page: String?,
            locale: Locale = Locale.getDefault()
        ): LivechatClientContext = LivechatClientContext(
            browserLanguage = clamp(locale.toLanguageTag(), FIELD_MAX),
            currentPageUrl = clamp(page, PAGE_MAX),
            os = NATIVE_OS
        )

        private fun clamp(
            value: String?,
            max: Int
        ): String? {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            if (trimmed.length <= max) return trimmed
            return trimmed.take(max)
        }
    }
}
