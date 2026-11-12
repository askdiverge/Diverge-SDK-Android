package ai.askdiverge.ui.compose.components.conversation.section

import androidx.compose.foundation.lazy.LazyListScope
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import ai.askdiverge.ui.compose.components.message.ChatBubble
import ai.askdiverge.ui.attachment.AttachmentUiCallbacks
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.livechat.LivechatUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleUiModel

/**
 * Adds a [ChatBubble] item for each entry of [historyPagingItems], keyed by message id.
 *
 * @param historyPagingItems the conversation as it was before this session, paged in as the user
 * scrolls up.
 * @param botAvatarUrl shown beside every bot bubble.
 * @param onUrlClick called with the url of a link tapped inside a bubble.
 */
internal fun LazyListScope.messageHistory(
    historyPagingItems: LazyPagingItems<ChatBubbleUiModel>,
    botAvatarUrl: String,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    livechatCallbacks: LivechatUiCallbacks = LivechatUiCallbacks.None,
    attachmentCallbacks: AttachmentUiCallbacks = AttachmentUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    items(
        count = historyPagingItems.itemCount,
        key = historyPagingItems.itemKey { bubble -> bubble.id }
    ) { index ->
        historyPagingItems[index]?.let { bubble ->
            ChatBubble(
                uiModel = bubble,
                botAvatarUrl = botAvatarUrl,
                isFormEditable = false,
                formCallbacks = formCallbacks,
                livechatCallbacks = livechatCallbacks,
                attachmentCallbacks = attachmentCallbacks,
                onUrlClick = onUrlClick
            )
        }
    }
}
