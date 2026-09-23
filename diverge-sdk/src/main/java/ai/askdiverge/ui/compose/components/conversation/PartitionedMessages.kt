package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel

/** Messages split in two: the latest user message with its reply, and everything older. */
internal data class PartitionedMessages(
    val latestUserMessageAndItsReply: List<ChatBubbleUiModel>,
    val earlierMessages: List<ChatBubbleUiModel>
)

/**
 * Splits at the first [MessageRoleUiModel.USER] message, which is the latest one because messages
 * arrive newest first, and keeps it with everything before it in the list.
 */
internal fun List<ChatBubbleUiModel>.partitioned(): PartitionedMessages {
    // -1 before the user has posted anything, which leaves every message in earlierMessages.
    val latestSize = indexOfFirst { message -> message.role == MessageRoleUiModel.USER } + 1
    return PartitionedMessages(
        latestUserMessageAndItsReply = take(latestSize),
        earlierMessages = drop(latestSize)
    )
}
