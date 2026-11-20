package ai.askdiverge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class LivechatSessionInfoTest {

    @Test
    fun `the agent name is kept only while active and a blank one is dropped`() {
        assertEquals(
            "Alice",
            LivechatSessionInfo(
                status = LivechatSessionInfo.Status.ACTIVE,
                agentDisplayName = "  Alice "
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo(
                status = LivechatSessionInfo.Status.ACTIVE,
                agentDisplayName = "   "
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo(
                status = LivechatSessionInfo.Status.CLOSED,
                agentDisplayName = "Alice"
            ).agentDisplayName
        )
        assertNull(
            LivechatSessionInfo(
                status = LivechatSessionInfo.Status.WAITING,
                agentDisplayName = "Alice"
            ).agentDisplayName
        )
    }

    @Test
    fun `sessions with the same phase and agent are equal`() {
        val trimmed = LivechatSessionInfo(
            status = LivechatSessionInfo.Status.ACTIVE,
            agentDisplayName = "Alice"
        )
        val padded = LivechatSessionInfo(
            status = LivechatSessionInfo.Status.ACTIVE,
            agentDisplayName = " Alice "
        )

        assertEquals(trimmed, padded)
        assertEquals(trimmed.hashCode(), padded.hashCode())
    }
}
