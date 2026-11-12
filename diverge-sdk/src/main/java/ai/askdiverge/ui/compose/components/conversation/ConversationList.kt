package ai.askdiverge.ui.compose.components.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import ai.askdiverge.ui.compose.components.conversation.collapsingspace.CollapsingSpaceState
import ai.askdiverge.ui.compose.components.conversation.section.conversationHeader
import ai.askdiverge.ui.compose.components.conversation.section.earlierMessages
import ai.askdiverge.ui.compose.components.conversation.section.latestUserMessageAndItsReply
import ai.askdiverge.ui.compose.components.conversation.section.messageHistory
import ai.askdiverge.ui.compose.previewprovider.ConversationPreviewProvider
import ai.askdiverge.ui.compose.previewprovider.emptyEndOfPaginationHistory
import ai.askdiverge.ui.attachment.AttachmentUiCallbacks
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.livechat.LivechatUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SubtitleLinkUiModel
import ai.askdiverge.ui.model.SubtitleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.state.PendingBotMessageState

@Composable
internal fun ConversationList(
    welcomeMessage: String,
    subtitle: SubtitleUiModel?,
    historyPagingItems: LazyPagingItems<ChatBubbleUiModel>,
    liveMessages: List<ChatBubbleUiModel>,
    pendingBotMessageState: PendingBotMessageState,
    botAvatarUrl: String,
    inputBarHeightPx: IntState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    livechatCallbacks: LivechatUiCallbacks = LivechatUiCallbacks.None,
    attachmentCallbacks: AttachmentUiCallbacks = AttachmentUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    val partitioned = remember(liveMessages) { liveMessages.partitioned() }

    // Keyed on the newest user bubble, so each send starts with a space of its own rather than
    // inheriting how far the previous one was collapsed, also when it joins an unanswered turn.
    val newestUserBubbleId = partitioned.latestUserMessageAndItsReply
        .firstOrNull { it.role == MessageRoleUiModel.USER }
        ?.id
    val replySpaceState = remember(listState, newestUserBubbleId) {
        CollapsingSpaceState(listState)
    }

    // reverseLayout puts index 0 at the bottom, so the sections are declared newest -> oldest.
    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = modifier
            .padding(16.dp)
            .nestedScroll(replySpaceState.nestedScrollConnection),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        latestUserMessageAndItsReply(
            messages = partitioned.latestUserMessageAndItsReply,
            pendingBotMessageState = pendingBotMessageState,
            botAvatarUrl = botAvatarUrl,
            spaceState = replySpaceState,
            inputBarHeightPx = inputBarHeightPx,
            formCallbacks = formCallbacks,
            livechatCallbacks = livechatCallbacks,
            attachmentCallbacks = attachmentCallbacks,
            onUrlClick = onUrlClick
        )

        earlierMessages(
            messages = partitioned.earlierMessages,
            botAvatarUrl = botAvatarUrl,
            formCallbacks = formCallbacks,
            livechatCallbacks = livechatCallbacks,
            attachmentCallbacks = attachmentCallbacks,
            onUrlClick = onUrlClick
        )

        messageHistory(
            historyPagingItems = historyPagingItems,
            botAvatarUrl = botAvatarUrl,
            formCallbacks = formCallbacks,
            livechatCallbacks = livechatCallbacks,
            attachmentCallbacks = attachmentCallbacks,
            onUrlClick = onUrlClick
        )

        conversationHeader(
            historyPagingItems = historyPagingItems,
            welcomeMessage = welcomeMessage,
            subtitle = subtitle,
            botAvatarUrl = botAvatarUrl,
            onUrlClick = onUrlClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListPreview(@PreviewParameter(ConversationPreviewProvider::class) messages: List<ChatBubbleUiModel>) {
    ConversationList(
        liveMessages = messages,
        pendingBotMessageState = PendingBotMessageState.None,
        historyPagingItems = emptyEndOfPaginationHistory().collectAsLazyPagingItems(),
        welcomeMessage = "Hi! How can I help you?",
        subtitle = SubtitleUiModel(
            text = "You are communicating with an artificial intelligence. Read more in our",
            link = SubtitleLinkUiModel(
                text = "privacy policy",
                url = "https://example.com/privacy"
            )
        ),
        botAvatarUrl = "",
        inputBarHeightPx = remember { mutableIntStateOf(0) },
        listState = rememberLazyListState()
    )
}
