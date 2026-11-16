package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.ui.model.ChatBubbleSender
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SenderAvatar
import ai.askdiverge.ui.sampleRichTextUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

internal class EditableHistoryFormCountTest {

    @Test
    fun `the bot bubbles after history's latest user message keep their forms editable`() {
        val history = listOf(bot("b2"), bot("b1"), user("u1"), bot("b0"))

        assertEquals(expected = 2, actual = editableHistoryFormCount(liveMessages = emptyList(), history = history))
    }

    @Test
    fun `no history form is editable once this session has a message of its own`() {
        val history = listOf(bot("b1"), user("u1"))

        assertEquals(expected = 0, actual = editableHistoryFormCount(liveMessages = listOf(user("u2")), history = history))
    }

    @Test
    fun `no history form is editable when the newest history message is the user's`() {
        val history = listOf(user("u1"), bot("b0"))

        assertEquals(expected = 0, actual = editableHistoryFormCount(liveMessages = emptyList(), history = history))
    }

    @Test
    fun `every loaded bubble is in the newest turn while no user message has loaded`() {
        val history = listOf(bot("b1"), bot("b0"))

        assertEquals(expected = 2, actual = editableHistoryFormCount(liveMessages = emptyList(), history = history))
    }

    private fun bot(id: String) = ChatBubbleUiModel(
        sender = ChatBubbleSender.Bot(SenderAvatar.Session),
        part = sampleRichTextUiModel(id),
        id = id
    )

    private fun user(id: String) = ChatBubbleUiModel(
        sender = ChatBubbleSender.User,
        part = sampleRichTextUiModel(id),
        id = id
    )
}
