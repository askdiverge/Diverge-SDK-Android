package ai.askdiverge.ui.compose.components.conversation

import ai.askdiverge.ui.model.ChatBubbleSender
import ai.askdiverge.ui.model.ChatBubbleUiModel
import ai.askdiverge.ui.model.SenderAvatar
import ai.askdiverge.ui.sampleRichTextUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PartitionedMessagesTest {

    @Test
    fun `before the user has posted anything every message is an earlier one`() {
        val messages = listOf(bot("b1"), bot("b0"))

        val partitioned = messages.partitioned()

        assertEquals(expected = emptyList(), actual = partitioned.latestUserMessageAndItsReply)
        assertEquals(expected = messages, actual = partitioned.earlierMessages)
    }

    @Test
    fun `an empty conversation has no latest turn and nothing earlier`() {
        val partitioned = emptyList<ChatBubbleUiModel>().partitioned()

        assertEquals(expected = emptyList(), actual = partitioned.latestUserMessageAndItsReply)
        assertEquals(expected = emptyList(), actual = partitioned.earlierMessages)
    }

    @Test
    fun `the latest user message keeps its reply and leaves the turn before it as earlier`() {
        val messages = listOf(bot("b2"), bot("b1"), user("u1"), bot("b0"), user("u0"))

        val partitioned = messages.partitioned()

        assertEquals(
            expected = listOf(bot("b2"), bot("b1"), user("u1")),
            actual = partitioned.latestUserMessageAndItsReply
        )
        assertEquals(expected = listOf(bot("b0"), user("u0")), actual = partitioned.earlierMessages)
    }

    @Test
    fun `the text and photos of one send stay in the latest turn`() {
        val messages = listOf(user("photo-2"), user("photo-1"), user("text"), bot("b0"), user("u0"))

        val partitioned = messages.partitioned()

        assertEquals(
            expected = listOf(user("photo-2"), user("photo-1"), user("text")),
            actual = partitioned.latestUserMessageAndItsReply
        )
        assertEquals(expected = listOf(bot("b0"), user("u0")), actual = partitioned.earlierMessages)
    }

    @Test
    fun `sends the agent has not answered yet stay one turn under the reply that follows them`() {
        val messages = listOf(bot("agent"), user("u3"), user("u2"), user("u1"), bot("b0"))

        val partitioned = messages.partitioned()

        assertEquals(
            expected = listOf(bot("agent"), user("u3"), user("u2"), user("u1")),
            actual = partitioned.latestUserMessageAndItsReply
        )
        assertEquals(expected = listOf(bot("b0")), actual = partitioned.earlierMessages)
    }

    @Test
    fun `a conversation of user messages only is one latest turn`() {
        val messages = listOf(user("u2"), user("u1"))

        val partitioned = messages.partitioned()

        assertEquals(expected = messages, actual = partitioned.latestUserMessageAndItsReply)
        assertEquals(expected = emptyList(), actual = partitioned.earlierMessages)
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
