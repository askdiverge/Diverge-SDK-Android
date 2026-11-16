package ai.askdiverge.ui.model

import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal data class ChatBubbleUiModel(
    val role: MessageRoleUiModel,
    val part: MessagePartUiModel,
    val id: String,
    /** Only applied for bot rich-text bubbles; defaults to the session avatar. */
    val senderAvatar: SenderAvatar = SenderAvatar.Session
)
