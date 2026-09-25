package ai.askdiverge.ui.state

import androidx.compose.runtime.Immutable
import ai.askdiverge.domain.model.config.theme.Theme
import ai.askdiverge.ui.model.ChatBubbleUiModel

@Immutable
internal data class ChatbotUiState(
    val session: SessionState = SessionState(),
    val pendingBotMessage: PendingBotMessageState = PendingBotMessageState.None,
    val conversation: List<ChatBubbleUiModel> = emptyList(),
    val messageToScrollTo: String? = null,
    val inputMessage: String = "",
    val activeSheet: SheetType? = null,
    val isDeletingChatData: Boolean = false,
    val isResettingConversation: Boolean = false,
    val snackbarState: SnackbarState? = null,
    val theme: Theme? = null
)
