package ai.askdiverge.ui.compose.components.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.state.PendingBotMessageState
import kotlin.collections.forEachIndexed
import kotlin.collections.lastIndex

/**
 * The chatbot's live reply, shown while it is still being generated.
 *
 * As the bot streams an answer, its delta events are accumulated into [PendingBotMessageState.Streaming]
 * and rendered here in real time under a single avatar, so the user watches the response build up as
 * it arrives. This is the in-progress message, not a finished one. Once streaming completes, the
 * reply is committed as a normal bot bubble in the conversation and this section renders nothing
 * again, until the next reply starts.
 */
@Composable
internal fun BotPendingMessageBubble(
    state: PendingBotMessageState,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 24.dp)
    ) {
        when (state) {
            is PendingBotMessageState.None -> Unit

            is PendingBotMessageState.AwaitingFirstPart -> BotLoadingBubbles(
                avatarUrl = avatarUrl
            )

            is PendingBotMessageState.Streaming -> state.parts.forEachIndexed { index, part ->
                BotMessage(
                    uiModel = part,
                    avatarUrl = avatarUrl,
                    onUrlClick = onUrlClick,
                    // This bubble only renders while generating, so its last part is the one still streaming in.
                    isStreaming = index == state.parts.lastIndex
                )
            }
        }
    }
}
