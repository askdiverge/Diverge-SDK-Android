package ai.askdiverge.ui.compose.components.conversation.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import ai.askdiverge.ui.compose.components.conversation.collapsingspace.CollapsingSpaceState
import ai.askdiverge.ui.compose.components.conversation.collapsingspace.collapsingSpace
import ai.askdiverge.ui.compose.components.message.BotPendingMessageBubble
import ai.askdiverge.ui.compose.components.message.ChatBubble
import ai.askdiverge.ui.compose.previewprovider.MessageAndReplyPreviewProvider
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.state.PendingBotMessageState

private const val ITEM_KEY = "latestUserMessage"

/**
 * Adds [messages] as a single lazy item, over a space that fills whatever of the viewport they leave.
 * Adds nothing when [messages] is empty.
 *
 * @param messages a [ChatBubble] each, rendered in reverse as they arrive newest first.
 * @param pendingBotMessageState one more bubble on the end, unless it is [PendingBotMessageState.None].
 * @param botAvatarUrl shown beside every bot bubble.
 * @param spaceState the space under the bubbles, shared with the list so a scroll can collapse it.
 * @param inputBarHeightPx the input bar's height, forwarded to the space so it re-measures when the
 * bar resizes.
 * @param onUrlClick called with the url of a link tapped inside a bubble.
 */
internal fun LazyListScope.latestUserMessageAndItsReply(
    messages: List<ChatBubbleUiModel>,
    pendingBotMessageState: PendingBotMessageState,
    botAvatarUrl: String,
    spaceState: CollapsingSpaceState,
    inputBarHeightPx: IntState,
    onUrlClick: ((String) -> Unit)? = null
) {
    if (messages.isEmpty()) {
        return
    }

    // A constant key keeps this item in its slot as its messages change, so the list stays anchored
    // here rather than following the old messages up and scrolling back down.
    item(key = ITEM_KEY) {
        // The Box takes the taller of the space and the bubbles, and measures both in one pass.
        Box {
            Spacer(
                modifier = Modifier
                    .collapsingSpace(
                        spaceState = spaceState,
                        keyboardInsets = WindowInsets.ime,
                        inputBarHeightPx = inputBarHeightPx
                    )
                    .fillParentMaxHeight()
            )

            Column(
                modifier = Modifier.onSizeChanged { newSize ->
                    spaceState.updateContentHeight(newSize.height.toFloat())
                },
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Keyed per bubble, since the item's own key is constant.
                messages.asReversed().forEach { message ->
                    key(message.id) {
                        ChatBubble(
                            uiModel = message,
                            botAvatarUrl = botAvatarUrl,
                            onUrlClick = onUrlClick
                        )
                    }
                }

                if (pendingBotMessageState != PendingBotMessageState.None) {
                    BotPendingMessageBubble(
                        state = pendingBotMessageState,
                        avatarUrl = botAvatarUrl,
                        onUrlClick = onUrlClick
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LatestUserMessageAndItsReplyPreview(
    @PreviewParameter(MessageAndReplyPreviewProvider::class) messages: List<ChatBubbleUiModel>
) {
    val listState = rememberLazyListState()
    val spaceState = remember(listState) { CollapsingSpaceState(listState) }
    val inputBarHeightPx = remember { mutableIntStateOf(0) }

    LazyColumn(state = listState) {
        latestUserMessageAndItsReply(
            messages = messages,
            pendingBotMessageState = PendingBotMessageState.AwaitingFirstPart,
            botAvatarUrl = "",
            spaceState = spaceState,
            inputBarHeightPx = inputBarHeightPx
        )
    }
}
