package ai.askdiverge.ui.model

import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel

internal data class ChatBubbleUiModel(
    val role: MessageRoleUiModel,
    val part: MessagePartUiModel,
    val id: String
)
