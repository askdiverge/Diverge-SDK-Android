package ai.askdiverge.ui.compose.previewprovider

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ai.askdiverge.ui.model.SubtitleLinkUiModel
import ai.askdiverge.ui.model.SubtitleUiModel
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.SessionState

internal class ChatbotUiStatePreviewProvider : PreviewParameterProvider<ChatbotUiState> {

    private val conversation = ConversationPreviewProvider().values.first()

    override val values: Sequence<ChatbotUiState>
        get() = sequenceOf(
            ChatbotUiState(
                session = SessionState(
                    connectionState = ChatConnectionState.Ready,
                    assistantName = "AI Assistant",
                    avatarUrl = "",
                    welcomeMessage = "Hi! How can I help you?",
                    subtitle = SubtitleUiModel(
                        text = "You are communicating with an artificial intelligence. Read more in our",
                        link = SubtitleLinkUiModel(
                            text = "privacy policy",
                            url = "https://example.com/privacy"
                        )
                    )
                ),
                conversation = conversation
            ),
            ChatbotUiState(
                session = SessionState(connectionState = ChatConnectionState.Connecting)
            ),
            ChatbotUiState(
                session = SessionState(connectionState = ChatConnectionState.Restarting),
                conversation = conversation,
                isResettingConversation = true
            ),
            ChatbotUiState(
                session = SessionState(
                    connectionState = ChatConnectionState.Ready,
                    assistantName = "AI Assistant",
                    avatarUrl = "",
                    welcomeMessage = "Hi! How can I help you?"
                ),
                pendingBotMessage = PendingBotMessageState.AwaitingFirstPart,
                conversation = conversation
            ),
            ChatbotUiState(
                session = SessionState(
                    connectionState = ChatConnectionState.Error(message = "Failed to connect. Please try again.")
                )
            )
        )
}
