package ai.askdiverge.ui.compose.components.conversation.section

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import ai.askdiverge.ui.compose.components.message.ChatBubble
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.livechat.LivechatUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleUiModel

/**
 * Adds a [ChatBubble] item for each of [messages], keyed by message id.
 *
 * @param messages the messages the user has already seen a reply to.
 * @param botAvatarUrl shown beside every bot bubble.
 * @param onUrlClick called with the url of a link tapped inside a bubble.
 */
internal fun LazyListScope.earlierMessages(
    messages: List<ChatBubbleUiModel>,
    botAvatarUrl: String,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    livechatCallbacks: LivechatUiCallbacks = LivechatUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    items(
        items = messages,
        key = { bubble -> bubble.id }
    ) { bubble ->
        ChatBubble(
            uiModel = bubble,
            botAvatarUrl = botAvatarUrl,
            isFormEditable = false,
            formCallbacks = formCallbacks,
            livechatCallbacks = livechatCallbacks,
            onUrlClick = onUrlClick
        )
    }
}
