package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.domain.model.message.outgoing.OutgoingImageInput
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import java.util.UUID

internal fun Message.mapToUi(
    apiBaseUrl: String? = null,
    offersAttachments: Boolean = true
): List<ChatBubbleUiModel> {
    val messageRoleUiModel = role.mapToUi() ?: return emptyList()
    val messageId = id
    return parts.reversed().mapIndexedNotNull { index, part ->
        val uiPart = part.mapToUi(
            apiBaseUrl = apiBaseUrl,
            offersAttachments = offersAttachments
        ) ?: return@mapIndexedNotNull null
        ChatBubbleUiModel(
            role = messageRoleUiModel,
            part = uiPart,
            // Every part but ImagePart has an id of its own, so the message id and the part's
            // position identify the bubble instead.
            id = "$messageId-$index"
        )
    }
}

internal fun String.toUserMessageItem(): ChatBubbleUiModel = ChatBubbleUiModel(
    role = MessageRoleUiModel.USER,
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
            role = MessageRoleUiModel.USER,
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

private fun MessageRole.mapToUi(): MessageRoleUiModel? = when (this) {
    MessageRole.USER -> MessageRoleUiModel.USER
    MessageRole.ASSISTANT, MessageRole.AGENT -> MessageRoleUiModel.BOT
    MessageRole.UNKNOWN -> null
}
