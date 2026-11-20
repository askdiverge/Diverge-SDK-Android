package ai.askdiverge.data.mapper

import ai.askdiverge.domain.model.AttachmentUrl
import ai.askdiverge.domain.model.event.StreamDoneEvent
import ai.askdiverge.domain.model.event.StreamEvent
import ai.askdiverge.domain.model.event.StreamPartEvent
import ai.askdiverge.domain.model.livechat.LivechatAgent
import ai.askdiverge.domain.model.livechat.LivechatMessage
import ai.askdiverge.domain.model.livechat.LivechatSync
import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.part.ImagePart
import ai.askdiverge.domain.model.message.incoming.part.MessageFile
import ai.askdiverge.domain.model.message.incoming.part.MessagePart

// Attachment links and agent photos may arrive as bare paths; these make them absolute against the
// API root, so nothing above `data` needs the base URL. A link that cannot be resolved is kept as
// sent; such an agent photo is dropped, so the person glyph shows instead.

internal fun MessagePart.withAbsoluteAttachmentUrls(baseUrl: String): MessagePart = when (this) {
    is MessageFile -> copy(url = AttachmentUrl.resolve(url, baseUrl) ?: url)
    is ImagePart -> copy(
        url = AttachmentUrl.resolve(url, baseUrl) ?: url,
        thumbnailUrl = thumbnailUrl?.let { AttachmentUrl.resolve(it, baseUrl) }
    )
    else -> this
}

internal fun Message.withAbsoluteAttachmentUrls(baseUrl: String): Message = copy(
    agent = agent?.withAbsoluteAvatarUrl(baseUrl),
    parts = parts.map { it.withAbsoluteAttachmentUrls(baseUrl) }
)

internal fun LivechatMessage.withAbsoluteAttachmentUrls(baseUrl: String): LivechatMessage = copy(
    agent = agent?.withAbsoluteAvatarUrl(baseUrl),
    parts = parts.map { it.withAbsoluteAttachmentUrls(baseUrl) }
)

private fun LivechatAgent.withAbsoluteAvatarUrl(baseUrl: String): LivechatAgent =
    copy(avatarUrl = avatarUrl?.let { AttachmentUrl.resolve(it, baseUrl) })

internal fun LivechatSync.withAbsoluteAttachmentUrls(baseUrl: String): LivechatSync =
    copy(messages = messages.map { it.withAbsoluteAttachmentUrls(baseUrl) })

internal fun StreamEvent.withAbsoluteAttachmentUrls(baseUrl: String): StreamEvent = when (this) {
    is StreamPartEvent -> copy(part = part.withAbsoluteAttachmentUrls(baseUrl))
    is StreamDoneEvent -> copy(message = message.withAbsoluteAttachmentUrls(baseUrl))
    else -> this
}
