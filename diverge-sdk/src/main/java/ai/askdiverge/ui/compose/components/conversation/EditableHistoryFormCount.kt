package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel

/**
 * How many of the newest [history] bubbles belong to the newest bot turn, whose forms stay
 * editable as the live reply's do: the bubbles before history's latest user message, or all of
 * them while no user message has loaded. None once this session has [liveMessages], which are
 * newer than the whole history.
 *
 * @param history loaded history bubbles, newest first; `null` entries are not loaded yet.
 */
internal fun editableHistoryFormCount(
    liveMessages: List<ChatBubbleUiModel>,
    history: List<ChatBubbleUiModel?>
): Int {
    if (liveMessages.isNotEmpty()) return 0
    val latestUserMessage = history.indexOfFirst { bubble -> bubble?.role == MessageRoleUiModel.USER }
    return if (latestUserMessage == -1) history.size else latestUserMessage
}
