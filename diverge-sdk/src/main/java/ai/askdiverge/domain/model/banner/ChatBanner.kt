package ai.askdiverge.domain.model.banner

import ai.askdiverge.domain.model.AttachmentUrl

/**
 * An in-chat promo strip from `GET /api/v1/chat/banners`. Schedule and URL matching are applied on
 * the server; the SDK paints the list that comes back.
 *
 * [API ref](https://docs.askdiverge.ai/api#model/chatbanner)
 */
internal data class ChatBanner(
    val id: String,
    val message: String,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val ctaUrl: String? = null,
    val ctaLabel: String? = null,
    val ctaStyle: CtaStyle? = null,
    val dismissible: Boolean = false
) {
    enum class CtaStyle {
        LINK,
        BUTTON
    }

    /**
     * Content fingerprint matching the web widget — dismiss hides until content changes or the
     * ViewModel is recreated.
     */
    val contentFingerprint: String
        get() = listOf(
            message,
            backgroundColor.orEmpty(),
            textColor.orEmpty(),
            ctaUrl.orEmpty(),
            ctaLabel.orEmpty(),
            ctaStyle?.wireValue.orEmpty(),
            if (dismissible) "1" else "0"
        ).joinToString("|")

    /** Absolute http(s) CTA only — relative paths and `javascript:` are dropped. */
    val sanitizedCtaUrl: String?
        get() {
            val raw = ctaUrl?.trim().orEmpty()
            if (raw.isEmpty()) return null
            return AttachmentUrl.loadableOrNull(raw)?.takeIf(AttachmentUrl::isWebUrl)
        }

    val trimmedCtaLabel: String?
        get() = ctaLabel?.trim()?.takeIf { it.isNotEmpty() }

    /** CTA renders only when both a sanitized http(s) URL and a non-empty label exist. */
    val showsCta: Boolean
        get() = sanitizedCtaUrl != null && trimmedCtaLabel != null

    /** Blank body text is dropped — an empty coloured bar is not a banner. */
    val hasVisibleMessage: Boolean
        get() = message.trim().isNotEmpty()

    private val CtaStyle.wireValue: String
        get() = when (this) {
            CtaStyle.LINK -> "link"
            CtaStyle.BUTTON -> "button"
        }
}
