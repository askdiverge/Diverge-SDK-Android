package ai.askdiverge.ui

import ai.askdiverge.domain.model.config.display.subtitle.Subtitle
import ai.askdiverge.domain.model.config.display.subtitle.SubtitleLink
import ai.askdiverge.ui.model.SubtitleLinkUiModel
import ai.askdiverge.ui.model.SubtitleUiModel
import ai.askdiverge.ui.model.mapper.toUserMessageItem
import ai.askdiverge.ui.model.message.MessageRoleUiModel
import ai.askdiverge.ui.state.ChatConnectionState
import ai.askdiverge.ui.state.ChatbotUiState
import ai.askdiverge.ui.state.PendingBotMessageState
import ai.askdiverge.ui.state.SheetType
import ai.askdiverge.ui.state.SnackbarState
import ai.askdiverge.ui.state.SnackbarType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ChatbotUiStateHolderTest {

    private val stateHolder = ChatbotUiStateHolder()

    @Test
    fun `when a message is posted expect the conversation asked to scroll to it`() {
        val message = "hello".toUserMessageItem()

        stateHolder.onSendStarted(message)

        assertEquals(
            expected = message.id,
            actual = stateHolder.uiState.value.messageToScrollTo
        )
    }

    @Test
    fun `when the conversation has scrolled to the posted message expect nothing left to scroll to`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())

        stateHolder.onScrolledToMessage()

        assertNull(stateHolder.uiState.value.messageToScrollTo)
    }

    @Test
    fun `onRetryableError drops the pending user message and restores input when bot hasn't replied`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())

        stateHolder.onRetryableError(serverMessage = "Please try again.", restoredInput = "hello")

        val state = stateHolder.uiState.value
        assertTrue(state.conversation.isEmpty())
        assertEquals("hello", state.inputMessage)
        assertEquals(
            SnackbarState(type = SnackbarType.StreamError, text = "Please try again."),
            state.snackbarState
        )
        assertEquals(expected = PendingBotMessageState.None, actual = state.pendingBotMessage)
    }

    @Test
    fun `onRetryableError drops the completed bot reply and the originating user message`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onReplyPartFinished("hi there".toUserMessageItem().part)

        stateHolder.onRetryableError(serverMessage = "Please try again.", restoredInput = "hello")

        val state = stateHolder.uiState.value
        // A completed BOT part means this turn started replying before failing, so both the
        // bot's partial reply and the user's message that triggered it are rolled back.
        assertTrue(state.conversation.isEmpty())
        assertEquals("hello", state.inputMessage)
        assertEquals(
            SnackbarState(type = SnackbarType.StreamError, text = "Please try again."),
            state.snackbarState
        )
    }

    @Test
    fun `onRetryableError leaves an earlier completed turn untouched`() {
        stateHolder.onSendStarted("hi there".toUserMessageItem())
        stateHolder.onReplyPartFinished("hello!".toUserMessageItem().part)
        stateHolder.onReplyFinished()
        stateHolder.onSendStarted("follow-up".toUserMessageItem())

        stateHolder.onRetryableError(serverMessage = "Please try again.", restoredInput = "follow-up")

        val state = stateHolder.uiState.value
        assertEquals(2, state.conversation.size)
        assertEquals(MessageRoleUiModel.BOT, state.conversation.first().role)
        assertEquals(MessageRoleUiModel.USER, state.conversation.last().role)
        assertEquals("follow-up", state.inputMessage)
        assertEquals(
            SnackbarState(type = SnackbarType.StreamError, text = "Please try again."),
            state.snackbarState
        )
    }

    @Test
    fun `onSnackbarConsumed clears the snackbar`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onRetryableError(serverMessage = "Please try again.", restoredInput = "hello")

        stateHolder.onSnackbarConsumed()

        assertNull(stateHolder.uiState.value.snackbarState)
    }

    @Test
    fun `onConnectionStarted wipes the previous session`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onRetryableError(serverMessage = "Please try again.", restoredInput = "hello")
        stateHolder.onSheetRequested(SheetType.Privacy)

        stateHolder.onConnectionStarted()

        assertEquals(
            expected = ChatbotUiState(),
            actual = stateHolder.uiState.value
        )
    }

    @Test
    fun `onSheetRequested opens the sheet and onSheetDismissed closes it`() {
        stateHolder.onSheetRequested(SheetType.Privacy)
        assertEquals(SheetType.Privacy, stateHolder.uiState.value.activeSheet)

        stateHolder.onSheetDismissed()
        assertNull(stateHolder.uiState.value.activeSheet)
    }

    @Test
    fun `sending a message waits for the first part`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())

        assertEquals(
            expected = PendingBotMessageState.AwaitingFirstPart,
            actual = stateHolder.uiState.value.pendingBotMessage
        )
    }

    @Test
    fun `receiving the first part starts streaming the reply`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())

        stateHolder.onReplyStreaming(listOf("hi there".toUserMessageItem().part))

        assertIs<PendingBotMessageState.Streaming>(stateHolder.uiState.value.pendingBotMessage)
    }

    @Test
    fun `a completed part with no delta before it also starts streaming the reply`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())

        stateHolder.onReplyPartFinished("hi there".toUserMessageItem().part)

        assertIs<PendingBotMessageState.Streaming>(stateHolder.uiState.value.pendingBotMessage)
    }

    @Test
    fun `a finished reply goes idle so a follow-up message shows the thinking bubble again`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onReplyStreaming(listOf("hi there".toUserMessageItem().part))
        stateHolder.onReplyPartFinished("hi there".toUserMessageItem().part)

        stateHolder.onReplyFinished()

        assertEquals(expected = PendingBotMessageState.None, actual = stateHolder.uiState.value.pendingBotMessage)

        stateHolder.onSendStarted("follow-up".toUserMessageItem())

        assertEquals(
            expected = PendingBotMessageState.AwaitingFirstPart,
            actual = stateHolder.uiState.value.pendingBotMessage
        )
    }

    @Test
    fun `when the config is loaded expect the chat ready and the assistant described`() {
        stateHolder.onConfigLoaded(
            sampleConfig(
                display = sampleDisplay(
                    name = "Ada",
                    avatarUrl = "https://example.com/avatar.png",
                    welcomeMessage = "How can I help?",
                    subtitle = Subtitle(
                        text = "Powered by AI",
                        link = SubtitleLink(
                            text = "Learn more",
                            url = "https://example.com/ai"
                        )
                    )
                )
            )
        )

        val state = stateHolder.uiState.value
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
        assertEquals(
            expected = "Ada",
            actual = state.session.assistantName
        )
        assertEquals(
            expected = "https://example.com/avatar.png",
            actual = state.session.avatarUrl
        )
        assertEquals(
            expected = "How can I help?",
            actual = state.session.welcomeMessage
        )
        assertEquals(
            expected = SAMPLE_PRIVACY_POLICY_URL,
            actual = state.session.privacyPolicyUrl
        )
        assertEquals(
            expected = SubtitleUiModel(
                text = "Powered by AI",
                link = SubtitleLinkUiModel(
                    text = "Learn more",
                    url = "https://example.com/ai"
                )
            ),
            actual = state.session.subtitle
        )
        assertEquals(
            expected = sampleTheme(),
            actual = state.theme
        )
    }

    @Test
    fun `when the connection fails expect the chat on the retry screen`() {
        stateHolder.onConnectionError(message = "No connection")

        assertEquals(
            expected = ChatConnectionState.Error(message = "No connection"),
            actual = stateHolder.uiState.value.session.connectionState
        )
    }

    @Test
    fun `when a restart starts expect the config to be kept`() {
        stateHolder.onConfigLoaded(sampleConfig(display = sampleDisplay(name = "Ada")))

        stateHolder.onRestartStarted()

        val state = stateHolder.uiState.value
        assertEquals(
            expected = ChatConnectionState.Restarting,
            actual = state.session.connectionState
        )
        assertEquals(
            expected = "Ada",
            actual = state.session.assistantName
        )
        assertEquals(
            expected = sampleTheme(),
            actual = state.theme
        )
    }

    @Test
    fun `when a restart finishes expect an empty conversation`() {
        stateHolder.onConfigLoaded(sampleConfig(display = sampleDisplay(name = "Ada")))
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onConversationResetStarted()
        stateHolder.onRestartStarted()

        stateHolder.onRestartFinished()

        val state = stateHolder.uiState.value
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
        assertTrue(state.conversation.isEmpty())
        assertEquals(
            expected = PendingBotMessageState.None,
            actual = state.pendingBotMessage
        )
        assertFalse(state.isResettingConversation)
        assertEquals(
            expected = "Ada",
            actual = state.session.assistantName
        )
    }

    @Test
    fun `when the conversation is resynced expect only the messages the history provides to be cleared`() {
        stateHolder.onConfigLoaded(sampleConfig())
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onReplyFinished()
        stateHolder.onInputChanged("half typed")

        stateHolder.onConversationResynced()

        val state = stateHolder.uiState.value
        assertTrue(state.conversation.isEmpty())
        assertEquals(
            expected = "half typed",
            actual = state.inputMessage
        )
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
    }

    @Test
    fun `when a send starts expect the message shown and the draft cleared`() {
        stateHolder.onInputChanged("hello")

        stateHolder.onSendStarted("hello".toUserMessageItem())

        val state = stateHolder.uiState.value
        assertEquals(
            expected = "",
            actual = state.inputMessage
        )
        assertEquals(
            expected = 1,
            actual = state.conversation.size
        )
        assertEquals(
            expected = MessageRoleUiModel.USER,
            actual = state.conversation.single().role
        )
    }

    @Test
    fun `when a reply part finishes expect it in the conversation and the pending bubble emptied`() {
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onReplyStreaming(listOf("hi the".toUserMessageItem().part))

        stateHolder.onReplyPartFinished("hi there".toUserMessageItem().part)

        val state = stateHolder.uiState.value
        assertEquals(
            expected = MessageRoleUiModel.BOT,
            actual = state.conversation.first().role
        )
        assertEquals(
            expected = "hi there".toUserMessageItem().part,
            actual = state.conversation.first().part
        )
        assertEquals(
            expected = PendingBotMessageState.Streaming(parts = emptyList()),
            actual = state.pendingBotMessage
        )
    }

    @Test
    fun `when the chat data delete succeeds expect the sheet closed and the deletion confirmed`() {
        stateHolder.onSheetRequested(SheetType.DeleteChatData)
        stateHolder.onDeleteChatDataStarted()

        stateHolder.onDeleteChatDataSucceeded()

        val state = stateHolder.uiState.value
        assertFalse(state.isDeletingChatData)
        assertNull(state.activeSheet)
        assertEquals(
            expected = SnackbarState(type = SnackbarType.DeleteChatDataSucceeded),
            actual = state.snackbarState
        )
    }

    @Test
    fun `when the chat data delete fails expect the sheet kept open and the failure reported`() {
        stateHolder.onSheetRequested(SheetType.DeleteChatData)
        stateHolder.onDeleteChatDataStarted()

        stateHolder.onDeleteChatDataFailed()

        val state = stateHolder.uiState.value
        assertFalse(state.isDeletingChatData)
        assertEquals(
            expected = SheetType.DeleteChatData,
            actual = state.activeSheet
        )
        assertEquals(
            expected = SnackbarState(type = SnackbarType.DeleteChatDataFailed),
            actual = state.snackbarState
        )
    }

    @Test
    fun `when the conversation reset fails expect the chat usable and the failure reported`() {
        stateHolder.onConfigLoaded(sampleConfig())
        stateHolder.onSendStarted("hello".toUserMessageItem())
        stateHolder.onReplyFinished()
        stateHolder.onConversationResetStarted()
        stateHolder.onRestartStarted()

        stateHolder.onConversationResetFailed()

        val state = stateHolder.uiState.value
        assertEquals(
            expected = ChatConnectionState.Ready,
            actual = state.session.connectionState
        )
        assertFalse(state.isResettingConversation)
        assertEquals(
            expected = SnackbarState(type = SnackbarType.ConversationResetFailed),
            actual = state.snackbarState
        )
        // The reset never happened, so the conversation the user was having is still theirs.
        assertEquals(
            expected = 1,
            actual = state.conversation.size
        )
    }
}
