package ai.askdiverge.ui.compose.components.message

import androidx.compose.runtime.Composable
import ai.askdiverge.ui.attachment.AttachmentUiCallbacks
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.livechat.LivechatUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleSender
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.suggestion.SuggestionUiCallbacks

@Composable
internal fun ChatBubble(
    uiModel: ChatBubbleUiModel,
    botAvatarUrl: String,
    isFormEditable: Boolean = false,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    livechatCallbacks: LivechatUiCallbacks = LivechatUiCallbacks.None,
    attachmentCallbacks: AttachmentUiCallbacks = AttachmentUiCallbacks.None,
    suggestionCallbacks: SuggestionUiCallbacks = SuggestionUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    when (val sender = uiModel.sender) {
        ChatBubbleSender.User -> UserMessageBubble(
            part = uiModel.part,
            attachmentCallbacks = attachmentCallbacks,
            onUrlClick = onUrlClick
        )

        is ChatBubbleSender.Bot -> BotMessage(
            uiModel = uiModel.part,
            avatarUrl = botAvatarUrl,
            senderAvatar = sender.avatar,
            isFormEditable = isFormEditable,
            formCallbacks = formCallbacks,
            livechatCallbacks = livechatCallbacks,
            attachmentCallbacks = attachmentCallbacks,
            suggestionCallbacks = suggestionCallbacks,
            onUrlClick = onUrlClick
        )
    }
}
