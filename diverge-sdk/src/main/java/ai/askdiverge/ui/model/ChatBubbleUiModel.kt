package ai.askdiverge.ui.model

import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal data class ChatBubbleUiModel(
    val role: MessageRoleUiModel,
    val part: MessagePartUiModel,
    val id: String,
    /** The face beside a bot rich-text bubble. User bubbles and other parts draw no avatar. */
    val senderAvatar: SenderAvatar
)
