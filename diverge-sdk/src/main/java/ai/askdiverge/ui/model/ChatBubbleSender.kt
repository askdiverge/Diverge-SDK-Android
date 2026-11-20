package ai.askdiverge.ui.model

import androidx.compose.runtime.Immutable

/** Who sent a chat bubble: the visitor, or the bot side together with the face beside it. */
@Immutable
internal sealed interface ChatBubbleSender {

    /** The visitor. */
    data object User : ChatBubbleSender

    /** The assistant or a livechat agent, as [avatar] says. */
    data class Bot(val avatar: SenderAvatar) : ChatBubbleSender
}
