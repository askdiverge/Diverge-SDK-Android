package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel

/** Messages split in two: the latest user message with its reply, and everything older. */
internal data class PartitionedMessages(
    val latestUserMessageAndItsReply: List<ChatBubbleUiModel>,
    val earlierMessages: List<ChatBubbleUiModel>
)

/**
 * Splits at the latest user turn: everything before the first [MessageRoleUiModel.USER] (its
 * reply, newest first) plus the run of user bubbles from there: one send's text and photos, or
 * several livechat sends the agent has not answered yet, which stay one turn as on iOS.
 */
internal fun List<ChatBubbleUiModel>.partitioned(): PartitionedMessages {
    val firstUser = indexOfFirst { message -> message.role == MessageRoleUiModel.USER }
    // -1 before the user has posted anything, which leaves every message in earlierMessages.
    if (firstUser < 0) {
        return PartitionedMessages(
            latestUserMessageAndItsReply = emptyList(),
            earlierMessages = this
        )
    }
    var end = firstUser + 1
    while (end < size && this[end].role == MessageRoleUiModel.USER) {
        end += 1
    }
    return PartitionedMessages(
        latestUserMessageAndItsReply = take(end),
        earlierMessages = drop(end)
    )
}
