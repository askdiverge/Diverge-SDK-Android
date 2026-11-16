package ai.askdiverge.ui.model

import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal data class ChatBubbleUiModel(
    val sender: ChatBubbleSender,
    val part: MessagePartUiModel,
    val id: String
) {
    /** The side of the conversation the bubble sits on, derived from [sender]. */
    val role: MessageRoleUiModel
        get() = when (sender) {
            ChatBubbleSender.User -> MessageRoleUiModel.USER
            is ChatBubbleSender.Bot -> MessageRoleUiModel.BOT
        }
}
