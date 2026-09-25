package ai.askdiverge.ui.model.mapper

import ai.askdiverge.domain.model.message.incoming.Message
import ai.askdiverge.domain.model.message.incoming.MessageRole
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextBlockUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanTypeUiModel
import ai.askdiverge.ui.model.message.part.text.RichTextSpanUiModel
import java.util.UUID

internal fun Message.mapToUi(): List<ChatBubbleUiModel> {
    val messageRoleUiModel = role.mapToUi() ?: return emptyList()
    val messageId = id
    return parts.reversed().mapIndexedNotNull { index, part ->
        val uiPart = part.mapToUi() ?: return@mapIndexedNotNull null
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

private fun MessageRole.mapToUi(): MessageRoleUiModel? = when (this) {
    MessageRole.USER -> MessageRoleUiModel.USER
    MessageRole.ASSISTANT, MessageRole.AGENT -> MessageRoleUiModel.BOT
    MessageRole.UNKNOWN -> null
}
