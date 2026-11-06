package ai.askdiverge.ui.compose.components.message

import androidx.compose.runtime.Composable
import ai.askdiverge.ui.form.FormUiCallbacks
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.message.MessageRoleUiModel

@Composable
internal fun ChatBubble(
    uiModel: ChatBubbleUiModel,
    botAvatarUrl: String,
    isFormEditable: Boolean = false,
    formCallbacks: FormUiCallbacks = FormUiCallbacks.None,
    onUrlClick: ((String) -> Unit)? = null
) {
    when (uiModel.role) {
        MessageRoleUiModel.USER -> UserMessageBubble(
            part = uiModel.part,
            onUrlClick = onUrlClick
        )

        MessageRoleUiModel.BOT -> BotMessage(
            uiModel = uiModel.part,
            avatarUrl = botAvatarUrl,
            isFormEditable = isFormEditable,
            formCallbacks = formCallbacks,
            onUrlClick = onUrlClick
        )
    }
}
