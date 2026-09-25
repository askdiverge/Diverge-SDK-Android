package ai.askdiverge.ui

import ai.askdiverge.domain.model.Config
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.mapper.mapToUi
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.model.message.part.MessagePartUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarState
import ai.askdiverge.ui.state.SnackbarType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Sole owner and writer of [ChatbotUiState]. The ViewModel keeps the asynchronous orchestration
 * (coroutines, use cases, pagination) and translates outcomes into the semantic calls below; this
 * holder only reduces state.
 */
internal class ChatbotUiStateHolder {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    // Wipes the chat back to a clean "connecting" state. This is for first open only.
    // A restart goes through onRestartStarted() instead, which keeps
    // the config-derived session fields so /config does not
    // have to be re-fetched.
    fun onConnectionStarted() {
        _uiState.update { ChatbotUiState() }
    }

    fun onConfigLoaded(config: Config) {
        _uiState.update {
            it.copy(
                session = it.session.copy(
                    connectionState = ChatConnectionState.Ready,
                    assistantName = config.display.name,
                    avatarUrl = config.display.avatarUrl.orEmpty(),
                    welcomeMessage = config.display.welcomeMessage.orEmpty(),
                    subtitle = config.display.subtitle?.mapToUi(),
                    privacyPolicyUrl = config.display.privacyPolicyUrl
                ),
                theme = config.theme
            )
        }
    }

    fun onConnectionError(message: String) {
        _uiState.update {
            it.copy(session = it.session.copy(connectionState = ChatConnectionState.Error(message = message)))
        }
    }

    fun onRestartStarted() {
        _uiState.update {
            it.copy(session = it.session.copy(connectionState = ChatConnectionState.Restarting))
        }
    }

    fun onRestartFinished() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Ready),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList(),
                isResettingConversation = false
            )
        }
    }

    // The chat data is already gone at this point, so the conversation is cleared like a successful
    // restart. Only the session is missing, which leaves the connection in Error rather than Ready,
    // so the retry screen is shown. snackbarState is deliberately left untouched: the
    // onDeleteChatDataSucceeded() snackbar set just before this is the only place the user learns
    // that the delete itself worked, and the error screen already reports the missing session.
    fun onRestartFailed(message: String) {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Error(message = message)),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList(),
                isResettingConversation = false
            )
        }
    }

    // The optimistic messages are dropped so the refreshed history is the only source of the
    // conversation, rather than rendering it twice.
    fun onConversationResynced() {
        _uiState.update { it.copy(conversation = emptyList()) }
    }

    // The token the conversation was bound to is gone, so the chat drops to the retry screen.
    fun onSessionExpired() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.SessionExpired),
                pendingBotMessage = PendingBotMessageState.None,
                conversation = emptyList()
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputMessage = text) }
    }

    // The user message is shown optimistically before the request goes out, and the draft is cleared.
    // pendingBotMessage starts fresh rather than copying the previous one, so hasStartedReplying
    // doesn't carry over and gate the typing bubble out of future replies.
    fun onSendStarted(message: ChatBubbleUiModel) {
        _uiState.update {
            it.copy(
                inputMessage = "",
                pendingBotMessage = PendingBotMessageState.AwaitingFirstPart,
                conversation = listOf(message) + it.conversation,
                messageToScrollTo = message.id
            )
        }
    }

    // Takes the message back off the screen and returns it to the input field. serverMessage is
    // null when the stream broke before the server could say why.
    fun onRetryableError(
        restoredInput: String,
        serverMessage: String? = null
    ) {
        _uiState.update { state ->
            // We will remove all messages until (and including) last message from the user
            val userMessageIndex = state.conversation.indexOfFirst { it.role == MessageRoleUiModel.USER }
            state.copy(
                inputMessage = restoredInput,
                snackbarState = if (serverMessage != null) {
                    SnackbarState(
                        type = SnackbarType.StreamError,
                        text = serverMessage
                    )
                } else {
                    SnackbarState(type = SnackbarType.MessageSendFailed)
                },
                pendingBotMessage = PendingBotMessageState.None,
                conversation = state.conversation.drop(userMessageIndex + 1)
            )
        }
    }

    fun onScrolledToMessage() {
        _uiState.update { it.copy(messageToScrollTo = null) }
    }

    fun onSnackbarConsumed() {
        _uiState.update { it.copy(snackbarState = null) }
    }

    fun onSheetRequested(sheet: SheetType) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun onSheetDismissed() {
        _uiState.update { it.copy(activeSheet = null) }
    }

    fun onReplyStreaming(parts: List<MessagePartUiModel>) {
        _uiState.update {
            it.copy(
                pendingBotMessage = PendingBotMessageState.Streaming(parts)
            )
        }
    }

    fun onReplyPartFinished(part: MessagePartUiModel) {
        _uiState.update {
            it.copy(
                conversation = listOf(
                    ChatBubbleUiModel(
                        role = MessageRoleUiModel.BOT,
                        part = part,
                        id = UUID.randomUUID().toString()
                    )
                ) + it.conversation,
                pendingBotMessage = PendingBotMessageState.Streaming(parts = emptyList())
            )
        }
    }

    fun onReplyFinished() {
        _uiState.update { it.copy(pendingBotMessage = PendingBotMessageState.None) }
    }

    fun onDeleteChatDataStarted() {
        _uiState.update { it.copy(isDeletingChatData = true) }
    }

    fun onDeleteChatDataSucceeded() {
        _uiState.update {
            it.copy(
                isDeletingChatData = false,
                activeSheet = null,
                snackbarState = SnackbarState(type = SnackbarType.DeleteChatDataSucceeded)
            )
        }
    }

    fun onDeleteChatDataFailed() {
        _uiState.update {
            it.copy(
                isDeletingChatData = false,
                snackbarState = SnackbarState(type = SnackbarType.DeleteChatDataFailed)
            )
        }
    }

    fun onConversationResetStarted() {
        _uiState.update { it.copy(isResettingConversation = true) }
    }

    fun onConversationResetFailed() {
        _uiState.update {
            it.copy(
                session = it.session.copy(connectionState = ChatConnectionState.Ready),
                isResettingConversation = false,
                snackbarState = SnackbarState(type = SnackbarType.ConversationResetFailed)
            )
        }
    }
}
