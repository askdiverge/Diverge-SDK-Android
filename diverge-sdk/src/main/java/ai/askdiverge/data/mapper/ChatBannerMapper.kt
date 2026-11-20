package ai.askdiverge.data.mapper

import ai.askdiverge.data.model.banner.ChatBannerListRemote
import ai.askdiverge.data.model.banner.ChatBannerRemote
import ai.askdiverge.domain.model.banner.ChatBanner

internal fun ChatBannerListRemote.mapToDomain(): List<ChatBanner> =
    banners.mapNotNull { it.mapToDomain() }

internal fun ChatBannerRemote.mapToDomain(): ChatBanner? {
    val mapped = ChatBanner(
        id = id,
        message = message,
        backgroundColor = background_color,
        textColor = text_color,
        ctaUrl = cta_url,
        ctaLabel = cta_label,
        ctaStyle = cta_style.toCtaStyle(),
        dismissible = dismissible
    )
    return mapped.takeIf { it.hasVisibleMessage }
}

private fun String?.toCtaStyle(): ChatBanner.CtaStyle? = when (this?.trim()?.lowercase()) {
    "link" -> ChatBanner.CtaStyle.LINK
    "button" -> ChatBanner.CtaStyle.BUTTON
    else -> null
}
