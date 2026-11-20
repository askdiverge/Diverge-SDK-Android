package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.trimToNull
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.ui.model.ChatBubbleSender
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SenderAvatar
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import java.util.UUID

internal fun Message.mapToUi(
    offersAttachments: Boolean
): List<ChatBubbleUiModel> {
    val sender = sender() ?: return emptyList()
    val messageId = id
    return parts.reversed().mapIndexedNotNull { index, part ->
        val uiPart = part.mapToUi(offersAttachments = offersAttachments) ?: return@mapIndexedNotNull null
        ChatBubbleUiModel(
            sender = sender,
            part = uiPart,
            // Every part but ImagePart has an id of its own, so the message id and the part's
            // position identify the bubble instead.
            id = "$messageId-$index"
        )
    }
}

/** Who sent this message; null for a role the SDK does not show. */
private fun Message.sender(): ChatBubbleSender? = when (role) {
    MessageRole.USER -> ChatBubbleSender.User
    MessageRole.ASSISTANT -> ChatBubbleSender.Bot(SenderAvatar.Session)
    MessageRole.AGENT -> ChatBubbleSender.Bot(
        SenderAvatar.Agent(
            // `data` has made `avatar_url` absolute and loadable, or null.
            imageUrl = agent?.avatarUrl,
            displayName = agent?.displayName.trimToNull()
        )
    )
    MessageRole.UNKNOWN -> null
}

internal fun String.toUserMessageItem(): ChatBubbleUiModel = ChatBubbleUiModel(
    sender = ChatBubbleSender.User,
    part = MessagePartUiModel.RichText(
        blocks = listOf(
            RichTextBlockUiModel.Paragraph(
                spans = listOf(RichTextSpanUiModel(text = this, type = RichTextSpanTypeUiModel.TEXT))
            )
        )
    ),
    id = UUID.randomUUID().toString()
)

/**
 * Optimistic bubbles for a visitor send: image parts newest-first, then text, matching
 * [Message.mapToUi]'s reverse part order. Each image shows its own payload as a `data:` URL.
 */
internal fun outgoingUserBubbles(
    text: String,
    images: List<OutgoingImageInput>
): List<ChatBubbleUiModel> {
    val bubbles = mutableListOf<ChatBubbleUiModel>()
    for (image in images.asReversed()) {
        bubbles += ChatBubbleUiModel(
            sender = ChatBubbleSender.User,
            part = MessagePartUiModel.Image(
                displayUrl = "data:${image.mimeType};base64,${image.dataBase64}",
                thumbnailUrl = null,
                attachmentId = null,
                mimeType = image.mimeType,
                sizeBytes = null,
                urlExpiresAt = null,
                caption = null
            ),
            id = UUID.randomUUID().toString()
        )
    }
    if (text.isNotBlank()) {
        bubbles += text.toUserMessageItem()
    }
    return bubbles
}
